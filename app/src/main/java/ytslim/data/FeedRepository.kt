package ytslim.data

/** Feed de Inscrições (canais) e a lista de canais inscritos. */
object FeedRepository {
    private const val MAX_CHANNEL_PAGES = 30

    /** Primeira página do feed dos canais inscritos, do mais novo para o mais antigo. */
    suspend fun subscriptions(): VideoPage = Engine.content.subscriptionsObserve.firstOrNull().toPage()

    /** IDs dos canais inscritos, para a regra de não duplicar vídeos de playlists. null se falhar. */
    suspend fun subscribedChannelIds(): Set<String>? = runCatching {
        var group = Engine.content.subscribedChannelsObserve.firstOrNull() ?: return@runCatching null
        val ids = HashSet<String>()
        for (page in 1..MAX_CHANNEL_PAGES) {
            group.mediaItems.orEmpty().mapNotNullTo(ids) { it.channelId }
            if (group.nextPageKey == null) break
            group = io { Engine.content.continueGroup(group) } ?: break
        }
        ids
    }.getOrNull()
}
