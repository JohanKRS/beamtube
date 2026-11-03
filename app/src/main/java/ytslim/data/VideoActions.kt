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
        )
    }

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

    suspend fun setSubscribed(channelId: String, subscribed: Boolean) = io {
        if (subscribed) Engine.mediaItems.subscribe(channelId) else Engine.mediaItems.unsubscribe(channelId)
    }
}
