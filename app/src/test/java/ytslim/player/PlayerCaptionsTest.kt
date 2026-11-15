package ytslim.player

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
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
import ytslim.data.CaptionLanguage
import ytslim.ui.captionChoices

/**
 * O botão de legenda só liga e desliga (pedido do Johan depois da 1.0.9, que tinha um painel com os
 * idiomas no player); o idioma fica em Settings > Captions ([captionChoices]).
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class PlayerCaptionsTest {
    private lateinit var activity: PlayerActivity
    private lateinit var playerView: PlayerView
    private lateinit var controller: PlaybackController

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        activity = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(context, "abc", "A video")).setup().get()
        playerView = activity.findViewById(R.id.player_view)
        controller = PlayerActivity::class.java.getDeclaredField("controller").apply { isAccessible = true }.get(activity) as PlaybackController
        playerView.showController()
        settle()
    }

    private fun settle() = shadowOf(Looper.getMainLooper()).idle()

    private fun show(controls: PlaybackController.Controls) {
        @Suppress("UNCHECKED_CAST")
        val flow = PlaybackController::class.java.getDeclaredField("_controls").apply { isAccessible = true }
            .get(controller) as MutableStateFlow<PlaybackController.Controls>
        flow.value = controls
        settle()
    }

    @Test
    fun theButtonOnlyTurnsCaptionsOnAndOff() {
        show(PlaybackController.Controls(captionsAvailable = true, captionsOn = false))
        val button = playerView.findViewById<View>(R.id.player_captions)
        assertEquals(View.VISIBLE, button.visibility)
        assertFalse(button.isActivated)
        button.performClick()
        settle()
        assertTrue(controller.controls.value.captionsOn)
        assertTrue(button.isActivated)
        assertEquals(activity.getString(R.string.player_captions_off), button.contentDescription)
        // Nenhum painel abre: o próximo toque desliga.
        button.performClick()
        settle()
        assertFalse(controller.controls.value.captionsOn)
        assertFalse(button.isActivated)
    }

    @Test
    fun withoutCaptionsTheButtonIsGone() {
        show(PlaybackController.Controls(captionsAvailable = false))
        assertEquals(View.GONE, playerView.findViewById<View>(R.id.player_captions).visibility)
    }

    @Test
    fun withTheAppInEnglishEnglishShowsOnce() {
        val choices = captionChoices(activity, CaptionLanguage.ENGLISH)
        assertEquals(listOf("English", "Original"), choices.map { it.title })
        assertEquals(listOf(CaptionLanguage.APP, CaptionLanguage.ORIGINAL), choices.map { it.value })
        // "Inglês" guardado aparece na linha do idioma do app, que é o inglês.
        assertEquals(listOf(true, false), choices.map { it.selected })
    }

    @Test
    @Config(qualifiers = "pt-w960dp-h540dp-land-television-xhdpi")
    fun inPortugueseTheLanguagesHaveTheirPortugueseNames() {
        val choices = captionChoices(activity, CaptionLanguage.ORIGINAL)
        assertEquals(listOf("Português", "Original", "Inglês"), choices.map { it.title })
        assertEquals(listOf("Idioma do app", "Cada vídeo no idioma dele", null), choices.map { it.subtitle })
        assertEquals(listOf(false, true, false), choices.map { it.selected })
    }
}
