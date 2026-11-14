package ytslim.player

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * O brilho do modo dormir nas telas e nos controles (1.0.4, pedido do Johan): o mesmo do vídeo,
 * mas nunca abaixo de 10%; com 0%, só o vídeo apaga.
 */
@RunWith(RobolectricTestRunner::class)
class SleepBrightnessTest {
    @After
    fun reset() = SleepBrightness.set(100)

    @Test
    fun screensFollowTheVideoButNotBelowTenPercent() {
        assertEquals(listOf(100, 50, 25, 10, 10), SleepBrightness.LEVELS.map(SleepBrightness::screens))
    }

    @Test
    fun theVideoEndsUpAtTheChosenLevel() {
        for (level in SleepBrightness.LEVELS) {
            // A camada da janela por cima de tudo e a do vídeo, por baixo dos controles.
            val video = (1 - SleepBrightness.windowDim(level)) * (1 - SleepBrightness.videoDim(level))
            assertEquals("brilho $level", level / 100f, video, 0.001f)
        }
        assertEquals(1f, SleepBrightness.videoDim(0), 0.001f)
        assertEquals(0f, SleepBrightness.videoDim(25), 0.001f)
    }

    @Test
    fun theWindowGetsADarkLayer() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        SleepBrightness.applyTo(activity.window, 25)
        assertEquals(191, (activity.window.decorView.foreground as ColorDrawable).alpha)
        SleepBrightness.applyTo(activity.window, 0)
        assertEquals(230, (activity.window.decorView.foreground as ColorDrawable).alpha)
        SleepBrightness.applyTo(activity.window, 100)
        assertNull(activity.window.decorView.foreground)
    }
}
