package ytslim.player

import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ytslim.R
import java.util.concurrent.TimeUnit

/**
 * Arrastar o vídeo para baixo fecha o player no celular (pedido do Johan, 2026-11-14: ele esconde a
 * barra de navegação e o indicador, e só tinha o gesto de voltar).
 */
@RunWith(RobolectricTestRunner::class)
class SwipeToCloseTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val density = 3f

    @Test
    fun onlyAMostlyDownwardMoveStartsIt() {
        val slop = 8
        assertFalse(SwipeRules.starts(dx = 0f, dy = 10f, slop = slop)) // um toque tremido
        assertTrue(SwipeRules.starts(dx = 0f, dy = 20f, slop = slop))
        assertTrue(SwipeRules.starts(dx = 10f, dy = 40f, slop = slop))
        assertFalse(SwipeRules.starts(dx = 40f, dy = 40f, slop = slop)) // torto, para o lado
        assertFalse(SwipeRules.starts(dx = 0f, dy = -40f, slop = slop)) // para cima
    }

    @Test
    fun farEnoughOrQuickEnoughCloses() {
        // Celular deitado: 411 dp de altura, fecha a 20% (82 dp).
        val landscape = (411 * density).toInt()
        assertFalse(SwipeRules.closes(distance = 60 * density, height = landscape, velocity = 0f, density = density))
        assertTrue(SwipeRules.closes(distance = 90 * density, height = landscape, velocity = 0f, density = density))
        // Em pé (891 dp): 160 dp bastam, sem precisar de 20% da altura.
        val portrait = (891 * density).toInt()
        assertTrue(SwipeRules.closes(distance = 165 * density, height = portrait, velocity = 0f, density = density))
        // Rápido para baixo fecha mesmo curto; parado no lugar, não.
        assertTrue(SwipeRules.closes(distance = 40 * density, height = portrait, velocity = 1500 * density, density = density))
        assertFalse(SwipeRules.closes(distance = 10 * density, height = portrait, velocity = 3000 * density, density = density))
        assertFalse(SwipeRules.closes(distance = 40 * density, height = portrait, velocity = -1500 * density, density = density))
    }

    @Test
    fun theVideoShrinksALittleAsItGoesDown() {
        assertEquals(1f, SwipeRules.scale(0f, 1000), 0.001f)
        assertEquals(0.94f, SwipeRules.scale(200f, 1000), 0.001f)
        assertEquals(SwipeRules.MIN_SCALE, SwipeRules.scale(5000f, 1000), 0.001f)
    }

    private fun openPlayer(): PlayerActivity {
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, false)
        return Robolectric.buildActivity(
            PlayerActivity::class.java,
            PlayerActivity.intent(app, "aqz-KE-bpKQ", "Big Buck Bunny"),
        ).setup().get()
    }

    /** O dedo no meio do vídeo, descendo [down] px em passos, e solto (a [stepMs] por passo). */
    private fun drag(player: PlayerActivity, down: Float, steps: Int = 10, stepMs: Long = 40, release: Boolean = true) {
        val root = player.window.decorView
        val x = root.width / 2f
        val y = root.height / 3f
        val start = SystemClock.uptimeMillis()
        fun send(action: Int, at: Float, time: Long) {
            val event = MotionEvent.obtain(start, time, action, x, at, 0)
            player.dispatchTouchEvent(event)
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN, y, start)
        for (i in 1..steps) send(MotionEvent.ACTION_MOVE, y + down * i / steps, start + i * stepMs)
        if (release) send(MotionEvent.ACTION_UP, y + down, start + steps * stepMs)
    }

    private fun screenOf(player: PlayerActivity) = player.findViewById<android.view.View>(R.id.player_view).parent as android.view.View

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun draggingTheVideoDownClosesThePlayer() {
        val player = openPlayer()
        drag(player, down = player.window.decorView.height * 0.4f)
        assertTrue(player.isFinishing)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun theVideoFollowsTheFingerAndGoesBackIfLetGoEarly() {
        val player = openPlayer()
        val screen = screenOf(player)
        // Segurando no meio do caminho: o vídeo desceu e encolheu um pouco.
        drag(player, down = 120f, release = false)
        assertTrue(screen.translationY > 0f)
        assertTrue(screen.scaleX < 1f)
        // Soltar devagar, pouco abaixo de onde começou: volta ao lugar e o player continua.
        val time = SystemClock.uptimeMillis() + 2_000
        player.dispatchTouchEvent(MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, player.window.decorView.width / 2f, 300f, 0))
        shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        assertFalse(player.isFinishing)
        assertEquals(0f, screen.translationY, 0.5f)
        assertEquals(1f, screen.scaleX, 0.001f)
        player.finish()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land-xxhdpi")
    fun upwardOrSidewaysDoesNothing() {
        val player = openPlayer()
        val screen = screenOf(player)
        drag(player, down = -300f)
        assertFalse(player.isFinishing)
        assertEquals(0f, screen.translationY, 0.01f)
        player.finish()
    }

    @Test
    @Config(sdk = [30], qualifiers = "w960dp-h540dp-land-television-xhdpi")
    fun openingAndClosingUseTheShortAnimations() {
        // Android 11, como as TVs: a animação vale pedida logo depois de abrir e de fechar.
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_LEANBACK, true)
        val player = Robolectric.buildActivity(PlayerActivity::class.java, PlayerActivity.intent(app, "aqz-KE-bpKQ", "Big Buck Bunny"))
            .setup().get()
        assertEquals(R.anim.player_open_enter, shadowOf(player).pendingTransitionEnterAnimationResourceId)
        player.finish()
        assertEquals(R.anim.player_close_exit, shadowOf(player).pendingTransitionExitAnimationResourceId)
    }
}
