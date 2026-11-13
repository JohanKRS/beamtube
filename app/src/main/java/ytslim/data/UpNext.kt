package ytslim.data

/**
 * O fim do vídeo (1.0, pedido do Johan, 2026-11-13: até ali o vídeo só ficava parado no fim). Sem
 * próximo na sequência da playlist, o player mostra até [MAX] sugestões e um "Back" já com o foco;
 * sem ninguém mexer por [BACK_AFTER_MS], volta sozinho para a tela de onde o vídeo foi aberto
 * (Inscrições, Recomendados, a playlist…). Em Settings dá para trocar por voltar direto, tocar a
 * primeira sugestão ou ficar parado ([EndAction]).
 *
 * O modo dormir vem antes de tudo ([sleeping]): com ele, o fim continua como sempre foi. Sem
 * Android, para dar para testar no JVM.
 */
object UpNext {
    /** Sugestões na tela do fim. */
    const val MAX = 6

    /** Sem toque nem tecla por este tempo, a tela do fim volta para a tela de onde o vídeo veio. */
    const val BACK_AFTER_MS = 15_000L

    /** Com "Play a suggestion", a primeira começa depois deste tempo (dá para escolher outra ou voltar). */
    const val NEXT_AFTER_MS = 8_000L

    /** Visto até aqui (%) já conta como assistido e não é sugerido de novo. */
    private const val WATCHED = 90

    /** Quantos vídeos depois do aberto ficam guardados da lista de onde ele veio. */
    private const val ORIGIN_KEPT = 3

    /** A lista de onde o último vídeo foi aberto: o id dele e os vídeos que vinham depois. */
    @Volatile
    private var origin: Pair<String, List<Video>>? = null

    /** Um vídeo foi aberto a partir de [list] (as Inscrições, os Recomendados, a Explore, um canal). */
    fun opened(list: List<Video>, video: Video) {
        val index = list.indexOfFirst { it.id == video.id }
        origin = video.id to if (index >= 0) list.subList(index + 1, minOf(list.size, index + 1 + ORIGIN_KEPT)).toList() else emptyList()
    }

    /** Os vídeos que vinham depois de [videoId] na lista de onde ele foi aberto (vazio se veio de outro lugar). */
    fun after(videoId: String): List<Video> = origin?.takeIf { it.first == videoId }?.second.orEmpty()

    /**
     * As sugestões, nesta ordem: o próximo da lista de onde o vídeo veio, um do mesmo canal e os
     * relacionados que o YouTube manda na página do vídeo, com os dos tópicos de "see more" na
     * frente. Ficam de fora o próprio vídeo, Shorts, lives, os já assistidos, os canais bloqueados
     * e os tópicos de "see less", como nas Recomendações.
     */
    fun pick(
        currentId: String,
        channelId: String?,
        channelName: String?,
        related: List<Video>,
        after: List<Video>,
        filters: ContentFilters,
        max: Int = MAX,
    ): List<Video> {
        fun ok(video: Video) =
            video.id != currentId && video.title.isNotBlank() && !video.isShorts && !video.isLive && !video.isUpcoming &&
                video.percentWatched < WATCHED && !TopicRules.isExcluded(video, filters)

        val channel = channelName?.let { TopicRules.normalize(Handles.channelName(it).orEmpty()) }?.ifEmpty { null }
        fun sameChannel(video: Video) =
            (channelId != null && video.channelId == channelId) ||
                (channel != null && TopicRules.normalize(Handles.channelName(video.channelName).orEmpty()) == channel)

        val picked = LinkedHashMap<String, Video>()
        after.firstOrNull(::ok)?.let { picked[it.id] = it }
        related.firstOrNull { ok(it) && sameChannel(it) }?.let { picked.putIfAbsent(it.id, it) }
        val others = related.filter(::ok)
        for (video in others.filter { TopicRules.isPreferred(it, filters) } + others) {
            if (picked.size >= max) break
            picked.putIfAbsent(video.id, video)
        }
        return picked.values.take(max)
    }

    /**
     * O modo dormir está em uso: o vídeo escurecido, um limite no "Then play" ou "Turn off". Aí o
     * fim fica como sempre foi (parado, ou a TV desligando), sem tela de sugestões.
     */
    fun sleeping(brightness: Int, queueLimited: Boolean, turnOff: Boolean): Boolean =
        brightness < 100 || queueLimited || turnOff
}
