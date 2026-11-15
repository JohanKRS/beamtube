package ytslim.player

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
        assertEquals(191, android.graphics.Color.alpha((activity.window.decorView.foreground as SleepBrightness.Dim).color))
        SleepBrightness.applyTo(activity.window, 0)
        assertEquals(230, android.graphics.Color.alpha((activity.window.decorView.foreground as SleepBrightness.Dim).color))
        SleepBrightness.applyTo(activity.window, 100)
        assertNull(activity.window.decorView.foreground)
    }

    /**
     * A camada cobre a janela toda mesmo com limites velhos (1.0.8: o player aberto já com o brilho
     * baixo ficava com uma faixa clara no pé, do segundo vídeo em diante).
     */
    @Test
    @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    fun theDarkLayerCoversTheWholeWindowEvenWithOldBounds() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        SleepBrightness.applyTo(activity.window, 10)
        val layer = activity.window.decorView.foreground!!
        layer.setBounds(0, 0, 100, 90)
        val bitmap = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.WHITE)
        layer.draw(android.graphics.Canvas(bitmap))
        // 90% de preto por cima do branco: o pé também escurece.
        assertEquals(26f, android.graphics.Color.red(bitmap.getPixel(50, 95)).toFloat(), 1.5f)
        assertEquals(26f, android.graphics.Color.red(bitmap.getPixel(50, 10)).toFloat(), 1.5f)
    }
}
