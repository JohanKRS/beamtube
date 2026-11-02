package ytslim.data

/** Feed de Inscrições (canais) e a lista de canais inscritos. */
object FeedRepository {
    private const val MAX_CHANNEL_PAGES = 30

    /** Primeira página do feed dos canais inscritos, do mais novo para o mais antigo. */
    suspend fun subscriptions(): VideoPage = Engine.content.subscriptionsObserve.firstOrNull().toPage()

    /** IDs dos canais inscritos, para a regra de não duplicar vídeos de playlists (e as fotos, em [ChannelPhotos]). null se falhar. */
    suspend fun subscribedChannelIds(): Set<String>? = subscribedChannels(maxAgeMs = 0)?.mapTo(HashSet()) { it.id }

    /**
     * Os canais inscritos (id, nome e foto). Uma lista lida há menos de [maxAgeMs] serve de novo
     * (a Explore aproveita a que o feed acabou de ler). null se falhar.
     */
    suspend fun subscribedChannels(maxAgeMs: Long = CHANNELS_MAX_AGE_MS): List<Channel>? {
        lastChannels?.let { (at, channels) -> if (System.currentTimeMillis() - at < maxAgeMs) return channels }
        return runCatching {
            var group = Engine.content.subscribedChannelsObserve.firstOrNull() ?: return@runCatching null
            val channels = LinkedHashMap<String, Channel>()
            for (page in 1..MAX_CHANNEL_PAGES) {
                group.mediaItems.orEmpty().forEach { item ->
                    val id = item.channelId ?: return@forEach
                    // De quebra, a foto de cada canal, para o avatar nos cards dos vídeos.
                    channels[id] = Channel(id, Handles.channelName(item.title).orEmpty(), null, ChannelPhotos.normalize(item.cardImageUrl))
                }
                if (group.nextPageKey == null) break
                group = io { Engine.content.continueGroup(group) } ?: break
            }
            ChannelPhotos.putAll(channels.values.mapNotNull { c -> c.thumbnailUrl?.let { c.id to it } }.toMap())
            channels.values.toList()
        }.getOrNull()?.also { lastChannels = System.currentTimeMillis() to it }
    }

    @Volatile
    private var lastChannels: Pair<Long, List<Channel>>? = null

    private const val CHANNELS_MAX_AGE_MS = 30 * 60 * 1000L
}
