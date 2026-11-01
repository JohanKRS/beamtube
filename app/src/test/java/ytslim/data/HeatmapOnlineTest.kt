package ytslim.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Fala com o YouTube de verdade; só roda com -Pytslim.online=true. */
class HeatmapOnlineTest {
    @Test
    fun popularVideoHasAHeatmapAndSomethingToSkipTo() = runBlocking {
        assumeTrue(System.getProperty("ytslim.online") == "true")
        val segments = HeatmapRepository.load("0e3GPea1Tyg")
        println("[online] heatmap: ${segments.size} trechos")
        assertEquals(100, segments.size)
        assertTrue(segments.all { it.intensity in 0f..1f })
        val target = SkipRules.nextHighlight(segments, 0)
        println("[online] pular a partir do início: $target ms")
        assertNotNull(target)
    }
}
