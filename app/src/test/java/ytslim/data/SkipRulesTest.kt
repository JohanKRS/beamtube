package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SkipRulesTest {
    // Gráfico real de QW_jlUn4gA8 (Kurzgesagt, 100 trechos de 8,52 s), lido em 2026-11-02. O
    // SponsorBlock marca o patrocínio de 313,8 s a 395,9 s (trechos 37 a 46).
    private val sponsored = (
        "0.49 0.26 0.14 0.10 0.12 0.14 0.05 0.05 0.08 0.13 0.17 0.14 0.15 0.11 0.09 0.10 0.10 0.09 0.11 0.15 " +
            "0.12 0.15 0.19 0.18 0.15 0.18 0.17 0.17 0.19 0.17 0.14 0.46 0.53 0.30 0.29 0.10 0.09 0.06 0.02 0.03 " +
            "0.02 0.03 0.06 0.05 0.05 0.10 0.63 1.00 0.29 0.18 0.16 0.13 0.15 0.17 0.22 0.33 0.15 0.11 0.11 0.11 " +
            "0.11 0.15 0.14 0.07 0.07 0.09 0.13 0.17 0.18 0.25 0.46 0.53 0.26 0.16 0.16 0.29 0.26 0.15 0.70 0.17 " +
            "0.19 0.19 0.18 0.17 0.17 0.16 0.20 0.17 0.09 0.05 0.02 0.01 0.01 0.06 0.03 0.00 0.00 0.01 0.07 0.10"
        ).split(' ').mapIndexed { i, v -> HeatSegment(i * 8_520L, 8_520L, v.toFloat()) }

    private fun sponsoredAt(index: Int) = index * 8_520L

    @Test
    fun forwardInsideTheSponsorGoesToWhereTheAudienceComesBack() {
        // Logo antes, no começo, no meio e quase no fim do patrocínio: cai no fim dele (391,9 s).
        assertEquals(sponsoredAt(46), SkipRules.valleyEnd(sponsored, 310_000))
        assertEquals(sponsoredAt(46), SkipRules.valleyEnd(sponsored, 317_000))
        // No meio a audiência sobe um pouco (0,06 no trecho 42), mas ainda é o patrocínio.
        assertEquals(sponsoredAt(46), SkipRules.valleyEnd(sponsored, 360_000))
        assertEquals(sponsoredAt(46), SkipRules.valleyEnd(sponsored, 380_000))
    }

    @Test
    fun forwardIsNormalEverywhereElse() {
        assertNull(SkipRules.valleyEnd(sponsored, 0))
        assertNull(SkipRules.valleyEnd(sponsored, 200_000))
        // A mais de 20 s do começo do patrocínio, ainda não.
        assertNull(SkipRules.valleyEnd(sponsored, sponsoredAt(38) - 21_000))
        // Depois do pulo, o próximo avanço é o de sempre.
        assertNull(SkipRules.valleyEnd(sponsored, sponsoredAt(46)))
        // A menos de 5 s do fim do trecho, pular não vale a pena.
        assertNull(SkipRules.valleyEnd(sponsored, sponsoredAt(46) - 3_000))
        assertNull(SkipRules.valleyEnd(sponsored, 500_000))
        // O vale do fim vai até o último trecho: pular seria só encerrar o vídeo.
        assertNull(SkipRules.valleyEnd(sponsored, 800_000))
        assertNull(SkipRules.valleyEnd(emptyList(), 0))
    }

    @Test
    fun marksWhereEachSkipStarts() {
        // Só o patrocínio (a marca cai onde a audiência já despencou, 10 s depois do começo dele);
        // o vale do fim vai até o último trecho e não conta.
        assertEquals(listOf(sponsoredAt(38)), SkipRules.valleys(sponsored))
        assertEquals(emptyList<Long>(), SkipRules.valleys(emptyList()))
    }
}
