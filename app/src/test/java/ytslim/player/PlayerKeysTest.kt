package ytslim.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ytslim.R

/**
 * Na TV, descer da última linha dos controles ou subir da primeira esconde os controles na hora
 * (pedido do Johan, 2026-11-04), em vez de esperar o tempo.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PlayerKeysTest {
    private lateinit var activity: PlayerActivity
    private lateinit var playerView: PlayerView

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // O qualificador "television" não chega ao UiModeManager do Robolectric.
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        activity = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(context, "abc", "A video")).setup().get()
        playerView = activity.findViewById(R.id.player_view)
        // A linha de cima com o nome do canal, como quando a página do vídeo chega.
        playerView.findViewById<View>(R.id.player_channel).visibility = View.VISIBLE
        playerView.showController()
        // O Robolectric fica no modo de toque, onde só o foco "de toque" pega: como o controle na TV.
        focusablesIn(playerView).forEach { it.isFocusableInTouchMode = true; it.isEnabled = true }
        settle()
    }

    private fun focusablesIn(view: View): List<View> = when (view) {
        is ViewGroup -> (0 until view.childCount).flatMap { focusablesIn(view.getChildAt(it)) } + listOfNotNull(view.takeIf { it.isFocusable })
        else -> listOfNotNull(view.takeIf { it.isFocusable })
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private fun focus(id: Int) {
        assertTrue(playerView.findViewById<View>(id).requestFocus())
        settle()
    }

    /** Aperta e solta [keyCode]; [repeats] repetições de segurar a tecla no meio. */
    private fun press(keyCode: Int, repeats: Int = 0) {
        val down = SystemClock.uptimeMillis()
        for (repeat in 0..repeats) activity.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_DOWN, keyCode, repeat))
        settle()
        activity.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_UP, keyCode, 0))
        settle()
    }

    @Test
    fun downFromTheLastRowHidesTheControls() {
        focus(R.id.exo_play_pause)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertFalse(playerView.isControllerFullyVisible)
    }

    @Test
    fun upFromTheFirstRowHidesTheControls() {
        focus(R.id.player_channel)
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertFalse(playerView.isControllerFullyVisible)
    }

    @Test
    fun betweenTheRowsTheArrowsStillMoveAround() {
        focus(R.id.exo_progress)
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertTrue(playerView.isControllerFullyVisible)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue(playerView.isControllerFullyVisible)
    }

    @Test
    fun holdingTheArrowDoesNotBringThemBack() {
        focus(R.id.exo_play_pause)
        press(KeyEvent.KEYCODE_DPAD_DOWN, repeats = 5)
        assertFalse(playerView.isControllerFullyVisible)
        // A próxima seta acende de novo, como antes.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue(playerView.isControllerFullyVisible)
    }

    @Test
    fun holdingTheArrowThatOpenedThemKeepsThemOpen() {
        playerView.hideController()
        settle()
        val down = SystemClock.uptimeMillis()
        activity.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0))
        settle()
        assertTrue(playerView.isControllerFullyVisible)
        // Abertos, o foco vai para o tocar/pausar, na linha de baixo (aqui, sem vídeo, à mão).
        focusablesIn(playerView).forEach { it.isEnabled = true }
        focus(R.id.exo_play_pause)
        for (repeat in 1..5) activity.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, repeat))
        activity.dispatchKeyEvent(KeyEvent(down, down, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0))
        settle()
        assertTrue(playerView.isControllerFullyVisible)
    }

    @Test
    fun withAPanelOpenTheArrowsStayWithIt() {
        playerView.findViewById<View>(R.id.player_sleep).performClick()
        settle()
        focusablesIn(playerView).forEach { it.isFocusableInTouchMode = true; it.isEnabled = true }
        focus(R.id.exo_play_pause)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue(playerView.isControllerFullyVisible)
    }

    /** 1.0.8, pedido do Johan: OK na lua só abre e fecha a caixa; o foco fica nela. */
    @Test
    fun okOnTheMoonOnlyOpensAndClosesTheBox() {
        val sleep = playerView.findViewById<View>(R.id.player_sleep)
        val panel = playerView.findViewById<View>(R.id.player_sleep_panel)
        focus(R.id.player_sleep)
        sleep.performClick()
        settle()
        assertEquals(View.VISIBLE, panel.visibility)
        assertTrue(sleep.isFocused)
        sleep.performClick()
        settle()
        assertEquals(View.GONE, panel.visibility)
        assertTrue(sleep.isFocused)
    }

    /** Cima na lua entra na linha de baixo da caixa (na opção marcada); baixo dali volta para a lua. */
    @Test
    fun upFromTheMoonEntersTheBoxAndDownComesBack() {
        val sleep = playerView.findViewById<View>(R.id.player_sleep)
        focus(R.id.player_sleep)
        sleep.performClick()
        settle()
        focusablesIn(playerView).forEach { it.isFocusableInTouchMode = true; it.isEnabled = true }
        val bottom = listOf(R.id.player_sleep_end, R.id.player_sleep_queue, R.id.player_sleep_brightness)
            .map { playerView.findViewById<ViewGroup>(it) }
            .first { (it.parent as View).visibility == View.VISIBLE }
        press(KeyEvent.KEYCODE_DPAD_UP)
        val focused = playerView.findFocus()
        assertTrue(focused.parent == bottom)
        assertTrue(focused.isActivated)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertTrue(sleep.isFocused)
        assertEquals(View.VISIBLE, playerView.findViewById<View>(R.id.player_sleep_panel).visibility)
    }

    /** 1.0.8, pedido do Johan: a linha dos botões dá a volta nas pontas. */
    @Test
    fun leftOnPlayGoesToTheMoonAndRightOnTheMoonBackToPlay() {
        focus(R.id.exo_play_pause)
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        assertTrue(playerView.findViewById<View>(R.id.player_sleep).isFocused)
        assertTrue(playerView.isControllerFullyVisible)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue(playerView.findViewById<View>(R.id.exo_play_pause).isFocused)
    }

    @Test
    fun inTheMiddleOfTheRowTheArrowsAreNotWrapped() {
        // Com mais um botão na linha, esquerda na lua acha outro botão dela: nada de dar a volta.
        playerView.findViewById<View>(R.id.player_quality_button).visibility = View.VISIBLE
        settle()
        val island = PlayerIsland(playerView, tv = true)
        val sleep = playerView.findViewById<View>(R.id.player_sleep)
        assertTrue(sleep.requestFocus())
        assertFalse(island.wrapFocus(sleep, View.FOCUS_LEFT))
        assertTrue(sleep.isFocused)
    }

    /** 1.0.8, pedido do Johan: "voltar" com os controles na tela só esconde os controles. */
    @Test
    fun backHidesTheControlsFirstAndThenLeaves() {
        focus(R.id.exo_play_pause)
        activity.onBackPressedDispatcher.onBackPressed()
        settle()
        assertFalse(playerView.isControllerFullyVisible)
        assertFalse(activity.isFinishing)
        activity.onBackPressedDispatcher.onBackPressed()
        settle()
        assertTrue(activity.isFinishing)
    }
}
