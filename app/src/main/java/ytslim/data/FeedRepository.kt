package ytslim.data

/** Feed de Inscrições (canais) e a lista de canais inscritos. */
object FeedRepository {
    private const val MAX_CHANNEL_PAGES = 30

    /** Primeira página do feed dos canais inscritos, do mais novo para o mais antigo. */
    suspend fun subscriptions(): VideoPage = Engine.content.subscriptionsObserve.firstOrNull().toPage()

    /** IDs dos canais inscritos, para a regra de não duplicar vídeos de playlists (e as fotos, em [ChannelPhotos]). null se falhar. */
    suspend fun subscribedChannelIds(): Set<String>? = runCatching {
        var group = Engine.content.subscribedChannelsObserve.firstOrNull() ?: return@runCatching null
        val ids = HashSet<String>()
        val photos = HashMap<String, String>()
        for (page in 1..MAX_CHANNEL_PAGES) {
            group.mediaItems.orEmpty().forEach { item ->
                val id = item.channelId ?: return@forEach
                ids += id
                // De quebra, a foto de cada canal, para o avatar nos cards dos vídeos.
                ChannelPhotos.normalize(item.cardImageUrl)?.let { photos[id] = it }
            }
            if (group.nextPageKey == null) break
            group = io { Engine.content.continueGroup(group) } ?: break
        }
        ChannelPhotos.putAll(photos)
        ids
    }.getOrNull()
}
