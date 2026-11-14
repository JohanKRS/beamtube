package ytslim.player

import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.TimeBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Na TV, esquerda e direita na barra só escolhem o ponto (com a miniatura); o vídeo vai para lá só
 * com OK, e sair da barra desiste (1.0.4, pedido do Johan). O PlayerControlView pula para o ponto no
 * onScrubStop sem "canceled", então é isso que os testes conferem.
 */
@UnstableApi
@RunWith(RobolectricTestRunner::class)
class ChapterTimeBarKeysTest {
    private lateinit var parent: FrameLayout
    private lateinit var bar: ChapterTimeBar
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        parent = FrameLayout(activity)
        bar = ChapterTimeBar(activity, null)
        parent.addView(bar, FrameLayout.LayoutParams(800, 40))
        activity.setContentView(parent)
        bar.setDuration(600_000)
        bar.setPosition(100_000)
        bar.setKeyTimeIncrement(10_000)
        bar.addListener(
            object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    events += "start $position"
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    events += "move $position"
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    events += if (canceled) "cancel" else "seek $position"
                }
            },
        )
        bar.isFocusableInTouchMode = true
        assertTrue(bar.requestFocus())
    }

    private fun press(keyCode: Int): Boolean = bar.onKeyDown(keyCode, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))

    /** O tempo que o DefaultTimeBar mostra (ele põe a posição na descrição). */
    private val shownTime: String get() = bar.contentDescription.toString()

    @Test
    fun theArrowsOnlyMoveThePoint() {
        assertTrue(press(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(press(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(press(KeyEvent.KEYCODE_DPAD_LEFT))
        assertTrue(press(KeyEvent.KEYCODE_DPAD_RIGHT))
        // O DefaultTimeBar pulava sozinho 1 s depois da última tecla.
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(5))
        assertEquals(listOf("start 100000", "move 110000", "move 120000", "move 110000", "move 120000"), events)
        assertTrue(bar.keyScrubbing)
        // O vídeo continua andando, mas o círculo fica no ponto escolhido.
        bar.setPosition(103_000)
        assertEquals("02:00", shownTime)
    }

    @Test
    fun okGoesToThePoint() {
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue(press(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals("seek 120000", events.last())
        assertFalse(bar.keyScrubbing)
    }

    @Test
    fun leavingTheBarKeepsTheVideoWhereItWas() {
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        bar.setPosition(102_000)
        // Descer para os botões (o foco sai da barra).
        bar.clearFocus()
        assertEquals("cancel", events.last())
        assertEquals("01:42", shownTime)
        assertFalse(events.any { it.startsWith("seek") })
    }

    @Test
    fun hidingTheControlsGivesUpToo() {
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        parent.visibility = View.GONE
        assertEquals("cancel", events.last())
        assertEquals("01:40", shownTime)
    }

    @Test
    fun backGivesUpAndStaysOnTheBar() {
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue(press(KeyEvent.KEYCODE_BACK))
        assertEquals("cancel", events.last())
        assertTrue(bar.hasFocus())
        // Sem um ponto escolhido, voltar segue como antes.
        assertFalse(press(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun atTheEndTheArrowStaysOnTheBarWhileChoosing() {
        bar.setPosition(595_000)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals("move 600000", events.last())
        // Já no fim: a tecla não sai da barra (não desiste sem querer).
        assertTrue(press(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(bar.keyScrubbing)
    }
}
