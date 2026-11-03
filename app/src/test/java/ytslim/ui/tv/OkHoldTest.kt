package ytslim.ui.tv

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** O OK curto e o OK longo do controle remoto, medidos pelo app. */
@OptIn(ExperimentalCoroutinesApi::class)
class OkHoldTest {
    private var clicks = 0
    private var longClicks = 0
    private val pressed = mutableListOf<Boolean>()

    private fun TestScope.hold() = OkHold(this, onPress = { pressed += it }, onClick = { clicks++ }, onLongClick = { longClicks++ })

    @Test
    fun aQuickPressIsAClick() = runTest {
        val ok = hold()
        ok.down()
        advanceTimeBy(150)
        ok.up()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, clicks)
        assertEquals(0, longClicks)
        assertEquals(listOf(true, false), pressed)
    }

    @Test
    fun holdingOpensTheMenuWhileOkIsStillDown() = runTest {
        val ok = hold()
        ok.down()
        advanceTimeBy(OkHold.HOLD_MS - 1)
        runCurrent()
        assertEquals(0, longClicks)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, longClicks)
        // Soltar depois do toque longo não é um clique.
        ok.up()
        assertEquals(0, clicks)
        assertEquals(listOf(true, false), pressed)
    }

    @Test
    fun releasingAnOkPressedElsewhereDoesNothing() = runTest {
        val ok = hold()
        ok.up()
        assertEquals(0, clicks)
        assertEquals(emptyList<Boolean>(), pressed)
    }

    @Test
    fun losingFocusWhileHeldCancelsBoth() = runTest {
        val ok = hold()
        ok.down()
        advanceTimeBy(100)
        ok.cancel()
        advanceTimeBy(5_000)
        runCurrent()
        ok.up()
        assertEquals(0, clicks)
        assertEquals(0, longClicks)
        assertEquals(listOf(true, false), pressed)
    }

    @Test
    fun aNewPressStartsOver() = runTest {
        val ok = hold()
        // Um OK que subiu fora daqui (sem o "up") não atrapalha o próximo.
        ok.down()
        advanceTimeBy(100)
        ok.down()
        advanceTimeBy(100)
        ok.up()
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(1, clicks)
        assertEquals(0, longClicks)
    }

    @Test
    fun shorterThanTheSystemRepeat() {
        // O sistema repete a tecla depois de 400 ms a 500 ms (ou mais, conforme a TV).
        assertTrue(OkHold.HOLD_MS < 400)
    }
}
