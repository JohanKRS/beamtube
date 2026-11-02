package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class FeedFilterTest {
    private fun video(id: String, minutes: Int = 10, published: String? = null, info: String? = null) = Video(
        id = id, title = id, channelName = "Channel", channelId = "UC1", thumbnailUrl = null, info = info,
        published = published, durationMs = minutes * 60_000L, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = -1,
    )

    private val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 2, 12, 0, 0) }.timeInMillis

    @Test
    fun noFiltersKeepsTheListAsIs() {
        val list = listOf(video("a"), video("b"))
        assertEquals(list, FeedFilter.apply(list, SearchFilters(), now))
    }

    @Test
    fun durationKeepsOnlyLongEnoughVideos() {
        val list = listOf(video("short", 12), video("half", 30), video("long", 75), video("unknown", 0))
        val ids = { f: SearchFilters -> FeedFilter.apply(list, f, now).map { it.id } }
        assertEquals(listOf("half", "long"), ids(SearchFilters(duration = SearchDuration.AT_LEAST_30)))
        assertEquals(listOf("long"), ids(SearchFilters(duration = SearchDuration.AT_LEAST_60)))
        assertEquals(listOf("short"), ids(SearchFilters(duration = SearchDuration.FROM_4_TO_20)))
    }

    @Test
    fun recentPeriodsUseTheRelativeTime() {
        val list = listOf(
            video("hours", published = "5 hours ago"),
            video("days", published = "há 6 dias"),
            video("week", published = "1 week ago"),
            video("months", info = "Channel • 1.2M views • 11 months ago"),
            video("year", published = "1 year ago"),
            video("nodate"),
        )
        val ids = { d: UploadDate -> FeedFilter.apply(list, SearchFilters(date = d), now).map { it.id } }
        assertEquals(listOf("hours"), ids(UploadDate.TODAY))
        assertEquals(listOf("hours", "days"), ids(UploadDate.WEEK))
        assertEquals(listOf("hours", "days", "week", "months"), ids(UploadDate.YEAR))
    }

    @Test
    fun beforeYearKeepsOnlyVideosSurelyOlder() {
        // Em 2 de novembro de 2026: "4 years ago" pode ser de 2021 ou de 2022; "5 years ago" é de 2021 ou antes.
        val list = listOf(video("four", published = "4 years ago"), video("five", published = "5 years ago"), video("new", published = "2 days ago"))
        assertEquals(listOf("five"), FeedFilter.apply(list, SearchFilters(beforeYear = 2022), now).map { it.id })
        assertEquals(listOf("four", "five"), FeedFilter.apply(list, SearchFilters(beforeYear = 2023), now).map { it.id })
    }

    @Test
    fun sortsByNewestAndByViews() {
        val list = listOf(
            video("a", info = "Channel • 12K views • 3 days ago"),
            video("b", info = "Channel • 1.2M views • 2 weeks ago"),
            video("c", info = "Channel • 980 views • 5 hours ago"),
        )
        assertEquals(listOf("c", "a", "b"), FeedFilter.apply(list, SearchFilters(sort = SortBy.DATE), now).map { it.id })
        assertEquals(listOf("b", "a", "c"), FeedFilter.apply(list, SearchFilters(sort = SortBy.VIEWS), now).map { it.id })
    }

    @Test
    fun readsViewCountsInEnglishAndPortuguese() {
        assertEquals(1_200_000L, ViewCount.parse("Channel • 1.2M views • 3 days ago"))
        assertEquals(12_000L, ViewCount.parse("12K views"))
        assertEquals(1_234L, ViewCount.parse("Canal • 1,234 views"))
        assertEquals(1_200_000L, ViewCount.parse("Canal • 1,2 mi de visualizações • há 3 dias"))
        assertEquals(12_000L, ViewCount.parse("12 mil visualizações"))
        assertEquals(1_234L, ViewCount.parse("1.234 visualizações"))
        assertEquals(3_100_000_000L, ViewCount.parse("3.1B views"))
        assertEquals(0L, ViewCount.parse("No views"))
        assertNull(ViewCount.parse("Channel • 3 days ago"))
    }
}
