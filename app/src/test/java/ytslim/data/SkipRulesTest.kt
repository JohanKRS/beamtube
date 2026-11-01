package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkipRulesTest {
    // Gráfico real de "mais repetidos" de 0e3GPea1Tyg (100 trechos de 15,42 s), lido em 2026-11-01.
    // O YouTube marca "Most replayed" de 339 s a 370 s (trechos 22 e 23) e de 740 s a 771 s (48 e 49).
    private val curve = (
        "1.00 0.25 0.16 0.13 0.14 0.13 0.18 0.16 0.06 0.07 0.05 0.26 0.12 0.12 0.14 0.18 0.17 0.14 0.09 0.15 " +
            "0.14 0.07 0.21 0.40 0.09 0.09 0.16 0.15 0.10 0.11 0.08 0.06 0.04 0.09 0.28 0.34 0.15 0.14 0.13 0.13 " +
            "0.15 0.10 0.15 0.12 0.10 0.10 0.06 0.12 0.10 0.48 0.10 0.14 0.12 0.16 0.16 0.18 0.18 0.14 0.16 0.18 " +
            "0.17 0.11 0.11 0.10 0.09 0.05 0.18 0.14 0.01 0.28 0.14 0.04 0.03 0.04 0.04 0.01 0.00 0.01 0.01 0.16 " +
            "0.02 0.11 0.11 0.11 0.10 0.09 0.09 0.11 0.09 0.10 0.11 0.12 0.09 0.06 0.04 0.03 0.08 0.22 0.18 0.29"
        ).split(' ').mapIndexed { i, v -> HeatSegment(i * 15_420L, 15_420L, v.toFloat()) }

    private fun at(index: Int) = index * 15_420L

    @Test
    fun jumpsFromPeakToPeak() {
        assertEquals(at(11), SkipRules.nextHighlight(curve, 0))
        // Cai no começo da subida, onde o próprio YouTube começa a marcar o trecho.
        assertEquals(at(22), SkipRules.nextHighlight(curve, at(11)))
        assertEquals(at(34), SkipRules.nextHighlight(curve, at(22)))
        assertEquals(at(49), SkipRules.nextHighlight(curve, at(34)))
        assertEquals(at(69), SkipRules.nextHighlight(curve, at(49)))
    }

    @Test
    fun ignoresAPeakThatIsTooClose() {
        // A 2 s do trecho 22: pular só 2 s não vale; vai para o seguinte.
        assertEquals(at(34), SkipRules.nextHighlight(curve, at(22) - 2_000))
    }

    @Test
    fun nothingAfterTheLastPeak() {
        assertNull(SkipRules.nextHighlight(curve, at(99)))
        assertNull(SkipRules.nextHighlight(emptyList(), 0))
    }

    @Test
    fun chaptersAreTheFallback() {
        assertEquals(120_000L, SkipRules.nextChapter(listOf(300_000, 120_000), 60_000))
        assertEquals(300_000L, SkipRules.nextChapter(listOf(300_000, 120_000), 118_000))
        assertNull(SkipRules.nextChapter(listOf(120_000), 200_000))
    }
}
