package ytslim.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ytslim.R

/**
 * A faixa clara no pé do vídeo com o brilho baixo (achado do Johan na TV, 1.0.8): com o brilho
 * abaixo de 100%, uma view que desenha cobre o vídeo inteiro, para o Android juntar a janela (e a
 * camada preta dela) por cima do vídeo em toda a tela ([SleepBrightness.coversVideo]).
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PlayerDimTest {
    @After
    fun bright() = SleepBrightness.set(100)

    private fun open(level: Int): Pair<PlayerActivity, PlayerView> {
        SleepBrightness.set(level)
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        // Como do segundo vídeo em diante: o player abre já com o brilho escolhido.
        val activity = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(context, "abc", "A video")).setup().get()
        shadowOf(Looper.getMainLooper()).idle()
        return activity to activity.findViewById(R.id.player_view)
    }

    private fun dimView(activity: PlayerActivity): View =
        PlayerActivity::class.java.getDeclaredField("dimView").apply { isAccessible = true }.get(activity) as View

    @Test
    fun atEveryLowLevelTheWholeVideoIsCovered() {
        for (level in listOf(50, 25, 10, 0)) {
            val (activity, playerView) = open(level)
            val dim = dimView(activity)
            assertEquals("brilho $level", View.VISIBLE, dim.visibility)
            assertEquals(playerView.width, dim.width)
            assertEquals(playerView.height, dim.height)
            activity.finish()
        }
    }

    @Test
    fun atFullBrightnessNothingCoversTheVideo() {
        val (activity, _) = open(100)
        assertEquals(View.GONE, dimView(activity).visibility)
    }
}
