package ytslim.ui

import kotlinx.serialization.Serializable
import ytslim.data.Playlist

@Serializable
data object SignInRoute

/** Início: abas Inscrições, Playlists e Configurações. */
@Serializable
data object HomeRoute

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

        /** Playlist aberta por link: só o ID; o título vem com a primeira página. */
        fun fromId(playlistId: String) = PlaylistRoute("VL$playlistId", null, playlistId, title = "")
    }
}

enum class HomeTab { SUBSCRIPTIONS, PLAYLISTS, SETTINGS }
