package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem

/** Um vídeo, já convertido do modelo do motor. */
data class Video(
    val id: String,
    val title: String,
    val channelName: String?,
    val channelId: String?,
    val thumbnailUrl: String?,
    /** Linha secundária pronta do YouTube (canal, visualizações, data). */
    val info: String?,
    val durationMs: Long,
    val isLive: Boolean,
    val isUpcoming: Boolean,
    val isShorts: Boolean,
    val percentWatched: Int,
    val playlistId: String?,
    val playlistIndex: Int,
)

/**
 * Uma playlist da conta (inclui "Assistir mais tarde").
 *
 * [browseId] e [params] são o que o motor precisa para abrir a playlist.
 */
data class Playlist(
    val browseId: String,
    val params: String?,
    val playlistId: String?,
    val title: String,
    val info: String?,
    val thumbnailUrl: String?,
)

/** Uma página de vídeos. [next] é a continuação opaca do motor (null quando acabou). */
class VideoPage(val videos: List<Video>, internal val next: MediaGroup?) {
    val hasMore: Boolean get() = next != null
}

internal fun MediaItem.toVideo(): Video? {
    val id = videoId ?: return null
    return Video(
        id = id,
        title = title.orEmpty(),
        channelName = author,
        channelId = channelId,
        thumbnailUrl = cardImageUrl,
        info = secondTitle?.toString(),
        durationMs = durationMs,
        isLive = isLive,
        isUpcoming = isUpcoming,
        isShorts = isShorts,
        percentWatched = percentWatched,
        playlistId = playlistId,
        playlistIndex = playlistIndex,
    )
}

internal fun MediaItem.toPlaylist(): Playlist? {
    val browseId = channelId ?: playlistId?.let { "VL$it" } ?: return null
    return Playlist(
        browseId = browseId,
        params = params,
        playlistId = playlistId ?: browseId.removePrefix("VL").takeIf { browseId.startsWith("VL") },
        title = title.orEmpty(),
        info = secondTitle?.toString(),
        thumbnailUrl = cardImageUrl,
    )
}

internal fun MediaGroup?.toPage(): VideoPage {
    val items = this?.mediaItems.orEmpty()
    return VideoPage(
        videos = items.mapNotNull { it.toVideo() },
        next = this?.takeIf { it.nextPageKey != null },
    )
}
