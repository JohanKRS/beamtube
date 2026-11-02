package ytslim.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Um tema achado no histórico. [key] é a palavra (ou o par de palavras) em minúsculas, que serve
 * de id da categoria; [name] é como ela aparece nos títulos ("Star Wars", "iPhone").
 */
data class Theme(val key: String, val name: String, val videos: Int, val channels: Int)

/** Os temas da Explore como ficam guardados: as categorias e quando foram achadas. */
@Serializable
data class ThemeSet(val updatedAt: Long, val categories: List<Category>)

/**
 * Os temas que se repetem no que o usuário assiste (escolha do Johan em 2026-11-02): as palavras
 * e os pares de palavras dos títulos do histórico que aparecem em vários vídeos de canais
 * diferentes. Um tema de um canal só seria o próprio canal, que já está nas Inscrições.
 */
object ThemeRules {
    const val MAX_THEMES = 12
    const val MIN_VIDEOS = 3
    const val MIN_CHANNELS = 2

    /** Dois temas com quase os mesmos vídeos são o mesmo assunto: fica o de mais peso. */
    private const val SAME_VIDEOS = 0.7

    /** Um par de palavras ("star wars") diz mais que cada palavra sozinha. */
    private const val PAIR_BONUS = 1.5

    private val WORD = Regex("[\\p{L}\\p{N}]+")

    private class Count {
        val videos = HashSet<String>()
        val channels = HashSet<String>()
        val forms = HashMap<String, Int>()
    }

    /**
     * Os temas de [videos], do de mais peso para o de menos. [exclude]: chaves que o usuário
     * escondeu (não voltam).
     */
    fun themes(videos: List<Video>, exclude: Set<String> = emptySet(), max: Int = MAX_THEMES): List<Theme> {
        val counts = HashMap<String, Count>()
        fun add(key: String, form: String, video: Video, channel: String) {
            val count = counts.getOrPut(key) { Count() }
            if (!count.videos.add(video.id)) return
            count.channels += channel
            count.forms.merge(form, 1, Int::plus)
        }
        videos.distinctBy { it.id }.forEach { video ->
            val channel = video.channelId ?: video.channelName ?: return@forEach
            // As palavras do nome do canal não contam: seriam o canal, não um assunto.
            val channelWords = words(video.channelName.orEmpty()).mapTo(HashSet()) { lower(it) }
            val forms = words(video.title)
            val keys = forms.map { lower(it) }
            val useful = forms.indices.map { isUseful(forms[it], keys[it]) && keys[it] !in channelWords }
            for (i in forms.indices) {
                if (!useful[i]) continue
                add(keys[i], forms[i], video, channel)
                if (i + 1 < forms.size && useful[i + 1]) add("${keys[i]} ${keys[i + 1]}", "${forms[i]} ${forms[i + 1]}", video, channel)
            }
        }
        val candidates = counts.entries
            .filter { (key, c) -> c.videos.size >= MIN_VIDEOS && c.channels.size >= MIN_CHANNELS && key !in exclude }
            .sortedWith(compareByDescending<Map.Entry<String, Count>> { score(it.key, it.value) }.thenBy { it.key })
        val chosen = ArrayList<Map.Entry<String, Count>>()
        for (candidate in candidates) {
            if (chosen.any { overlap(it.value.videos, candidate.value.videos) >= SAME_VIDEOS }) continue
            chosen += candidate
            if (chosen.size == max) break
        }
        return chosen.map { (key, c) -> Theme(key, display(c.forms), c.videos.size, c.channels.size) }
    }

    private fun score(key: String, count: Count): Double =
        (count.channels.size * 3 + count.videos.size) * (if (' ' in key) PAIR_BONUS else 1.0)

    private fun overlap(a: Set<String>, b: Set<String>): Double = a.count { it in b }.toDouble() / minOf(a.size, b.size)

    /** A forma mais comum nos títulos; toda em minúsculas ganha a primeira letra maiúscula. */
    private fun display(forms: Map<String, Int>): String {
        val form = forms.maxWith(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).key
        return if (form == form.lowercase()) form.split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.titlecase(Locale.ROOT) } } else form
    }

    internal fun words(text: String): List<String> = WORD.findAll(text).map { it.value }.toList()

    private fun lower(word: String) = word.lowercase(Locale.ROOT)

    /** Palavras que não são assunto: as comuns do português e do inglês, números e o jargão do YouTube. */
    private fun isUseful(form: String, key: String): Boolean = when {
        key in STOPWORDS -> false
        key.all { it.isDigit() } -> false
        key.length >= 3 -> true
        // Siglas de duas letras ("AI", "F1") valem; "de", "of" não.
        else -> key.length == 2 && form == form.uppercase(Locale.ROOT) && form.any { it.isLetter() }
    }

    private val STOPWORDS = (
        // Português
        "a o e é um uma uns umas de do da dos das no na nos nas em por para pra pro com sem sob sobre " +
            "que quem qual quais quando onde como porque porquê por que se sua seu suas seus meu minha meus " +
            "minhas teu tua nosso nossa nossos nossas ele ela eles elas eu tu você vocês voce voces nós nos " +
            "isso isto aquilo esse essa esses essas este esta estes estas aquele aquela mais menos muito muita " +
            "muitos muitas pouco tudo todo toda todos todas nada algo alguém ninguém cada outro outra outros " +
            "outras mesmo mesma já ainda também tambem só so até ate não nao sim ou mas mais entre depois antes " +
            "agora hoje ontem amanhã amanha aqui ali lá la então entao foi ser ter tem têm tinha fazer faz " +
            "fiz feito vai vou vamos vem veja ver viu está esta estão estao estou era são sao sou há ha pode " +
            "posso podem fica ficou dia dias ano anos vez vezes coisa coisas jeito forma parte novo nova novos " +
            "novas melhor melhores pior maior menor primeiro primeira último ultimo última ultima grande " +
            "pequeno bom boa bem mal real verdade tipo assim aí cara gente vídeo video vídeos videos " +
            "canal episódio episodio ep temporada parte completo completa oficial ao vivo live shorts short " +
            "reagindo react reação reacao horas hora minutos minuto segundos fácil facil simples rápido rapido " +
            "iniciantes iniciante tutorial aula dicas guia explicado explicando resumo " +
            // Inglês
            "the an and or but of to in on at by for from with without into onto over under about as is " +
            "are was were be been being am it its it's this that these those there here what which who whom " +
            "whose why how when where all any each every some no not only just also very too so than then " +
            "now new old my your his her our their me you he she we they them us i do does did done doing " +
            "have has had having can could will would should may might must shall get got gets make made " +
            "makes go goes going went one two three first last best worst big more most less least out up " +
            "down off again ever never vs ft feat official video videos channel episode part full live " +
            "stream streaming shorts short reaction reacts react trailer hd 4k 8k pt br en us uk " +
            "day days year years time times thing things way world people guy guys hour hours minute minutes " +
            "seconds beginner beginners tutorial tutorials guide explained complete ultimate everything need " +
            "know easy simple quick tips review highlights"
        ).split(' ').filter { it.isNotEmpty() }.toSet()
}

/**
 * A Explore automática: os temas do histórico viram categorias, achados de novo a cada
 * [MAX_AGE_MS] (ou quando o usuário puxa para atualizar). Um tema que continua guarda os filtros
 * e o mosaico que já tinha; os escondidos não voltam.
 */
object ExploreRepository {
    const val MAX_AGE_MS = 3 * 24 * 60 * 60 * 1000L

    /** Quantas páginas do histórico ler (umas 150 a 200 vídeos). */
    private const val HISTORY_PAGES = 5
    private const val HISTORY_VIDEOS = 200

    private val mutex = Mutex()

    /** Acha os temas de novo se a última vez foi há mais de [MAX_AGE_MS] (ou nunca), ou se [force]. */
    suspend fun refresh(force: Boolean, now: Long = System.currentTimeMillis()) = mutex.withLock {
        val current = SettingsRepository.explore.first()
        if (!force && current != null && now - current.updatedAt < MAX_AGE_MS) return@withLock
        val history = history()
        val hidden = SettingsRepository.hiddenThemes.first()
        val topics = SettingsRepository.current().filters.excluded
        val themes = ThemeRules.themes(history, exclude = hidden, max = ThemeRules.MAX_THEMES + topics.size)
            .filterNot { theme -> topics.any { TopicRules.matches(theme.name, it) } }
            .take(ThemeRules.MAX_THEMES)
        val old = current?.categories.orEmpty().associateBy { it.id }
        val categories = themes.map { theme ->
            old[theme.key]?.copy(name = theme.name) ?: Category(theme.key, theme.name, keywords = listOf(theme.name))
        }
        SettingsRepository.saveExplore(ThemeSet(now, categories))
        categories.filter { it.thumbnails.isEmpty() }.forEach { refreshThumbnails(it) }
    }

    /** Busca a primeira página só para o mosaico do cartão (sem mexer em nada se falhar). */
    suspend fun refreshThumbnails(category: Category) {
        val videos = runCatching { CategorySearch.first(category).videos }.getOrNull() ?: return
        storeThumbnails(category.id, videos)
    }

    suspend fun storeThumbnails(id: String, videos: List<Video>) {
        val thumbs = CategoryRules.thumbnails(videos.filterNot { it.isShorts })
        if (thumbs.isEmpty()) return
        SettingsRepository.updateCategory(id) { if (it.thumbnails == thumbs) it else it.copy(thumbnails = thumbs) }
    }

    suspend fun hide(id: String) = SettingsRepository.hideTheme(id)

    private suspend fun history(): List<Video> {
        var page = LibraryRepository.history()
        val videos = ArrayList(page.videos)
        repeat(HISTORY_PAGES - 1) {
            if (!page.hasMore || videos.size >= HISTORY_VIDEOS) return videos
            page = LibraryRepository.more(page)
            videos += page.videos
        }
        return videos
    }
}
