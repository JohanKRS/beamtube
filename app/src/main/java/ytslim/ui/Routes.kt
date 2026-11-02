package ytslim.ui

import kotlinx.serialization.Serializable
import ytslim.data.Playlist

@Serializable
data object SignInRoute

/** Início: as abas de [HomeTab]. */
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

/** Página de um canal (da busca ou do player). */
@Serializable
data class ChannelRoute(val channelId: String, val title: String)

/** A Busca é uma aba, mas no celular ela abre pela lupa da barra de cima. */
enum class HomeTab { SUBSCRIPTIONS, RECOMMENDED, EXPLORE, SEARCH, PLAYLISTS, HISTORY, SETTINGS }

/** Uma página pedida de fora da navegação: link de playlist de outro app, ou o canal no player. */
sealed interface Link {
    data class Playlist(val id: String) : Link
    data class Channel(val id: String, val name: String) : Link
}
