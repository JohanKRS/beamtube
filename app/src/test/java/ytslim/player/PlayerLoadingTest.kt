package ytslim.player

import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * O que aparece enquanto o vídeo não começa (pedido do Johan, 2026-11-13: "às vezes a tela fica só
 * preta, sem nenhuma indicação"): a miniatura e o círculo, desde que o player abre.
 */
@RunWith(RobolectricTestRunner::class)
class PlayerLoadingTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    private fun later(ms: Long) = shadowOf(Looper.getMainLooper()).idleFor(ms, TimeUnit.MILLISECONDS)

    /** Numa tela de verdade: fora de uma, as animações e as esperas da View não rodam. */
    private fun attached(): PlayerLoading {
        val screen = Robolectric.buildActivity(Activity::class.java).setup().get()
        return PlayerLoading(screen, tv = false).also { screen.setContentView(it) }
    }

    @Test
    fun theRingTurnsWhileTheVideoIsAskedForAndWhileItLoads() {
        // O pedido ao YouTube, com o player ainda parado: era aqui que a tela ficava preta.
        assertTrue(LoadingRules.busy(requesting = true, failed = false, playbackState = Player.STATE_IDLE))
        assertTrue(LoadingRules.busy(requesting = false, failed = false, playbackState = Player.STATE_BUFFERING))
        assertFalse(LoadingRules.busy(requesting = false, failed = false, playbackState = Player.STATE_READY))
        assertFalse(LoadingRules.busy(requesting = false, failed = false, playbackState = Player.STATE_ENDED))
        // Com o erro na tela, nada gira.
        assertFalse(LoadingRules.busy(requesting = true, failed = true, playbackState = Player.STATE_BUFFERING))
    }

    @Test
    fun aNewVideoShowsThePosterAndTheRingAtOnce() {
        val loading = attached()
        assertFalse(loading.posterShown)
        assertFalse(loading.ringShown)
        loading.start(null)
        assertTrue(loading.posterShown)
        assertTrue(loading.ringShown)
    }

    @Test
    fun theFirstFrameTakesThePosterAway() {
        val loading = attached()
        loading.start(null)
        loading.firstFrame()
        loading.setBusy(false)
        later(1_000)
        assertFalse(loading.posterShown)
        assertFalse(loading.ringShown)
    }

    @Test
    fun midVideoTheRingOnlyShowsIfTheLoadingLasts() {
        val loading = attached()
        loading.start(null)
        loading.firstFrame()
        loading.setBusy(false)
        later(1_000)

        // Um pulo para um trecho já carregado: o "carregando" passa antes de o círculo aparecer.
        loading.setBusy(true)
        later(LoadingRules.MID_VIDEO_DELAY_MS / 2)
        loading.setBusy(false)
        later(LoadingRules.MID_VIDEO_DELAY_MS)
        assertFalse(loading.ringShown)

        // A rede atrasou de verdade: o círculo aparece, e some quando o vídeo volta.
        loading.setBusy(true)
        later(LoadingRules.MID_VIDEO_DELAY_MS + 1)
        assertTrue(loading.ringShown)
        assertFalse(loading.posterShown)
        loading.setBusy(false)
        assertFalse(loading.ringShown)
    }

    @Test
    fun aVideoThatFailsShowsNeither() {
        val loading = attached()
        loading.start(null)
        loading.stop()
        assertFalse(loading.posterShown)
        assertFalse(loading.ringShown)
    }

    @Test
    fun theNextVideoOfTheSequenceStartsWithItsPosterAgain() {
        val loading = attached()
        loading.start(null)
        loading.firstFrame()
        later(1_000)
        loading.start(null)
        assertTrue(loading.posterShown)
        assertTrue(loading.ringShown)
    }

    private fun openPlayer(tv: Boolean): PlayerActivity {
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, tv)
        return Robolectric.buildActivity(
            PlayerActivity::class.java,
            PlayerActivity.intent(app, "aqz-KE-bpKQ", "Big Buck Bunny", thumbnail = "https://i.ytimg.com/vi/aqz-KE-bpKQ/hq720.jpg"),
        ).setup().get()
    }

    private fun View.find(match: (View) -> Boolean): View? {
        if (match(this)) return this
        if (this is ViewGroup) for (i in 0 until childCount) getChildAt(i).find(match)?.let { return it }
        return null
    }

    private fun loadingOf(player: PlayerActivity): PlayerLoading =
        player.window.decorView.find { it is PlayerLoading } as PlayerLoading

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun thePlayerOpensAlreadyShowingThatItIsLoading() {
        val player = openPlayer(tv = false)
        val loading = loadingOf(player)
        assertTrue(loading.posterShown)
        assertTrue(loading.ringShown)
        // O círculo do Media3 (que só aparecia depois do pedido ao YouTube) não é mais usado.
        val media3Ring = player.window.decorView.findViewById<View>(androidx.media3.ui.R.id.exo_buffering)
        assertNotNull(media3Ring)
        assertEquals(View.GONE, media3Ring.visibility)
        player.finish()
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun onTheTvToo() {
        val player = openPlayer(tv = true)
        val loading = loadingOf(player)
        assertTrue(loading.posterShown)
        assertTrue(loading.ringShown)
        // Por baixo dos controles e do escurecer do modo dormir: não pega o foco do D-pad.
        assertFalse(loading.isFocusable)
        player.finish()
    }
}
