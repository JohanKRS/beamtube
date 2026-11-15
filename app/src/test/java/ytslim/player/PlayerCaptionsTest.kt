package ytslim.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ytslim.R
import ytslim.data.Caption
import ytslim.data.SubtitleRules

/** O painel da legenda (1.0.9, pedido do Johan): Off, o idioma do app, o original e inglês, sem repetir. */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PlayerCaptionsTest {
    private lateinit var activity: PlayerActivity
    private lateinit var playerView: PlayerView

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        activity = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(context, "abc", "A video")).setup().get()
        playerView = activity.findViewById(R.id.player_view)
        playerView.showController()
        settle()
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    /** O que o controller mostra num vídeo em japonês, com o app em [app]. */
    private fun show(app: String) {
        val captions = listOf(Caption("https://x/t?lang=ja&kind=asr", "text/vtt", "ja", "Japanese", auto = true, translatable = true))
        val options = SubtitleRules.options(captions, app, "ja")
        val controller = PlayerActivity::class.java.getDeclaredField("controller").apply { isAccessible = true }.get(activity)
        @Suppress("UNCHECKED_CAST")
        val flow = PlaybackController::class.java.getDeclaredField("_controls").apply { isAccessible = true }
            .get(controller) as MutableStateFlow<PlaybackController.Controls>
        flow.value = PlaybackController.Controls(captionOptions = options, captionShown = options.last())
        settle()
    }

    private fun labels(): List<String> {
        val row = playerView.findViewById<LinearLayout>(R.id.player_captions_options)
        return (0 until row.childCount).map { (row.getChildAt(it) as TextView).text.toString() }
    }

    @Test
    fun withTheAppInEnglishEnglishShowsOnce() {
        show("en")
        assertEquals(listOf("Off", "English", "Original"), labels())
        assertEquals(View.VISIBLE, playerView.findViewById<View>(R.id.player_captions).visibility)
    }

    @Test
    @Config(qualifiers = "pt-w960dp-h540dp-land-television-xhdpi")
    fun inPortugueseTheLanguagesHaveTheirPortugueseNames() {
        show("pt")
        assertEquals(listOf("Desligada", "Português", "Original", "Inglês"), labels())
    }

    @Test
    fun theButtonOpensThePanelOnTheCaptionShown() {
        show("en")
        val button = playerView.findViewById<View>(R.id.player_captions)
        button.isFocusableInTouchMode = true
        val row = playerView.findViewById<LinearLayout>(R.id.player_captions_options)
        (0 until row.childCount).forEach { row.getChildAt(it).isFocusableInTouchMode = true }
        button.performClick()
        settle()
        assertEquals(View.VISIBLE, playerView.findViewById<View>(R.id.player_captions_panel).visibility)
        assertTrue(row.getChildAt(2).isFocused)
        assertTrue(row.getChildAt(2).isActivated)
        // "Off" desliga e fecha o painel, com o foco de volta no botão.
        row.getChildAt(0).performClick()
        settle()
        assertEquals(View.GONE, playerView.findViewById<View>(R.id.player_captions_panel).visibility)
        assertTrue(button.isFocused)
    }
}
