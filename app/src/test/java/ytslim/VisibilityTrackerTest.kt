package ytslim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ytslim.data.FeedRules

/**
 * Quando o app inteiro sai da tela e volta ([AppVisibility]): é o que faz as Inscrições se
 * atualizarem sozinhas depois de um tempo fora (pedido do Johan, 2026-11-13).
 */
class VisibilityTrackerTest {
    private val tracker = VisibilityTracker()

    @Test
    fun openingTheAppIsNotAReturn() {
        assertNull(tracker.started(1_000))
    }

    @Test
    fun comingBackSaysHowLongTheAppWasAway() {
        tracker.started(0)
        tracker.stopped(1_000)
        assertEquals(3_600_000L, tracker.started(3_601_000))
    }

    @Test
    fun goingFromTheListToThePlayerAndBackIsNotLeaving() {
        tracker.started(0) // o início
        tracker.started(1_000) // o player abre por cima
        tracker.stopped(1_200) // o início sai de vista
        assertNull(tracker.started(900_000)) // voltar: o início reaparece
        tracker.stopped(900_200) // o player fecha
        // O app nunca saiu da tela: a próxima volta conta a partir de quando ele sair de verdade.
        tracker.stopped(1_000_000)
        assertEquals(500_000L, tracker.started(1_500_000))
    }

    @Test
    fun rotatingIsNotLeaving() {
        tracker.started(0)
        tracker.stoppedForChange()
        assertNull(tracker.started(10))
    }

    @Test
    fun theTvTurnedOffForTheNightRefreshesTheSubscriptions() {
        tracker.started(0)
        tracker.stopped(0)
        val away = tracker.started(8 * 3_600_000L)!!
        assertEquals(true, FeedRules.refreshesAfterAway(away))
    }

    @Test
    fun aQuickTripToAnotherAppDoesNot() {
        tracker.started(0)
        tracker.stopped(0)
        val away = tracker.started(2 * 60_000L)!!
        assertEquals(false, FeedRules.refreshesAfterAway(away))
    }
}
