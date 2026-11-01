package ytslim.data

import com.liskovsoft.googlecommon.common.helpers.YouTubeHelper
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
    /** Tempo relativo da publicação, como o YouTube escreve ("3 hours ago"). */
    val published: String?,
    val durationMs: Long,
    val isLive: Boolean,
    val isUpcoming: Boolean,
    val isShorts: Boolean,
    val percentWatched: Int,
    val playlistId: String?,
    val playlistIndex: Int,
    /** No feed de Inscrições: o título da playlist inscrita de onde o vídeo veio. */
    val fromPlaylist: String? = null,
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

/** Um canal (resultado de busca, ou o canal de um vídeo). */
data class Channel(val id: String, val title: String, val info: String?, val thumbnailUrl: String?)

/** Um resultado de busca: vídeo, canal ou playlist. */
sealed interface SearchItem {
    val key: String

    data class VideoItem(val video: Video) : SearchItem {
        override val key get() = "v:" + video.id
    }

    data class ChannelItem(val channel: Channel) : SearchItem {
        override val key get() = "c:" + channel.id
    }

    data class PlaylistItem(val playlist: Playlist) : SearchItem {
        override val key get() = "p:" + playlist.browseId
    }
}

/** Uma página de vídeos. [next] é a continuação opaca do motor (null quando acabou). */
class VideoPage(val videos: List<Video>, internal val next: MediaGroup?, val title: String? = null) {
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
        published = productionDate,
        durationMs = durationMs,
        isLive = isLive,
        isUpcoming = isUpcoming,
        // A marca do YouTube às vezes falta; a regra do motor olha duração, "#" no título e a capa.
        isShorts = isShorts || YouTubeHelper.isShorts(this, false),
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
        title = this?.title,
    )
}

/**
 * Converte um resultado de busca: playlist ("VL…", ou um item com playlistId: a busca manda a
 * playlist junto com o primeiro vídeo dela), vídeo ou canal ("UC…").
 */
internal fun MediaItem.toSearchItem(): SearchItem? {
    val channel = channelId
    if (channel?.startsWith("VL") == true || playlistId != null) {
        return toPlaylist()?.let { playlist ->
            // "3Blue1Brown • 16 lessons"
            val info = listOfNotNull(playlist.info, badgeText).filter { it.isNotBlank() }.joinToString(" • ")
            SearchItem.PlaylistItem(playlist.copy(info = info.ifEmpty { null }))
        }
    }
    videoId?.let { return toVideo()?.let(SearchItem::VideoItem) }
    if (channel == null) return null
    // A foto do canal às vezes vem sem o "https:" ("//yt3.ggpht.com/…").
    val photo = cardImageUrl?.let { if (it.startsWith("//")) "https:$it" else it }
    return SearchItem.ChannelItem(Channel(channel, title.orEmpty(), secondTitle?.toString(), photo))
}
