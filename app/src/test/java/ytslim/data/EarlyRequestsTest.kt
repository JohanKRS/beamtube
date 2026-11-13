package ytslim.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Os vídeos pedidos antes de a tela do player precisar deles: no toque e, na TV, com o foco parado. */
@OptIn(ExperimentalCoroutinesApi::class)
class EarlyRequestsTest {
    /** Cada pedido ao "motor": o vídeo e se era um palpite. */
    private val requests = mutableListOf<Pair<String, Boolean>>()

    /** As respostas que o teste segura (o pedido fica em andamento até o teste soltar). */
    private val held = HashMap<String, CompletableDeferred<String>>()

    private fun TestScope.early(kept: Int = 3) = EarlyRequests(
        // Como o AppScope: um pedido que falha não derruba os outros.
        scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job])),
        now = { testScheduler.currentTime },
        maxAgeMs = MAX_AGE_MS,
        kept = kept,
        dwellMs = DWELL_MS,
        usable = { it.startsWith("ok") },
        request = { id, guess ->
            requests += id to guess
            held[id]?.await() ?: "ok $id"
        },
    )

    @Test
    fun aVideoAskedOnTheTapIsNotAskedAgainByThePlayer() = runTest {
        val early = early()
        early.asked("a")
        runCurrent()
        assertEquals("ok a", early.take("a"))
        assertEquals(listOf("a" to false), requests)
        // Vale uma vez: depois de um erro do player, o vídeo é pedido de novo.
        assertNull(early.take("a"))
    }

    @Test
    fun focusRestingOnACardAsksForTheVideoBeforeTheOk() = runTest {
        val early = early()
        early.focused("a")
        advanceTimeBy(DWELL_MS - 1)
        runCurrent()
        assertTrue(requests.isEmpty())
        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf("a" to true), requests)
        // O OK aproveita o que já foi pedido.
        early.asked("a")
        runCurrent()
        assertEquals("ok a", early.take("a"))
        assertEquals(1, requests.size)
    }

    @Test
    fun focusPassingThroughCardsAsksForNothing() = runTest {
        val early = early()
        for (id in listOf("a", "b", "c")) {
            early.focused(id)
            advanceTimeBy(DWELL_MS / 2)
            early.unfocused(id)
        }
        advanceTimeBy(DWELL_MS * 3)
        runCurrent()
        assertTrue(requests.isEmpty())
    }

    @Test
    fun onlyOneGuessRunsAtATime() = runTest {
        val early = early()
        held["a"] = CompletableDeferred()
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        // O foco foi para outro card com o palpite de "a" ainda em andamento: "b" espera.
        early.unfocused("a")
        early.focused("b")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        assertEquals(listOf("a" to true), requests)
        held.getValue("a").complete("ok a")
        runCurrent()
        assertEquals(listOf("a" to true, "b" to true), requests)
    }

    @Test
    fun theOkDoesNotWaitBehindTheDwell() = runTest {
        val early = early()
        early.focused("a")
        advanceTimeBy(DWELL_MS / 2)
        early.asked("a")
        runCurrent()
        assertEquals(listOf("a" to false), requests)
        advanceTimeBy(DWELL_MS * 2)
        runCurrent()
        assertEquals(1, requests.size)
    }

    @Test
    fun aGuessThatFailedIsAskedAgainForReal() = runTest {
        val early = early()
        held["a"] = CompletableDeferred<String>().apply { complete("unplayable") }
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        // O palpite não trouxe o vídeo: o OK pede de novo, e o player não recebe o palpite.
        early.asked("a")
        runCurrent()
        assertEquals(listOf("a" to true, "a" to false), requests)
        assertEquals("unplayable", early.take("a"))
    }

    @Test
    fun aGuessThatThrewIsNotAnAnswer() = runTest {
        val early = early()
        held["a"] = CompletableDeferred<String>().apply { completeExceptionally(IOException("no network")) }
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        assertNull(early.take("a"))
    }

    @Test
    fun whatTheUserAskedForComesAsItIs() = runTest {
        val early = early()
        held["a"] = CompletableDeferred<String>().apply { complete("unplayable") }
        early.asked("a")
        runCurrent()
        // Um vídeo que não toca não é pedido duas vezes: o player mostra o motivo.
        assertEquals("unplayable", early.take("a"))
        assertEquals(1, requests.size)
    }

    @Test
    fun comingBackToARecentCardDoesNotAskAgain() = runTest {
        val early = early()
        for (id in listOf("a", "b")) {
            early.focused(id)
            advanceTimeBy(DWELL_MS + 1)
            runCurrent()
            early.unfocused(id)
        }
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        assertEquals(listOf("a" to true, "b" to true), requests)
        assertEquals("ok a", early.take("a"))
    }

    @Test
    fun anOldAnswerIsNotUsed() = runTest {
        val early = early()
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        advanceTimeBy(MAX_AGE_MS)
        assertNull(early.take("a"))
    }

    @Test
    fun onlyTheLastFewAreKept() = runTest {
        val early = early(kept = 2)
        for (id in listOf("a", "b", "c")) {
            early.focused(id)
            advanceTimeBy(DWELL_MS + 1)
            runCurrent()
            early.unfocused(id)
        }
        assertNull(early.take("a"))
        assertEquals("ok b", early.take("b"))
        assertEquals("ok c", early.take("c"))
    }

    @Test
    fun backFromThePlayerTheFocusOnTheSameCardIsNotAGuess() = runTest {
        val early = early()
        early.asked("a")
        runCurrent()
        assertEquals("ok a", early.take("a"))
        early.focused("a")
        advanceTimeBy(DWELL_MS * 2)
        runCurrent()
        assertEquals(listOf("a" to false), requests)
    }

    @Test
    fun thePlayerWaitsForAGuessStillRunning() = runTest {
        val early = early()
        held["a"] = CompletableDeferred()
        early.focused("a")
        advanceTimeBy(DWELL_MS + 1)
        runCurrent()
        early.asked("a")
        val taken = async { early.take("a") }
        runCurrent()
        assertTrue(taken.isActive)
        held.getValue("a").complete("ok a")
        assertEquals("ok a", taken.await())
        assertEquals(1, requests.size)
    }

    private companion object {
        const val DWELL_MS = 600L
        const val MAX_AGE_MS = 60_000L
    }
}
