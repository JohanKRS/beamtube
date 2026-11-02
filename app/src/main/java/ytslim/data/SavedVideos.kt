package ytslim.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Os vídeos que estão na playlist do botão Save (o Watch later, ou a escolhida nas Configurações).
 * Servem para o marcador discreto nas miniaturas e para o toque longo (OK longo na TV), que põe ou
 * tira o vídeo de lá.
 *
 * A lista local é lida até [MAX_PAGES] páginas e pode estar incompleta numa playlist enorme. Por
 * isso o toque longo confere na hora, com o YouTube, se o vídeo já está lá.
 */
object SavedVideos {
    private const val MAX_PAGES = 10

    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    private val _target = MutableStateFlow(SaveTarget())
    /** A playlist do botão Save, para os textos ("Saved to Watch later"). */
    val target: StateFlow<SaveTarget> = _target.asStateFlow()

    private val lock = Any()
    private var base: Set<String> = emptySet()

    /** Mudanças feitas neste aparelho (vídeo -> salvo?), com a hora, por cima da lista lida. */
    private val changes = HashMap<String, Pair<Boolean, Long>>()

    /** Lê a playlist do botão Save. Chamado ao abrir o app e quando a playlist escolhida muda. */
    suspend fun refresh() {
        if (!AuthRepository.isSigned()) return
        val target = SettingsRepository.current().saveTarget
        _target.value = target
        val started = System.nanoTime()
        val found = runCatching { readIds(target.playlistId) }.getOrNull() ?: return
        synchronized(lock) {
            base = found
            // O que mudou antes desta leitura começar já está nela.
            changes.values.removeAll { (_, at) -> at < started }
            publish()
        }
    }

    /** Anota uma mudança feita no app (toque longo ou o botão Save do player). */
    fun mark(videoId: String, saved: Boolean) = synchronized(lock) {
        changes[videoId] = saved to System.nanoTime()
        publish()
    }

    /** Põe ou tira o vídeo da playlist do botão Save. Devolve true se ele ficou salvo. */
    suspend fun toggle(videoId: String): Boolean {
        val target = SettingsRepository.current().saveTarget
        _target.value = target
        val savedNow = PlaylistEditor.isSaved(videoId, target.playlistId) ?: (videoId in _ids.value)
        PlaylistEditor.setSaved(videoId, target.playlistId, !savedNow)
        mark(videoId, !savedNow)
        return !savedNow
    }

    private fun publish() {
        val result = base.toHashSet()
        changes.forEach { (id, change) -> if (change.first) result += id else result -= id }
        _ids.value = result
    }

    private suspend fun readIds(playlistId: String): Set<String> {
        // A playlist como a biblioteca da conta a entrega (o Watch later tem um jeito próprio de abrir).
        val playlist = runCatching { LibraryRepository.playlists() }.getOrNull()
            ?.firstOrNull { it.playlistId == playlistId }
            ?: Playlist("VL$playlistId", params = null, playlistId = playlistId, title = "", info = null, thumbnailUrl = null)
        val ids = HashSet<String>()
        var page = LibraryRepository.playlistVideos(playlist)
        page.videos.mapTo(ids) { it.id }
        var read = 1
        while (page.hasMore && read < MAX_PAGES) {
            page = LibraryRepository.more(page)
            page.videos.mapTo(ids) { it.id }
            read++
        }
        return ids
    }
}

/** Fotos dos canais (ID do canal -> URL), vindas da lista de canais inscritos. */
object ChannelPhotos {
    private val _photos = MutableStateFlow<Map<String, String>>(emptyMap())
    val photos: StateFlow<Map<String, String>> = _photos.asStateFlow()

    fun putAll(found: Map<String, String>) {
        if (found.isNotEmpty()) _photos.value = _photos.value + found
    }

    /** A foto às vezes vem sem o "https:" ("//yt3.ggpht.com/…"). */
    fun normalize(url: String?): String? = url?.takeIf { it.isNotBlank() }?.let { if (it.startsWith("//")) "https:$it" else it }
}
