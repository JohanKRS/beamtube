package ytslim.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaylistLedgerTest {
    @Test
    fun firstReadIsBaselineAndLaterAdditionsAreNew() {
        val first = Ledger.update(null, listOf("a", "b"), nowMs = 100)
        assertTrue(Ledger.newSince(first).isEmpty())

        val second = Ledger.update(first, listOf("c", "a", "b"), nowMs = 200)
        assertEquals(mapOf("c" to 200L), Ledger.newSince(second))

        // Ver de novo não muda a data de quando apareceu.
        val third = Ledger.update(second, listOf("c", "a", "b", "d"), nowMs = 300)
        assertEquals(mapOf("c" to 200L, "d" to 300L), Ledger.newSince(third))
    }

    @Test
    fun storeSurvivesARestart() = runTest {
        val file = File.createTempFile("ledger", ".json").apply { delete() }
        try {
            LedgerStore(file).record("PL1", listOf("a"), 100)
            LedgerStore(file).record("PL1", listOf("a", "b"), 200)
            val entry = LedgerStore(file).get("PL1")!!
            assertEquals(mapOf("b" to 200L), Ledger.newSince(entry))
        } finally {
            file.delete()
        }
    }
}
