package ytslim.data

import android.content.Context
import android.util.Log
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ln
import kotlin.random.Random

/** Um canal que um canal inscrito destaca na própria página ("Featured channels"). */
@Serializable
data class FeaturedChannel(val id: String, val name: String, val handle: String? = null, val photo: String? = null)

/** O que a página de um canal inscrito destaca, e quando ela foi lida. */
@Serializable
data class FeaturedSource(val name: String, val handle: String? = null, val scannedAt: Long, val featured: List<FeaturedChannel>)

/** Um canal que pode aparecer na Explore e quantos canais inscritos o destacam. */
data class Candidate(val channel: FeaturedChannel, val weight: Int)

/** Os vídeos de um canal sorteado: os recentes (a aba de vídeos) e os das prateleiras da página dele. */
data class ChannelVideos(val channel: FeaturedChannel, val recent: List<Video>, val older: List<Video>)

/**
 * A Explore (pedido do Johan, 2026-11-02): vídeos dos canais que os canais inscritos destacam,
 * misturando recentes e mais antigos, e um sorteio diferente a cada atualização. As regras puras
 * ficam aqui (testadas sem rede); a busca e o cache em [ExploreRepository].
 */
object ExploreRules {
    const val CHANNELS_PER_DRAW = 10
    const val RECENT_PER_CHANNEL = 2
    const val OLDER_PER_CHANNEL = 2

    /** Os recentes saem dos primeiros desta quantidade da aba de vídeos. */
    const val RECENT_WINDOW = 12

    /** Vídeo assistido até aqui (%) não volta na Explore. */
    const val WATCHED = 90

    /** A página de um canal inscrito é lida de novo depois disso. */
    const val RESCAN_AFTER_MS = 30L * 24 * 60 * 60 * 1000

    /** Os canais dos últimos sorteios (uns 4) ficam de fora do próximo, se der. */
    const val REMEMBER_CHANNELS = 40

    /**
     * Canal-irmão do mesmo criador (tradução, segundo canal, podcast), que não é descoberta:
     * "Veritasium em Português" e "2veritasium" de Veritasium, "Dinge Erklärt – Kurzgesagt" de
     * "Kurzgesagt – In a Nutshell", "Mentour Now!" de "Mentour Pilot". Vale só para o par: outro
     * canal inscrito que destaque o mesmo canal ainda conta.
     */
    fun isSibling(sourceName: String, sourceHandle: String?, featured: FeaturedChannel): Boolean {
        val name = compact(featured.name)
        val handle = compact(featured.handle.orEmpty())
        val source = compact(sourceName)
        val sourceAt = compact(sourceHandle.orEmpty())
        if (source.length >= 3 && (source in name || source in handle)) return true
        if (sourceAt.length >= 3 && (sourceAt in name || sourceAt in handle)) return true
        if (name.length >= 4 && name in source) return true
        // A primeira palavra do nome, se for marca ("Kurzgesagt", "Mentour"), e não um artigo.
        val first = words(sourceName).firstOrNull()?.takeIf { it.length >= 4 } ?: return false
        return first in words(featured.name)
    }

    /**
     * Os candidatos de todos os canais inscritos lidos: sem os já inscritos, os bloqueados e os
     * irmãos de quem destacou. O peso é quantos canais inscritos destacam o canal.
     */
    fun candidates(sources: Collection<FeaturedSource>, subscribed: Set<String>, blocked: Set<String>): List<Candidate> {
        val weights = LinkedHashMap<String, Int>()
        val channels = HashMap<String, FeaturedChannel>()
        for (source in sources) {
            source.featured
                .filter { it.id !in subscribed && it.id !in blocked && !isSibling(source.name, source.handle, it) }
                .distinctBy { it.id }
                .forEach {
                    weights.merge(it.id, 1, Int::plus)
                    channels.putIfAbsent(it.id, it)
                }
        }
        return weights.map { (id, weight) -> Candidate(channels.getValue(id), weight) }
    }

    /**
     * Sorteia [count] canais, com mais chance para os destacados por mais canais inscritos
     * (Efraimidis–Spirakis). Os de [avoid] (os últimos sorteios) só entram se faltar canal.
     */
    fun draw(candidates: List<Candidate>, count: Int, avoid: Set<String>, random: Random): List<FeaturedChannel> {
        fun ranked(list: List<Candidate>) = list
            .map { it to ln(random.nextDouble().coerceAtLeast(Double.MIN_VALUE)) / it.weight }
            .sortedByDescending { it.second }
            .map { it.first.channel }
        val (fresh, recent) = candidates.partition { it.channel.id !in avoid }
        return (ranked(fresh) + ranked(recent)).take(count)
    }

    /**
     * Os vídeos que vão para a tela de um canal: [RECENT_PER_CHANNEL] dos recentes e
     * [OLDER_PER_CHANNEL] das prateleiras, sorteados; se um lado tiver poucos, o outro completa.
     * Alternados: recente, antigo, recente, antigo. Só entram os que passam por [keep] (os filtros
     * de data e duração da aba): o sorteio é feito entre eles.
     */
    fun pick(videos: ChannelVideos, filters: ContentFilters, random: Random, keep: (Video) -> Boolean = { true }): List<Video> {
        fun usable(list: List<Video>) = list.filter {
            !it.isShorts && !it.isLive && !it.isUpcoming && it.percentWatched < WATCHED && !TopicRules.isExcluded(it, filters) && keep(it)
        }
        val recentPool = usable(videos.recent.take(RECENT_WINDOW)).shuffled(random)
        val recentIds = videos.recent.mapTo(HashSet()) { it.id }
        val olderPool = usable(videos.older.filter { it.id !in recentIds }).distinctBy { it.id }.shuffled(random)
        val wanted = RECENT_PER_CHANNEL + OLDER_PER_CHANNEL
        val recent = recentPool.take(maxOf(RECENT_PER_CHANNEL, wanted - olderPool.size))
        val older = olderPool.take(wanted - recent.size)
        val out = ArrayList<Video>()
        for (i in 0 until maxOf(recent.size, older.size)) {
            recent.getOrNull(i)?.let(out::add)
            older.getOrNull(i)?.let(out::add)
        }
        return out
    }

    /**
     * Junta os canais em rodadas (o 1º vídeo de cada canal, depois o 2º…), para a tela não ter
     * quatro vídeos do mesmo canal seguidos. Um vídeo repetido fica na primeira posição.
     */
    fun mix(perChannel: List<List<Video>>, skip: Set<String> = emptySet()): List<Video> {
        val seen = HashSet(skip)
        val out = ArrayList<Video>()
        for (i in 0 until (perChannel.maxOfOrNull { it.size } ?: 0)) {
            perChannel.forEach { list -> list.getOrNull(i)?.takeIf { seen.add(it.id) }?.let(out::add) }
        }
        return out
    }

    /**
     * A lista da tela: em cada sorteio, [pick] de cada canal e [mix] em rodadas, com a ordem dos
     * canais embaralhada; um vídeo que já apareceu num sorteio anterior não se repete. A semente
     * de cada sorteio fixa a escolha, então mudar um filtro refaz a lista sem ler nada de novo.
     */
    fun screen(draws: List<ExploreDraw>, filters: ContentFilters, keep: (Video) -> Boolean = { true }): List<Video> {
        val seen = HashSet<String>()
        return draws.flatMap { draw ->
            val picks = draw.channels.mapIndexed { i, channel -> pick(channel, filters, Random(draw.seed + i + 1), keep) }
            mix(picks.shuffled(Random(draw.seed)), seen).onEach { seen.add(it.id) }
        }
    }

    /**
     * Só os vídeos do próprio canal (as prateleiras também mostram participações em outros
     * canais), já com o id e o nome do canal, que a página do canal não manda em cada vídeo.
     * "3Blue1Brown and PolyaMath" é do 3Blue1Brown; "Numberphile and 3Blue1Brown", não.
     */
    fun ownVideos(channel: FeaturedChannel, videos: List<Video>): List<Video> {
        val name = TopicRules.normalize(channel.name)
        return videos
            .filter { v -> v.channelName.isNullOrBlank() || TopicRules.normalize(v.channelName).startsWith(name) }
            .map { it.copy(channelId = it.channelId ?: channel.id, channelName = it.channelName ?: channel.name) }
    }

    /** O @ de um canal pelo que mais aparece nos autores dos vídeos dele ("Veritasium • @veritasium"). */
    fun handleOf(authors: List<String?>): String? = authors
        .flatMap { it.orEmpty().split('•').map(String::trim) }
        .filter { it.startsWith("@") }
        .groupingBy { it }.eachCount()
        .maxByOrNull { it.value }?.key

    private fun compact(text: String) = TopicRules.normalize(text).replace(" ", "")

    private fun words(text: String) = TopicRules.normalize(text).split(' ').filter { it.isNotEmpty() }
}

/** O cache da Explore no aparelho: o que cada canal inscrito destaca e os canais dos últimos sorteios. */
@Serializable
data class ExploreCache(
    val sources: Map<String, FeaturedSource> = emptyMap(),
    val recent: List<String> = emptyList(),
)

/**
 * Um sorteio da Explore: os vídeos de cada canal sorteado (a escolha do que vai para a tela é de
 * [ExploreRules.screen], com [seed]), os ids de todos os sorteados e se ainda há canais.
 */
class ExploreDraw(val channels: List<ChannelVideos>, val ids: List<String>, val hasMore: Boolean, val seed: Long)

/**
 * A busca da Explore. Lê a página dos canais inscritos (um pedido por canal, guardado por
 * [ExploreRules.RESCAN_AFTER_MS] num arquivo do app), sorteia os canais destacados e lê a página
 * de cada canal sorteado, que traz os vídeos recentes e as prateleiras com os mais antigos.
 */
object ExploreRepository {
    private const val TAG = "Doorglow"
    /** Canais inscritos lidos de uma vez antes do primeiro sorteio (até [FIRST_BATCHES] vezes, até ter [MIN_CANDIDATES]). */
    private const val FIRST_SCAN = 12
    private const val FIRST_BATCHES = 3
    private const val MIN_CANDIDATES = 30
    private const val SAVE_EVERY = 8
    private const val PARALLEL = 4
    private const val BACKGROUND_PARALLEL = 2

    private lateinit var file: File
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cache: ExploreCache? = null
    private val scanning = AtomicBoolean(false)

    fun init(context: Context) {
        file = File(context.filesDir, "explore.json")
    }

    /**
     * Um sorteio novo de canais, sem os que já estão na tela ([shown]), com os vídeos de cada um.
     * Lança um erro se não deu para ler as inscrições (sem login ou sem rede).
     */
    suspend fun draw(shown: Set<String> = emptySet(), random: Random = Random.Default): ExploreDraw {
        val subscribed = FeedRepository.subscribedChannels() ?: error("Couldn't read your subscriptions.")
        val ids = subscribed.mapTo(HashSet()) { it.id }
        val filters = SettingsRepository.current().filters
        suspend fun candidates() = ExploreRules.candidates(load().sources.filterKeys { it in ids }.values, ids, filters.blockedIds)

        // A primeira vez: lê alguns canais inscritos já, até ter o que sortear; o resto vai depois.
        var candidates = candidates()
        repeat(FIRST_BATCHES) {
            if (candidates.size >= MIN_CANDIDATES) return@repeat
            val unread = subscribed.filter { it.id !in load().sources }
            if (unread.isEmpty()) return@repeat
            scan(unread.shuffled(random).take(FIRST_SCAN), PARALLEL)
            candidates = candidates()
        }
        scanRestLater(stale(subscribed))

        val chosen = ExploreRules.draw(candidates, ExploreRules.CHANNELS_PER_DRAW, load().recent.toSet() + shown, random)
            .filter { it.id !in shown }
        ChannelPhotos.putAll(chosen.mapNotNull { c -> c.photo?.let { c.id to it } }.toMap())
        val gate = Semaphore(PARALLEL)
        val videos = coroutineScope {
            chosen.map { channel ->
                async {
                    gate.withPermit {
                        runCatching { videosOf(channel) }.onFailure { Log.w(TAG, "explore: no videos from ${channel.id}", it) }.getOrNull()
                    }
                }
            }.awaitAll()
        }.filterNotNull()
        remember(chosen.map { it.id })
        val hasMore = candidates.size > shown.size + chosen.size
        Log.i(TAG, "explore: ${subscribed.size} subscriptions, ${load().sources.count { it.key in ids }} read, " +
            "${candidates.size} candidates, ${chosen.size} drawn, ${videos.size} with videos, ${videos.sumOf { it.recent.size + it.older.size }} videos")
        return ExploreDraw(videos, chosen.map { it.id }, hasMore, random.nextLong())
    }

    /**
     * Lê em segundo plano as páginas dos canais inscritos que faltam ou ficaram velhas, para a
     * Explore abrir mais rápido. O feed chama depois de ler as inscrições.
     */
    fun prepare() {
        AppScope.launch {
            val subscribed = FeedRepository.subscribedChannels() ?: return@launch
            scanRestLater(stale(subscribed))
        }
    }

    private suspend fun stale(subscribed: List<Channel>): List<Channel> {
        val sources = load().sources
        val limit = System.currentTimeMillis() - ExploreRules.RESCAN_AFTER_MS
        return subscribed.filter { (sources[it.id]?.scannedAt ?: 0) < limit }
    }

    /** O que a página de um canal mostra: os destaques e os vídeos (recentes e das prateleiras). */
    private suspend fun page(channelId: String): List<MediaGroup> =
        Engine.content.getChannelObserve(channelId).firstOrNull().orEmpty()

    internal suspend fun videosOf(channel: FeaturedChannel): ChannelVideos {
        val groups = page(channel.id)
        val videoGroups = groups.map { g -> g.mediaItems.orEmpty().mapNotNull { it.toVideo() }.filterNot { it.isShorts } to g }
            .filter { it.first.isNotEmpty() }
        // A aba de vídeos é a paginada (as prateleiras não seguem), como na página do canal.
        val recentGroup = videoGroups.firstOrNull { it.second.nextPageKey != null } ?: videoGroups.firstOrNull()
        val recent = recentGroup?.first.orEmpty()
        val older = videoGroups.filter { it !== recentGroup }.flatMap { it.first }
        return ChannelVideos(channel, ExploreRules.ownVideos(channel, recent), ExploreRules.ownVideos(channel, older))
    }

    /** Lê a página de cada canal inscrito em [channels] e guarda o que ele destaca, de [SAVE_EVERY] em [SAVE_EVERY]. */
    private suspend fun scan(channels: List<Channel>, parallel: Int) {
        val gate = Semaphore(parallel)
        channels.chunked(SAVE_EVERY).forEach { chunk ->
            val read = coroutineScope {
                chunk.map { channel ->
                    async {
                        gate.withPermit {
                            runCatching { channel.id to sourceOf(channel) }
                                .onFailure { Log.w(TAG, "explore: couldn't read ${channel.id}", it) }
                                .getOrNull()
                        }
                    }
                }.awaitAll()
            }.filterNotNull()
            if (read.isNotEmpty()) save { it.copy(sources = it.sources + read) }
        }
    }

    /** Os canais inscritos que faltam ler (ou que ficaram velhos), sem segurar a tela; uma leitura por vez. */
    private fun scanRestLater(stale: List<Channel>) {
        if (stale.isEmpty() || !scanning.compareAndSet(false, true)) return
        AppScope.launch {
            try {
                scan(stale, BACKGROUND_PARALLEL)
            } finally {
                scanning.set(false)
            }
        }
    }

    internal suspend fun sourceOf(channel: Channel): FeaturedSource {
        val items = page(channel.id).flatMap { it.mediaItems.orEmpty() }
        val featured = items.mapNotNull { item ->
            if (item.videoId != null) return@mapNotNull null
            val found = (item.toSearchItem() as? SearchItem.ChannelItem)?.channel ?: return@mapNotNull null
            // Os itens das abas ("Videos", "Shorts") apontam para o próprio canal.
            if (found.id == channel.id || !found.id.startsWith("UC")) return@mapNotNull null
            FeaturedChannel(found.id, found.title, ExploreRules.handleOf(listOf(item.secondTitle?.toString())), found.thumbnailUrl)
        }.distinctBy { it.id }
        val handle = ExploreRules.handleOf(items.filter { it.videoId != null }.map { it.author })
        return FeaturedSource(channel.title, handle, System.currentTimeMillis(), featured)
    }

    private suspend fun remember(channels: List<String>) = save { cache ->
        val recent = (cache.recent - channels.toSet()) + channels
        cache.copy(recent = recent.takeLast(ExploreRules.REMEMBER_CHANNELS))
    }

    private suspend fun load(): ExploreCache = mutex.withLock { loadLocked() }

    private suspend fun loadLocked(): ExploreCache = cache ?: io {
        runCatching { json.decodeFromString(ExploreCache.serializer(), file.readText()) }.getOrDefault(ExploreCache())
    }.also { cache = it }

    private suspend fun save(change: (ExploreCache) -> ExploreCache) = mutex.withLock {
        val updated = change(loadLocked())
        cache = updated
        io { file.writeText(json.encodeToString(ExploreCache.serializer(), updated)) }
    }
}
