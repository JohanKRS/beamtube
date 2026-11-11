package ytslim.player

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * A notificação e a tela de bloqueio mostram o título, o canal e a miniatura que o app conhece, que
 * chegam depois do vídeo (o cliente de TV do YouTube manda o stream sem título nem canal).
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionPlayerTest {
    /** Um player com um vídeo só, como o do app: o MediaItem leva só o id. */
    private class OneVideo : SimpleBasePlayer(Looper.getMainLooper()) {
        var playing = true

        override fun getState(): State = State.Builder()
            .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
            .setPlaylist(listOf(MediaItemData.Builder("item").setMediaItem(MediaItem.Builder().setMediaId("abc").build()).build()))
            .setPlaybackState(Player.STATE_READY)
            .setPlayWhenReady(playing, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .build()

        override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
            playing = playWhenReady
            return Futures.immediateVoidFuture()
        }
    }

    @Test
    fun showsTheTitleChannelAndThumbnailTheAppKnows() {
        val player = SessionPlayer(OneVideo())
        assertNull(player.mediaMetadata.title)

        player.describe("abc", title = "How bridges stand", channel = null)
        assertEquals("How bridges stand", player.mediaMetadata.title)
        assertNull(player.mediaMetadata.artist)
        assertEquals("https://i.ytimg.com/vi/abc/mqdefault.jpg", player.mediaMetadata.artworkUri.toString())

        // O canal chega com a página do vídeo.
        player.describe("abc", title = "How bridges stand", channel = "Practical Engineering")
        assertEquals("Practical Engineering", player.mediaMetadata.artist)
    }

    @Test
    fun tellsTheNotificationOnlyWhenSomethingChanged() {
        val player = SessionPlayer(OneVideo())
        // A sessão lê o player assim que é criada; só o que muda depois disso vira aviso.
        assertNull(player.mediaMetadata.title)
        var changes = 0
        player.addListener(
            object : Player.Listener {
                override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                    changes++
                }
            },
        )
        player.describe("abc", title = "How bridges stand", channel = null)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, changes)

        player.describe("abc", title = "How bridges stand", channel = null)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, changes)
    }

    @Test
    fun blankNamesCountAsUnknown() {
        val metadata = NowPlaying.metadata("abc", title = " ", channel = "")
        assertNull(metadata.title)
        assertNull(metadata.artist)
    }

    @Test
    fun pausingFromTheLockScreenPausesTheVideo() {
        val video = OneVideo()
        val player = SessionPlayer(video)
        player.describe("abc", title = "How bridges stand", channel = null)
        player.pause()
        shadowOf(Looper.getMainLooper()).idle()
        assertFalse(video.playing)
        player.play()
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(video.playing)
    }
}
