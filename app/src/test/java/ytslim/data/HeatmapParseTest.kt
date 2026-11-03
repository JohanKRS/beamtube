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
        val segments = HeatmapRepository.parse(json).heat
        assertEquals(listOf(0L, 15_420L), segments.map { it.startMs })
        assertEquals(1f, segments[0].intensity)
        assertEquals(0.25f, segments[1].intensity)
    }

    @Test
    fun noHeatmapIsEmpty() {
        val markers = HeatmapRepository.parse("""{"contents":{}}""")
        assertTrue(markers.heat.isEmpty())
        assertTrue(markers.chapters.isEmpty())
    }

    // O formato da página "next" do cliente WEB (aircAruvnKk, lido em 2026-11-03), resumido.
    @Test
    fun readsTheChaptersOfTheTimeBar() {
        val json = """
            {"playerOverlays":{"playerOverlayRenderer":{"decoratedPlayerBarRenderer":{"decoratedPlayerBarRenderer":{"playerBar":
              {"multiMarkersPlayerBarRenderer":{"markersMap":[
                {"key":"HEATSEEKER","value":{"heatmap":{}}},
                {"key":"DESCRIPTION_CHAPTERS","value":{"chapters":[
                  {"chapterRenderer":{"title":{"simpleText":"Series preview"},"timeRangeStartMillis":67000}},
                  {"chapterRenderer":{"title":{"simpleText":"Introduction example"},"timeRangeStartMillis":0}},
                  {"chapterRenderer":{"title":{"runs":[{"text":"What are "},{"text":"neurons?"}]},"timeRangeStartMillis":162000}}
                ]}}
              ]}}
            }}}}}
        """.trimIndent()
        val chapters = HeatmapRepository.parse(json).chapters
        assertEquals(listOf(0L, 67_000L, 162_000L), chapters.map { it.startMs })
        assertEquals(listOf("Introduction example", "Series preview", "What are neurons?"), chapters.map { it.title })
        assertEquals("Series preview", Chapters.at(chapters, 100_000)?.title)
    }

    @Test
    fun aSingleChapterIsNoChapters() {
        val json = """
            {"playerOverlays":{"playerOverlayRenderer":{"decoratedPlayerBarRenderer":{"decoratedPlayerBarRenderer":{"playerBar":
              {"multiMarkersPlayerBarRenderer":{"markersMap":[{"value":{"chapters":[
                {"chapterRenderer":{"title":{"simpleText":"Video"},"timeRangeStartMillis":0}}
              ]}}]}}
            }}}}}
        """.trimIndent()
        assertTrue(HeatmapRepository.parse(json).chapters.isEmpty())
    }
}
