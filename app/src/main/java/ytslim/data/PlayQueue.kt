package ytslim.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A sequência de uma playlist: um vídeo aberto pela página da playlist é seguido pelos próximos da
 * lista quando acaba. Quantos ainda tocam ([left]) se escolhe só no player, no painel do modo
 * dormir; cada sequência começa com todos. Abrir um vídeo de qualquer outro lugar limpa a sequência.
 *
 * Usado só na thread da tela.
 */
object PlayQueue {
    /** [left] sem limite: toca até a playlist acabar. */
    const val ALL = -1

    /** As opções do painel, em ordem. */
    val CHOICES = listOf(0, 1, 2, 3, 4, 5, ALL)

    private val upcoming = ArrayDeque<Video>()
    private val seen = HashSet<String>()

    /** A última página lida da playlist, para buscar mais vídeos quando os da fila acabarem. */
    private var page: VideoPage? = null

    private val _active = MutableStateFlow(false)
    /** O vídeo atual faz parte de uma sequência (veio da página de uma playlist)? */
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _left = MutableStateFlow(ALL)
    /** Quantos vídeos ainda tocam depois do atual ([ALL] = até a playlist acabar). */
    val left: StateFlow<Int> = _left.asStateFlow()

    /**
     * Começa uma sequência com os vídeos de [videos] que vêm depois de [videoId].
     * @param page a última página carregada da lista, para continuar além do que está na tela.
     */
    fun start(videos: List<Video>, videoId: String, page: VideoPage?) {
        clear()
        val from = videos.indexOfFirst { it.id == videoId }
        if (from < 0) return
        seen += videoId
        videos.drop(from + 1).filterTo(upcoming) { playable(it) && seen.add(it.id) }
        // Os de antes do atual também contam como vistos: páginas novas não os trazem de volta.
        videos.take(from).mapTo(seen) { it.id }
        this.page = page?.takeIf { it.hasMore }
        _active.value = true
    }

    fun clear() {
        upcoming.clear()
        seen.clear()
        page = null
        _active.value = false
        _left.value = ALL
    }

    /** Escolha do painel do player: [count] vídeos depois do atual, ou [ALL]. */
    fun setLeft(count: Int) {
        _left.value = count
    }

    /**
     * O próximo vídeo da sequência, ou null quando ela acabou (sem vídeos, ou o limite chegou a zero).
     *
     * @param counts false quando o vídeo anterior da sequência não tocou (apagado, privado): o
     * próximo toma o lugar dele sem gastar o limite.
     */
    suspend fun next(counts: Boolean = true): Video? {
        if (!_active.value) return null
        if (counts) {
            val left = _left.value
            if (left == 0) return null
            if (left != ALL) _left.value = left - 1
        }
        while (upcoming.isEmpty()) {
            val current = page ?: break
            val more = runCatching { LibraryRepository.more(current) }.getOrNull() ?: break
            // Outra sequência começou (ou esta foi limpa) enquanto a página carregava.
            if (page !== current) return null
            page = more.takeIf { it.hasMore }
            more.videos.filterTo(upcoming) { playable(it) && seen.add(it.id) }
        }
        return upcoming.removeFirstOrNull()
    }

    /**
     * O vídeo que [next] daria agora, sem avançar a sequência nem buscar páginas: para resolver o
     * próximo antes de o atual acabar. null se não há (ou o limite do painel chegou a zero).
     */
    fun peek(): Video? {
        if (!_active.value || _left.value == 0) return null
        return upcoming.firstOrNull()
    }

    /** Agendados ainda não têm o que tocar. */
    private fun playable(video: Video) = !video.isUpcoming
}
