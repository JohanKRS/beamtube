package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup

/** A página básica de um canal: os vídeos (com paginação) e as playlists que ele mostra. */
class ChannelPage(val videos: VideoPage, val playlists: List<Playlist>)

object ChannelRepository {
    suspend fun channel(channelId: String): ChannelPage {
        val groups = Engine.content.getChannelObserve(channelId).firstOrNull().orEmpty()
        return ChannelPage(videos = videosOf(groups), playlists = playlistsOf(groups))
    }

    /** A aba de vídeos: o primeiro grupo com vídeos que não são Shorts (de preferência, o paginado). */
    private fun videosOf(groups: List<MediaGroup>): VideoPage {
        val withVideos = groups.filter { g -> g.mediaItems.orEmpty().any { it.toVideo()?.isShorts == false } }
        return (withVideos.firstOrNull { it.nextPageKey != null } ?: withVideos.firstOrNull()).toPage()
    }

    /**
     * As playlists do canal. As seções da página costumam ser playlists ("Physics", "Math"…):
     * todos os vídeos da seção têm o mesmo playlistId. Itens que já são playlists entram também.
     */
    private fun playlistsOf(groups: List<MediaGroup>): List<Playlist> {
        val found = mutableListOf<Playlist>()
        for (group in groups) {
            val items = group.mediaItems.orEmpty()
            items.filter { it.videoId == null }.mapNotNullTo(found) { (it.toSearchItem() as? SearchItem.PlaylistItem)?.playlist }
            val ids = items.mapNotNull { it.videoId?.let { _ -> it.playlistId } }.distinct()
            val id = ids.singleOrNull() ?: continue
            if (items.any { it.videoId != null && it.playlistId == null }) continue
            found += Playlist(
                browseId = "VL$id",
                params = null,
                playlistId = id,
                title = group.title.orEmpty(),
                info = null,
                thumbnailUrl = items.firstOrNull()?.cardImageUrl,
            )
        }
        return found.filter { it.title.isNotBlank() }.distinctBy { it.browseId }
    }
}
