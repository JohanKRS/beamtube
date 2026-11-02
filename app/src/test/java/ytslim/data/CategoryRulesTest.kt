package ytslim.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryRulesTest {
    private fun video(id: String, thumb: String? = "t$id") = Video(
        id = id, title = id, channelName = null, channelId = null, thumbnailUrl = thumb, info = null, published = null,
        durationMs = 0, isLive = false, isUpcoming = false, isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    @Test
    fun keywordsTakeTurnsAndRepeatsStayInTheirFirstPlace() {
        val joinery = listOf(video("a"), video("b"), video("c"))
        val tools = listOf(video("x"), video("a"))
        val bench = listOf(video("m"))
        val merged = CategoryRules.interleave(listOf(joinery, tools, bench, emptyList()))
        assertEquals(listOf("a", "x", "m", "b", "c"), merged.map { it.id })
    }

    @Test
    fun mosaicTakesTheFirstFourDifferentThumbnails() {
        val videos = listOf(video("a"), video("b", thumb = null), video("c", thumb = "ta"), video("d"), video("e"), video("f"))
        assertEquals(listOf("ta", "td", "te", "tf"), CategoryRules.thumbnails(videos))
    }

    @Test
    fun categoriesSurviveTheTripToTheDevice() {
        val category = Category("woodworking", "Woodworking", listOf("Woodworking"))
            .withFilters(SearchFilters(sort = SortBy.VIEWS, duration = SearchDuration.AT_LEAST_30, beforeYear = 2020))
            .copy(thumbnails = listOf("u1"))
        val themes = ThemeSet(updatedAt = 1_700_000_000_000, categories = listOf(category))
        val json = Json { ignoreUnknownKeys = true }
        val back = json.decodeFromString(ThemeSet.serializer(), json.encodeToString(ThemeSet.serializer(), themes))
        assertEquals(themes, back)
        assertEquals(SearchFilters(sort = SortBy.VIEWS, duration = SearchDuration.AT_LEAST_30, beforeYear = 2020), back.categories.first().filters)
    }
}
