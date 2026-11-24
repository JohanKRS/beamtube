package ytslim.data

import com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.rx2.awaitFirstOrNull
import kotlinx.coroutines.withContext

/** Playlists da conta e os vídeos de cada uma. */
object LibraryRepository {
    internal const val PLAYLISTS_MAX_AGE_MS = 60_000L

    private val lock = Any()
    private var lastPlaylists: Pair<Long, List<Playlist>>? = null
    private var inFlight: Pair<Int, Deferred<List<Playlist>>>? = null
    private var generation = 0

    /** Uma playlist entrou ou saiu da conta (ou trocou de usuário): a próxima leitura vai à conta. */
    fun forgetPlaylists() = synchronized(lock) {
        generation++
        lastPlaylists = null
        inFlight = null
    }

    /**
     * Todas as playlists da conta, com o "Assistir mais tarde" (o real, da conta).
     * Com [maxAgeMs] > 0, reaproveita uma leitura recente ou em andamento (a abertura do app pede
     * esta lista de vários lugares ao mesmo tempo).
     */
    suspend fun playlists(maxAgeMs: Long = 0): List<Playlist> {
        val now = System.currentTimeMillis()
        val deferred = synchronized(lock) {
            if (maxAgeMs > 0) {
                lastPlaylists?.let { (at, list) -> if (now - at < maxAgeMs) return list }
                inFlight?.second?.takeIf { !it.isCompleted }?.let { return@synchronized it }
            }
            val gen = generation
            AppScope.async(Dispatchers.IO) {
                try {
                    val group = Engine.content.playlistsObserve.awaitFirstOrNull()
                    val list = group?.mediaItems.orEmpty().mapNotNull { it.toPlaylist() }
                    synchronized(lock) {
                        if (generation == gen) lastPlaylists = System.currentTimeMillis() to list
                    }
                    list
                } finally {
                    synchronized(lock) {
                        if (inFlight?.first == gen) inFlight = null
                    }
                }
            }.also { inFlight = gen to it }
        }
        return deferred.await()
    }

    /** Primeira página de vídeos de uma playlist. */
    suspend fun playlistVideos(playlist: Playlist): VideoPage =
        Engine.content.getChannelObserve(playlistKey(playlist)).firstOrNullMapped { groups ->
            groups?.firstOrNull { !it.isEmpty }.toPage()
        }

    /** Só os IDs dos vídeos de uma playlist (até [maxPages] páginas), sem montar os cards. */
    internal suspend fun playlistVideoIds(playlist: Playlist, maxPages: Int): Set<String> = withContext(Dispatchers.IO) {
        val ids = HashSet<String>()
        var group = Engine.content.getChannelObserve(playlistKey(playlist)).awaitFirstOrNull()?.firstOrNull { !it.isEmpty }
        var read = 0
        while (group != null && read < maxPages) {
            group.mediaItems.orEmpty().forEach { item -> item.videoId?.let(ids::add) }
            read++
            if (read >= maxPages || group.nextPageKey == null) break
            group = Engine.content.continueGroup(group)
        }
        ids
    }

    private fun playlistKey(playlist: Playlist) = YouTubeMediaItem().apply {
        setChannelId(playlist.browseId)
        setParams(playlist.params)
        setTitle(playlist.title)
        setPlaylistId(playlist.playlistId)
    }

    /** Histórico da conta (o que foi assistido), do mais recente para o mais antigo. */
    suspend fun history(): VideoPage = Engine.content.historyObserve.firstOrNullMapped { it.toPage() }

    /**
     * Tira [video] do histórico da conta. No Histórico, o menu de cada cartão traz o pedido
     * "Remove from watch history" do YouTube; o motor manda esse pedido como manda o "Not
     * interested" (o mesmo endpoint de feedback).
     */
    suspend fun removeFromHistory(video: Video) {
        val token = checkNotNull(video.feedbackToken) { "YouTube didn't send a way to remove this video" }
        io { Engine.mediaItems.markAsNotInterested(token) }
    }

    /** Recomendações da conta (o "Início" do YouTube), ainda sem os nossos filtros. */
    suspend fun recommended(): VideoPage = Engine.content.recommendedObserve.firstOrNullMapped { it.toPage(home = true) }

    /** Próxima página de uma lista (playlist, feed etc.). */
    suspend fun more(page: VideoPage): VideoPage {
        val next = page.next ?: return VideoPage(emptyList(), null)
        return io { Engine.content.continueGroup(next).toPage(page.home) }
    }
}
