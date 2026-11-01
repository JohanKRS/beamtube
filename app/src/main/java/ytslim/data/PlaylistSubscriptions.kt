package ytslim.data

import android.content.Context
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

/**
 * Inscrição em playlists (o YouTube não tem). Decisão do Johan: a lista de inscrições são as
 * playlists de outros canais salvas na conta, para ser a mesma em todos os aparelhos.
 * Inscrever = salvar na biblioteca; cancelar = tirar da biblioteca.
 */
object PlaylistSubscriptions {
    /** Qualquer vídeo serve: a lista de "salvar em" traz só as playlists do próprio usuário. */
    private const val PROBE_VIDEO = "jNQXAC9IVRw"

    /** Páginas lidas por playlist. Playlists muito grandes em ordem "mais antigos primeiro" passam disso. */
    private const val MAX_PAGES = 10
    private const val PARALLEL = 4

    private lateinit var ledger: LedgerStore

    fun init(context: Context) {
        ledger = LedgerStore(File(context.filesDir, "playlist_ledger.json"))
    }

    /** IDs das playlists do próprio usuário (com Assistir mais tarde). null se não deu para saber. */
    suspend fun ownPlaylistIds(): Set<String>? = runCatching {
        io { Engine.mediaItems.getPlaylistsInfo(PROBE_VIDEO) }.orEmpty().mapNotNull { it.playlistId }.toSet()
    }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** Playlist de outro canal? Mixes ("RD…") e listas embutidas (WL, LL) ficam de fora. */
    fun isForeign(playlistId: String?, own: Set<String>): Boolean =
        playlistId != null && playlistId.length >= 5 && !playlistId.startsWith("RD") && playlistId !in own

    /** As playlists inscritas: as de outros canais que estão salvas na conta. */
    suspend fun subscribed(): List<Playlist> {
        val own = ownPlaylistIds() ?: return emptyList()
        return LibraryRepository.playlists().filter { isForeign(it.playlistId, own) }
    }

    sealed interface Status {
        /** Playlist do próprio usuário, ou não deu para saber: sem botão de inscrição. */
        data object NotApplicable : Status
        data class Foreign(val subscribed: Boolean) : Status
    }

    suspend fun status(playlistId: String?): Status {
        val own = ownPlaylistIds() ?: return Status.NotApplicable
        if (!isForeign(playlistId, own)) return Status.NotApplicable
        val saved = LibraryRepository.playlists().any { it.playlistId == playlistId }
        return Status.Foreign(saved)
    }

    suspend fun subscribe(playlist: Playlist) {
        val id = requireNotNull(playlist.playlistId)
        Engine.mediaItems.savePlaylistObserve(id).complete()
        // O que já está na playlist agora não é "novo".
        fetchAll(playlist)?.let { videos -> ledger.record(id, videos.map { it.id }, System.currentTimeMillis()) }
    }

    suspend fun unsubscribe(playlistId: String) {
        // O motor, se não conseguir tirar uma playlist alheia da biblioteca, tenta APAGAR a playlist
        // como se fosse do usuário. Por isso só seguimos com a lista de playlists próprias em mãos.
        val own = checkNotNull(ownPlaylistIds()) { "Couldn't check your playlists" }
        check(isForeign(playlistId, own)) { "This is your own playlist" }
        Engine.mediaItems.removePlaylistObserve(playlistId).complete()
    }

    /**
     * Vídeos que entraram nas playlists inscritas depois da inscrição, sem os de canais
     * inscritos (esses já vêm pelo feed dos canais).
     */
    suspend fun newVideos(subscribedChannels: Set<String>): List<Timed> = coroutineScope {
        val gate = Semaphore(PARALLEL)
        subscribed().map { playlist ->
            async { gate.withPermit { newIn(playlist, subscribedChannels) } }
        }.awaitAll().flatten()
    }

    private suspend fun newIn(playlist: Playlist, subscribedChannels: Set<String>): List<Timed> {
        val id = playlist.playlistId ?: return emptyList()
        // Se a leitura falhar no meio, não atualiza: senão a parte que faltou viraria "nova" depois.
        val videos = fetchAll(playlist) ?: return emptyList()
        val fresh = Ledger.newSince(ledger.record(id, videos.map { it.id }, System.currentTimeMillis()))
        return videos
            .filter { it.id in fresh && (it.channelId == null || it.channelId !in subscribedChannels) }
            .map { Timed(it.copy(fromPlaylist = playlist.title), fresh.getValue(it.id)) }
    }

    private suspend fun fetchAll(playlist: Playlist): List<Video>? = runCatching {
        var page = LibraryRepository.playlistVideos(playlist)
        val videos = page.videos.toMutableList()
        repeat(MAX_PAGES - 1) {
            if (!page.hasMore) return@runCatching videos
            page = LibraryRepository.more(page)
            videos += page.videos
        }
        videos
    }.getOrNull()
}
