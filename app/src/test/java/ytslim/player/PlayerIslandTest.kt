package ytslim.player

import android.view.ContextThemeWrapper
import android.view.FocusFinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.R

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class PlayerIslandTest {
    private val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_App_Player)

    /** Os controles com tudo que pode aparecer na ilha, medidos numa tela de [widthDp]. */
    private fun controls(widthDp: Int, tv: Boolean = false): View {
        val view = LayoutInflater.from(context).inflate(R.layout.player_controls, null)
        listOf(R.id.player_skip, R.id.player_captions, R.id.player_quality_button, R.id.player_rotate)
            .forEach { view.findViewById<View>(it).visibility = View.VISIBLE }
        PlayerIsland(view, tv).fit(widthDp)
        val density = context.resources.displayMetrics.density
        val px = (widthDp * density).toInt()
        view.measure(View.MeasureSpec.makeMeasureSpec(px, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(px * 2, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, px, px * 2)
        return view
    }

    private fun View.fitsInItsRow(): Boolean {
        val row = parent as ViewGroup
        return right <= row.width - row.paddingEnd && width > 0
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-port")
    fun everyButtonFitsOnASmallPhoneStandingUp() {
        val view = controls(360)
        val bar = view.findViewById<View>(R.id.exo_progress)
        assertEquals(R.id.player_bar_row, (bar.parent as View).id)
        assertTrue(bar.width > 150 * context.resources.displayMetrics.density)
        listOf(R.id.player_skip, R.id.player_captions, R.id.player_quality_button, R.id.player_rotate, R.id.player_sleep)
            .forEach { assertTrue(view.resources.getResourceEntryName(it), view.findViewById<View>(it).fitsInItsRow()) }
        assertTrue(view.findViewById<TextView>(R.id.player_skip).text.isNullOrEmpty())
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun lyingDownItIsOneRowWithTheLabel() {
        val view = controls(800)
        val bar = view.findViewById<View>(R.id.exo_progress)
        assertEquals(R.id.player_button_row, (bar.parent as View).id)
        assertFalse(view.findViewById<View>(R.id.player_bar_row).isShown)
        assertEquals(context.getString(R.string.player_skip), view.findViewById<TextView>(R.id.player_skip).text.toString())
        assertTrue(view.findViewById<View>(R.id.player_sleep).fitsInItsRow())
    }

    @Test
    @Config(qualifiers = "w400dp-h860dp-port")
    fun turningBackPutsTheBarBackInPlace() {
        val view = LayoutInflater.from(context).inflate(R.layout.player_controls, null)
        val island = PlayerIsland(view, tv = false)
        island.fit(400)
        island.fit(860)
        val row = view.findViewById<ViewGroup>(R.id.player_button_row)
        // Tocar/pausar, tempo, barra, duração, na ordem de antes.
        assertEquals(
            listOf(R.id.exo_play_pause, R.id.exo_position, R.id.exo_progress, R.id.exo_duration),
            (0 until 4).map { row.getChildAt(it).id },
        )
        assertEquals(0, view.findViewById<ViewGroup>(R.id.player_bar_row).childCount)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    fun onTheTvTheRemoteReachesTheButtonsBelowTheBar() {
        val view = controls(960, tv = true) as ViewGroup
        val bar = view.findViewById<View>(R.id.exo_progress)
        // A barra numa linha só dela, e o Skip ahead com o texto.
        assertEquals(R.id.player_bar_row, (bar.parent as View).id)
        assertEquals(context.getString(R.string.player_skip), view.findViewById<TextView>(R.id.player_skip).text.toString())
        // Na barra, esquerda e direita avançam o vídeo: baixo leva aos botões, e cima volta.
        // Com o controle remoto a TV não está no modo de toque, e todo botão focável conta. Fora de
        // uma janela o Robolectric fica no modo de toque: aqui os botões contam nele também.
        ArrayList<View>().also { view.addFocusables(it, View.FOCUS_DOWN, View.FOCUSABLES_ALL) }
            .forEach { it.isFocusableInTouchMode = true }
        val below = FocusFinder.getInstance().findNextFocus(view, bar, View.FOCUS_DOWN)
        assertEquals(R.id.player_button_row, (below.parent as View).id)
        val sleep = view.findViewById<View>(R.id.player_sleep)
        assertEquals(bar, FocusFinder.getInstance().findNextFocus(view, sleep, View.FOCUS_UP))
        // E entre os botões, direita anda de um para o outro.
        val quality = view.findViewById<View>(R.id.player_quality_button)
        assertEquals(R.id.player_rotate, FocusFinder.getInstance().findNextFocus(view, quality, View.FOCUS_RIGHT).id)
    }
}
