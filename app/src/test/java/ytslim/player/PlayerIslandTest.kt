package ytslim.player

import android.view.ContextThemeWrapper
import android.view.FocusFinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        listOf(R.id.player_captions, R.id.player_quality_button, R.id.player_rotate, R.id.player_actions, R.id.player_save)
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
        listOf(R.id.player_captions, R.id.player_quality_button, R.id.player_rotate, R.id.player_sleep)
            .forEach { assertTrue(view.resources.getResourceEntryName(it), view.findViewById<View>(it).fitsInItsRow()) }
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun aSmallPhoneLyingDownIsOneRow() {
        val view = controls(640)
        val bar = view.findViewById<View>(R.id.exo_progress)
        assertEquals(R.id.player_button_row, (bar.parent as View).id)
        assertFalse(view.findViewById<View>(R.id.player_bar_row).isShown)
        assertTrue(bar.width > 200 * context.resources.displayMetrics.density)
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
        // A barra numa linha só dela.
        assertEquals(R.id.player_bar_row, (bar.parent as View).id)
        // Na barra, esquerda e direita avançam o vídeo: baixo leva aos botões, e cima volta.
        // Com o controle remoto a TV não está no modo de toque, e todo botão focável conta. Fora de
        // uma janela o Robolectric fica no modo de toque: aqui os botões contam nele também.
        ArrayList<View>().also { view.addFocusables(it, View.FOCUS_DOWN, View.FOCUSABLES_ALL) }
            .forEach { it.isFocusableInTouchMode = true }
        val below = FocusFinder.getInstance().findNextFocus(view, bar, View.FOCUS_DOWN)
        // Na linha de baixo: um botão dela, ou uma das ações que desceram para ela.
        assertTrue((below.parent as View).id in listOf(R.id.player_button_row, R.id.player_actions))
        val sleep = view.findViewById<View>(R.id.player_sleep)
        assertEquals(bar, FocusFinder.getInstance().findNextFocus(view, sleep, View.FOCUS_UP))
        // E entre os botões, direita anda de um para o outro.
        val quality = view.findViewById<View>(R.id.player_quality_button)
        assertEquals(R.id.player_rotate, FocusFinder.getInstance().findNextFocus(view, quality, View.FOCUS_RIGHT).id)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    fun onTheTvTheActionsSitInTheBottomBar() {
        val view = controls(960, tv = true) as ViewGroup
        val row = view.findViewById<ViewGroup>(R.id.player_button_row)
        val actions = view.findViewById<View>(R.id.player_actions)
        // Logo depois do tocar/pausar (e do LIVE, que só aparece em lives), sem a pílula, e fora do canto de cima.
        assertEquals(row, actions.parent)
        assertEquals(2, row.indexOfChild(actions))
        assertEquals(1, row.indexOfChild(view.findViewById(R.id.player_live)))
        assertEquals(null, actions.background)
        assertFalse(view.findViewById<ViewGroup>(R.id.player_top).indexOfChild(actions) >= 0)
        // As ações ficam à esquerda; legenda, resolução e lua continuam à direita.
        assertTrue(actions.left + view.findViewById<View>(R.id.player_subscribe).right < view.findViewById<View>(R.id.player_captions).left)
        ArrayList<View>().also { view.addFocusables(it, View.FOCUS_DOWN, View.FOCUSABLES_ALL) }
            .forEach { it.isFocusableInTouchMode = true }
        val play = view.findViewById<View>(R.id.exo_play_pause)
        assertEquals(R.id.player_like, FocusFinder.getInstance().findNextFocus(view, play, View.FOCUS_RIGHT).id)
        val like = view.findViewById<View>(R.id.player_like)
        assertEquals(view.findViewById<View>(R.id.exo_progress), FocusFinder.getInstance().findNextFocus(view, like, View.FOCUS_UP))
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television")
    fun onTheTvTheRemoteReachesTheLiveButton() {
        val view = controls(960, tv = true) as ViewGroup
        val live = view.findViewById<View>(R.id.player_live).apply { visibility = View.VISIBLE }
        view.measure(View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, view.width, view.height)
        // Na linha dos botões, logo depois do tocar: na linha da barra, direita avançaria o vídeo.
        assertEquals(R.id.player_button_row, (live.parent as View).id)
        ArrayList<View>().also { view.addFocusables(it, View.FOCUS_DOWN, View.FOCUSABLES_ALL) }
            .forEach { it.isFocusableInTouchMode = true }
        val play = view.findViewById<View>(R.id.exo_play_pause)
        assertEquals(live, FocusFinder.getInstance().findNextFocus(view, play, View.FOCUS_RIGHT))
        assertEquals(R.id.player_like, FocusFinder.getInstance().findNextFocus(view, live, View.FOCUS_RIGHT).id)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun lyingDownTheLiveButtonTakesThePlaceOfTheTime() {
        val view = controls(891)
        val row = view.findViewById<ViewGroup>(R.id.player_button_row)
        val live = view.findViewById<View>(R.id.player_live)
        assertEquals(row, live.parent)
        assertEquals(row.indexOfChild(view.findViewById(androidx.media3.ui.R.id.exo_duration)) + 1, row.indexOfChild(live))
    }
}
