package ytslim.player

import android.app.Application
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import ytslim.R

/**
 * A tela do player de verdade, no celular: desligar a tela no meio do vídeo não pausa (pedido do
 * Johan, 2026-11-11), e o vídeo segue só com o som; sair para outro app pausa, como antes.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenOffPlaybackTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val power = shadowOf(app.getSystemService(PowerManager::class.java))
    private lateinit var screen: ActivityController<PlayerActivity>
    private lateinit var player: Player

    @Before
    fun openVideo() {
        power.turnScreenOn(true)
        screen = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(app, "aqz-KE-bpKQ", "Big Buck Bunny")).setup()
        player = screen.get().findViewById<PlayerView>(R.id.player_view).player!!
    }

    @After
    fun close() = screen.close()

    private val pictureOff get() = C.TRACK_TYPE_VIDEO in player.trackSelectionParameters.disabledTrackTypes

    @Test
    fun theServiceStartsWithThePlayerOnScreen() {
        // Com o app na frente: com a tela já apagada, o Android não deixaria.
        assertEquals(PlaybackService::class.java.name, shadowOf(app).nextStartedService.component?.className)
    }

    @Test
    fun turningTheScreenOffKeepsPlayingWithTheSoundOnly() {
        assertTrue(player.playWhenReady)
        power.turnScreenOn(false)
        screen.pause().stop()
        assertTrue(player.playWhenReady)
        assertTrue(pictureOff)

        // Tela de volta: a imagem volta.
        power.turnScreenOn(true)
        screen.start().resume()
        assertTrue(player.playWhenReady)
        assertFalse(pictureOff)
    }

    @Test
    fun leavingForAnotherAppStillPauses() {
        screen.pause().stop()
        assertFalse(player.playWhenReady)
    }

    @Test
    fun closingThePlayerStopsTheService() {
        screen.pause().stop().destroy()
        assertEquals(PlaybackService::class.java.name, shadowOf(app).nextStoppedService.component?.className)
    }
}
