package ytslim.player

import android.app.Application
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.After
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
 * A tela do player de verdade no mini player flutuante (pedido do Johan, 2026-11-11). O Android
 * abre e fecha a janelinha; aqui ela é simulada pelo aviso que o Android manda à tela.
 */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class MiniPlayerTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val opened = mutableListOf<ActivityController<PlayerActivity>>()

    @Before
    fun setUp() {
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE, true)
        shadowOf(app.getSystemService(PowerManager::class.java)).turnScreenOn(true)
    }

    @After
    fun close() = opened.asReversed().forEach { it.close() }

    private fun open(videoId: String): ActivityController<PlayerActivity> =
        Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(app, videoId, "Big Buck Bunny"))
            .setup()
            .also { opened += it }

    private val ActivityController<PlayerActivity>.playerView: PlayerView get() = get().findViewById(R.id.player_view)

    /** O aviso do Android à tela: entrou na janelinha ou saiu dela. */
    private fun ActivityController<PlayerActivity>.floating(on: Boolean) = apply {
        get().onPictureInPictureModeChanged(on, get().resources.configuration)
    }

    /** Sair do app: a tela pausa, mas continua à vista, na janelinha. */
    private fun ActivityController<PlayerActivity>.float() = pause().floating(true)

    /** Tocar na janelinha: o player volta a ocupar a tela. */
    private fun ActivityController<PlayerActivity>.expand() = floating(false).resume()

    @Test
    fun theMiniPlayerShowsOnlyTheVideoAndKeepsPlaying() {
        val screen = open("aqz-KE-bpKQ")
        screen.float()
        assertFalse(screen.playerView.useController)
        assertTrue(screen.playerView.player!!.playWhenReady)

        // Tocar na janelinha: o player volta, com os controles, e o vídeo segue.
        screen.expand()
        assertTrue(screen.playerView.useController)
        assertTrue(screen.playerView.player!!.playWhenReady)
        assertFalse(screen.get().isFinishing)
    }

    @Test
    fun closingTheMiniPlayerClosesTheVideo() {
        val screen = open("aqz-KE-bpKQ")
        val player = screen.playerView.player!!
        screen.float()
        // O Android tira a tela de vista e depois avisa que a janelinha saiu.
        screen.stop().floating(false)
        assertFalse(player.playWhenReady)
        assertTrue(screen.get().isFinishing)
    }

    @Test
    fun theScreenOffKeepsTheMiniPlayer() {
        val screen = open("aqz-KE-bpKQ")
        screen.float()
        shadowOf(app.getSystemService(PowerManager::class.java)).turnScreenOn(false)
        screen.stop()
        assertTrue(screen.playerView.player!!.playWhenReady)
        assertFalse(screen.get().isFinishing)
    }

    @Test
    fun aNewVideoClosesTheOldMiniPlayer() {
        val first = open("aqz-KE-bpKQ")
        first.float()
        open("YE7VzlLtp-4")
        assertTrue(first.get().isFinishing)
    }

    @Test
    fun aNewVideoLeavesAPlayerOutsideTheMiniPlayerAlone() {
        val first = open("aqz-KE-bpKQ")
        first.float().expand()
        first.pause().stop()
        open("YE7VzlLtp-4")
        assertFalse(first.get().isFinishing)
    }
}
