package ytslim.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * O player como a notificação e a tela de bloqueio o veem ([PlaybackService]): o mesmo do vídeo,
 * com o título, o canal e a miniatura que o app conhece. O cliente de TV do YouTube manda o vídeo
 * sem título nem canal, e eles chegam aos poucos (do card, da sequência, da página do vídeo); aqui
 * eles entram assim que chegam, sem mexer no vídeo que está tocando.
 */
@OptIn(UnstableApi::class)
internal class SessionPlayer(player: Player) : ForwardingSimpleBasePlayer(player) {
    private var known: MediaMetadata = MediaMetadata.EMPTY

    /** O que se sabe agora do vídeo aberto ([videoId]); null = ainda não se sabe. */
    fun describe(videoId: String, title: String?, channel: String?) {
        val next = NowPlaying.metadata(videoId, title, channel)
        if (next == known) return
        known = next
        invalidateState()
    }

    override fun getState(): State {
        val state = super.getState()
        val playlist = state.playlist
        val index = state.currentMediaItemIndex
        if (index !in playlist.indices) return state
        val item = playlist[index]
        val base = item.mediaMetadata ?: item.mediaItem.mediaMetadata
        val items = playlist.toMutableList()
        items[index] = item.buildUpon().setMediaMetadata(base.buildUpon().populate(known).build()).build()
        return state.buildUpon().setPlaylist(items).build()
    }
}

/** O título, o canal e a miniatura do vídeo na notificação e na tela de bloqueio. */
internal object NowPlaying {
    /** A miniatura de 320x180 que todo vídeo tem, sem as faixas pretas da hqdefault (4:3). */
    fun artworkUrl(videoId: String) = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

    fun metadata(videoId: String, title: String?, channel: String?): MediaMetadata =
        MediaMetadata.Builder()
            .setTitle(title?.takeIf { it.isNotBlank() })
            .setArtist(channel?.takeIf { it.isNotBlank() })
            .setArtworkUri(Uri.parse(artworkUrl(videoId)))
            .build()
}
