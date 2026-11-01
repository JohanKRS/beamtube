package ytslim.ui

import kotlinx.serialization.Serializable
import ytslim.data.Playlist

@Serializable
data object SignInRoute

@Serializable
data object PlaylistsRoute

@Serializable
data class PlaylistRoute(
    val browseId: String,
    val params: String?,
    val playlistId: String?,
    val title: String,
) {
    fun toPlaylist() = Playlist(browseId, params, playlistId, title, info = null, thumbnailUrl = null)

    companion object {
        fun from(p: Playlist) = PlaylistRoute(p.browseId, p.params, p.playlistId, p.title)
    }
}
