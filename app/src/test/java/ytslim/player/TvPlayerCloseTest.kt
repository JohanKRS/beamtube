package ytslim.player

import android.app.Application
import android.content.pm.PackageManager
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * A tela do player de verdade, na TV: desligar a TV ou sair do app fecha o player, e o app volta na
 * lista de onde o vídeo foi aberto, não numa tela preta carregando o vídeo de antes (pedido do
 * Johan, 2026-11-13). No celular, o player fica (mini player e tela desligada tocando).
 */
@RunWith(RobolectricTestRunner::class)
class TvPlayerCloseTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val power = shadowOf(app.getSystemService(PowerManager::class.java))
    private lateinit var screen: ActivityController<PlayerActivity>

    private fun open(tv: Boolean) {
        // O qualificador "television" não chega ao UiModeManager do Robolectric.
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, tv)
        power.turnScreenOn(true)
        screen = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(app, "aqz-KE-bpKQ", "Big Buck Bunny")).setup()
    }

    @After
    fun close() = screen.close()

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun turningTheTvOffClosesThePlayer() {
        open(tv = true)
        power.turnScreenOn(false)
        screen.pause().stop()
        assertTrue(screen.get().isFinishing)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun leavingTheAppOnTheTvClosesThePlayer() {
        open(tv = true)
        screen.pause().stop()
        assertTrue(screen.get().isFinishing)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun onThePhoneThePlayerStays() {
        open(tv = false)
        power.turnScreenOn(false)
        screen.pause().stop()
        assertFalse(screen.get().isFinishing)
    }
}
