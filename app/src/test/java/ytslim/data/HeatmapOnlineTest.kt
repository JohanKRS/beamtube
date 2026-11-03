package ytslim.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Fala com o YouTube de verdade; só roda com -Pytslim.online=true. */
class HeatmapOnlineTest {
    @Test
    fun popularVideoHasAHeatmapAndChapters() = runBlocking {
        assumeTrue(System.getProperty("ytslim.online") == "true")
        val markers = HeatmapRepository.load("0e3GPea1Tyg")
        val segments = markers.heat
        println("[online] heatmap: ${segments.size} trechos")
        assertEquals(100, segments.size)
        assertTrue(segments.all { it.intensity in 0f..1f })
        println("[online] trechos que o avanço pula: ${SkipRules.valleys(segments)}")
        println("[online] capítulos: ${markers.chapters}")
        assertTrue(markers.chapters.size >= 2)
    }
}
