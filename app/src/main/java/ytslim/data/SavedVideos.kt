package ytslim.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Os vídeos que estão na playlist do botão Save (o Watch later, ou a escolhida nas Configurações).
 * Servem para o marcador discreto nas miniaturas, para o toque longo (OK longo na TV) e para o
 * botão Save do player, que põem ou tiram o vídeo de lá.
 *
 * O toque muda o marcador e mostra o aviso na hora; a conta é atualizada logo depois, em segundo
 * plano. Tocar de novo antes de a conta responder só muda o que o usuário quer: no fim, a conta
 * fica como o último toque, sem pedidos repetidos nem invertidos. Se a conta recusar, o marcador
 * volta e um aviso diz que falhou.
 *
 * A lista local é lida até [MAX_PAGES] páginas e pode estar incompleta numa playlist enorme. Por
 * isso, antes de mudar um vídeo pela primeira vez, o app confere com o YouTube se ele já está lá.
 */
object SavedVideos {
    private const val MAX_PAGES = 10

    /** Depois de mudar um vídeo, o app confia no que mandou por este tempo (a leitura do YouTube demora a refletir). */
    private const val TRUST_OWN_WRITE_NS = 10 * 60 * 1_000_000_000L

    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids.asStateFlow()

    private val _target = MutableStateFlow(SaveTarget())
    /** A playlist do botão Save, para os textos ("Saved to Watch later"). */
    val target: StateFlow<SaveTarget> = _target.asStateFlow()

    private val lock = Any()
    private var base: Set<String> = emptySet()

    /** Mudanças feitas neste aparelho (vídeo -> salvo?), com a hora, por cima da lista lida. */
    private val changes = HashMap<String, Pair<Boolean, Long>>()

    /** O que o usuário pediu e a conta ainda não confirmou (vídeo -> salvo?). */
    private val wanted = HashMap<String, Boolean>()

    /** Quem avisar se a conta recusar a mudança de um vídeo (o último toque). */
    private val onFailure = HashMap<String, (Throwable) -> Unit>()

    /** O que o app já gravou na conta (vídeo -> salvo?, hora). */
    private val written = HashMap<String, Pair<Boolean, Long>>()

    /** Lê a playlist do botão Save. Chamado ao abrir o app e quando a playlist escolhida muda. */
    suspend fun refresh() {
        if (!AuthRepository.isSigned()) return
        val target = SettingsRepository.current().saveTarget
        _target.value = target
        val started = System.nanoTime()
        val found = runCatching { readIds(target.playlistId) }.getOrNull() ?: return
        synchronized(lock) {
            base = found
            // O que mudou antes desta leitura começar já está nela (menos o que ainda vai para a conta).
            changes.entries.removeAll { (id, change) -> change.second < started && id !in wanted }
            if (written.isNotEmpty()) written.clear()
            publish()
        }
    }

    /** O vídeo está salvo, pelo que o app sabe agora (inclui os toques que ainda vão para a conta)? */
    fun isSaved(videoId: String): Boolean = videoId in _ids.value

    /** O que este aparelho mudou no vídeo desde a última leitura da playlist (null = nada). */
    fun changedHere(videoId: String): Boolean? = synchronized(lock) { wanted[videoId] ?: changes[videoId]?.first }

    /**
     * Põe ou tira o vídeo da playlist do botão Save e devolve como ele ficou (true = salvo). Volta
     * na hora: a conta é atualizada em segundo plano, e [failed] é chamado se ela recusar.
     *
     * @param current o estado que a tela mostra, quando ela sabe melhor que a lista local (o player
     * confere na conta ao abrir o vídeo).
     */
    fun toggle(videoId: String, current: Boolean? = null, failed: (Throwable) -> Unit = {}): Boolean {
        val (saved, idle) = synchronized(lock) {
            val saved = !(wanted[videoId] ?: current ?: isSaved(videoId))
            val idle = videoId !in wanted
            wanted[videoId] = saved
            onFailure[videoId] = failed
            markLocked(videoId, saved)
            saved to idle
        }
        if (idle) AppScope.launch { sync(videoId) }
        return saved
    }

    /** Leva a conta até o que o usuário quer, um pedido por vez, até não sobrar mudança pendente. */
    private suspend fun sync(videoId: String) {
        val playlist = (runCatching { SettingsRepository.current().saveTarget }.getOrNull() ?: _target.value)
            .also { _target.value = it }.playlistId
        var server: Boolean? = synchronized(lock) {
            written[videoId]?.takeIf { System.nanoTime() - it.second < TRUST_OWN_WRITE_NS }?.first
        }
        while (true) {
            val want = synchronized(lock) { wanted[videoId] } ?: return
            try {
                if (server == null) server = PlaylistEditor.isSaved(videoId, playlist)
                if (server != want) PlaylistEditor.setSaved(videoId, playlist, want)
                server = want
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                val notify = synchronized(lock) {
                    wanted.remove(videoId)
                    // Volta para o que a conta tem (ou, sem saber, para antes do toque).
                    markLocked(videoId, server ?: !want)
                    onFailure.remove(videoId)
                }
                notify?.invoke(e)
                return
            }
            synchronized(lock) {
                written[videoId] = want to System.nanoTime()
                if (wanted[videoId] == want) {
                    wanted.remove(videoId)
                    onFailure.remove(videoId)
                    return
                }
            }
        }
    }

    /** Anota uma mudança feita no app (chamar com o [lock]). */
    private fun markLocked(videoId: String, saved: Boolean) {
        changes[videoId] = saved to System.nanoTime()
        publish()
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
