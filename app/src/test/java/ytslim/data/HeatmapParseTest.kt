package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapParseTest {
    @Test
    fun readsOnlyTheHeatmapMarkers() {
        val json = """
            {"frameworkUpdates":{"entityBatchUpdate":{"mutations":[
              {"payload":{"macroMarkersListEntity":{"markersList":{"markerType":"MARKER_TYPE_CHAPTERS","markers":[{"startMillis":"5"}]}}}},
              {"payload":{"macroMarkersListEntity":{"markersList":{"markerType":"MARKER_TYPE_HEATMAP","markers":[
                {"startMillis":"15420","durationMillis":"15420","intensityScoreNormalized":0.25},
                {"startMillis":"0","durationMillis":"15420","intensityScoreNormalized":1}
              ]}}}}
            ]}}}
        """.trimIndent()
        val segments = HeatmapRepository.parse(json)
        assertEquals(listOf(0L, 15_420L), segments.map { it.startMs })
        assertEquals(1f, segments[0].intensity)
        assertEquals(0.25f, segments[1].intensity)
    }

    @Test
    fun noHeatmapIsEmpty() {
        assertTrue(HeatmapRepository.parse("""{"contents":{}}""").isEmpty())
    }
}
