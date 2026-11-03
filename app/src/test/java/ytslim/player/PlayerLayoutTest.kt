package ytslim.player

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import ytslim.R

/** O PlayerView do Media3 aceita os nossos controles (player_controls) sem quebrar. */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class PlayerLayoutTest {
    @Test
    fun customControlsInflateAndShow() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_App_Player)
        val view = LayoutInflater.from(context).inflate(R.layout.player_view, null) as PlayerView
        val player = ExoPlayer.Builder(context).build()
        try {
            view.player = player
            view.showController()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(view.isControllerFullyVisible)
            assertNotNull(view.findViewById<View>(R.id.exo_play_pause))
            assertNotNull(view.findViewById<View>(R.id.exo_progress))
            // Botões que dependem do vídeo começam escondidos.
            assertEquals(View.GONE, view.findViewById<View>(R.id.player_captions).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.player_actions).visibility)
            // A lua fica sempre; o painel de dormir só abre pelo botão.
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.player_sleep).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.player_sleep_panel).visibility)
            // A resolução aparece quando as faixas do vídeo chegam.
            assertEquals(View.GONE, view.findViewById<View>(R.id.player_quality_button).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.player_quality_panel).visibility)
            assertNotNull(view.overlayFrameLayout)
        } finally {
            player.release()
        }
    }
}
