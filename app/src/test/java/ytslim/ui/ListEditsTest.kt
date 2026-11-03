package ytslim.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** O prazo do Undo e as mudanças na lista da tela. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListEditsTest {
    @Test
    fun theAccountOnlyChangesWhenTheUndoWindowEnds() = runTest {
        var committed = 0
        PendingEdit(this, 5_000) { committed++ }
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(0, committed)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(1, committed)
    }

    @Test
    fun undoInTimeKeepsTheAccountAsItWas() = runTest {
        var committed = 0
        val edit = PendingEdit(this, 5_000) { committed++ }
        advanceTimeBy(3_000)
        assertTrue(edit.undo())
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(0, committed)
        // Desfazer de novo não faz nada.
        assertFalse(edit.undo())
    }

    @Test
    fun undoAfterTheWindowDoesNothing() = runTest {
        var committed = 0
        val edit = PendingEdit(this, 5_000) { committed++ }
        advanceTimeBy(6_000)
        runCurrent()
        assertFalse(edit.undo())
        assertEquals(1, committed)
    }

    @Test
    fun moveTakesTheItemToItsNewPlace() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "c", "a", "d"), ListEdits.moved(list, 0, 2))
        assertEquals(listOf("d", "a", "b", "c"), ListEdits.moved(list, 3, 0))
        assertEquals(list, ListEdits.moved(list, 1, 1))
        assertEquals(list, ListEdits.moved(list, 1, 9))
    }

    @Test
    fun putBackReturnsTheVideoToWhereItWas() {
        assertEquals(listOf("a", "b", "c"), ListEdits.putBack(listOf("a", "c"), "b", 1))
        assertEquals(listOf("a", "b", "c"), ListEdits.putBack(listOf("b", "c"), "a", 0))
        // A lista encolheu enquanto isso: o vídeo volta para o fim.
        assertEquals(listOf("a", "z"), ListEdits.putBack(listOf("a"), "z", 5))
    }
}
