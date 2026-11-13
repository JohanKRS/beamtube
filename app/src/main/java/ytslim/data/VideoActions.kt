package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata
import com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem

enum class Rating { NONE, LIKED, DISLIKED }

/** O que a página do vídeo mostra além do player: avaliação e inscrição no canal. */
data class VideoState(
    /** O título pela página do vídeo: o player usa quando o stream vem sem título. */
    val title: String?,
    val channelId: String?,
    val channelName: String?,
    val subscribed: Boolean,
    val rating: Rating,
    /** A data de publicação por extenso ("Nov 10, 2025"); o player mostra como idade ([Published.age]). */
    val published: String? = null,
    /** Os vídeos relacionados da página do vídeo, para a tela do fim ([UpNext]). Sem mixes. */
    val related: List<Video> = emptyList(),
)

/** Like/dislike e inscrever/cancelar inscrição no canal do vídeo. */
object VideoActions {
    suspend fun state(videoId: String): VideoState? = runCatching {
        io { Engine.mediaItems.getMetadata(videoId) }
    }.getOrNull()?.let { m ->
        VideoState(
            title = m.title?.takeIf { it.isNotBlank() },
            channelId = m.channelId,
            channelName = Handles.channelName(m.author),
            subscribed = m.isSubscribed,
            rating = when (m.likeStatus) {
                MediaItemMetadata.LIKE_STATUS_LIKE -> Rating.LIKED
                MediaItemMetadata.LIKE_STATUS_DISLIKE -> Rating.DISLIKED
                else -> Rating.NONE
            },
            published = m.publishedDate?.takeIf { it.isNotBlank() },
            related = m.suggestions.orEmpty()
                .flatMap { group -> group.mediaItems.orEmpty().mapNotNull { it.toVideo() } }
                // Os mixes ("RD…") são listas sem fim, não um vídeo.
                .filter { it.playlistId?.startsWith("RD") != true },
        )
    }

    /** Os canais já achados pela página do vídeo ([channel]), por id do vídeo. */
    private val channels = object : LinkedHashMap<String, Channel>(CHANNELS_KEPT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Channel>?) = size > CHANNELS_KEPT
    }

    /**
     * O canal de um vídeo pela página dele. Os cards das Recomendações vêm sem o id do canal (o
     * YouTube não manda), e o "Go to channel" precisa dele (pedido do Johan, 2026-11-13).
     */
    suspend fun channel(videoId: String): Channel? {
        synchronized(channels) { channels[videoId] }?.let { return it }
        val m = runCatching { io { Engine.mediaItems.getMetadata(videoId) } }.getOrNull() ?: return null
        val id = m.channelId?.takeIf { it.isNotBlank() } ?: return null
        val channel = Channel(id, Handles.channelName(m.author).orEmpty(), info = null, thumbnailUrl = null)
        synchronized(channels) { channels[videoId] = channel }
        return channel
    }

    private const val CHANNELS_KEPT = 200

    /** Leva a avaliação de [from] para [to] (tocar no botão já ativo desfaz). */
    suspend fun rate(videoId: String, from: Rating, to: Rating) = io {
        val item = YouTubeMediaItem().apply { setVideoId(videoId) }
        val service = Engine.mediaItems
        when (to) {
            Rating.LIKED -> service.setLike(item)
            Rating.DISLIKED -> service.setDislike(item)
            Rating.NONE -> when (from) {
                Rating.LIKED -> service.removeLike(item)
                Rating.DISLIKED -> service.removeDislike(item)
                Rating.NONE -> Unit
            }
        }
    }

    suspend fun setSubscribed(channelId: String, subscribed: Boolean) {
        io { if (subscribed) Engine.mediaItems.subscribe(channelId) else Engine.mediaItems.unsubscribe(channelId) }
        // A Explore não sugere canais inscritos: ela relê a lista.
        FeedRepository.forgetChannels()
    }
}
