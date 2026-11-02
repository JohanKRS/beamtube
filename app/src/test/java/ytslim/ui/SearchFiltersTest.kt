package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ytslim.data.ResultType
import ytslim.data.SearchDuration
import ytslim.data.SearchFilters
import ytslim.data.SearchParams
import ytslim.data.SortBy
import ytslim.data.UploadDate

@RunWith(RobolectricTestRunner::class)
class SearchFiltersTest {
    @Test
    fun noFilterSendsNoParams() {
        assertNull(SearchParams.of(SearchFilters()))
    }

    /** Os mesmos valores da tabela do motor (que são os do site), para as combinações que ela tem. */
    @Test
    fun paramsMatchWhatYouTubeUses() {
        assertEquals("EgIQAQ%3D%3D", SearchParams.of(SearchFilters(type = ResultType.VIDEO)))
        assertEquals("EgIQAg%3D%3D", SearchParams.of(SearchFilters(type = ResultType.CHANNEL)))
        assertEquals("EgQQARgC", SearchParams.of(SearchFilters(type = ResultType.VIDEO, duration = SearchDuration.OVER_20)))
        assertEquals("EgQQARgD", SearchParams.of(SearchFilters(type = ResultType.VIDEO, duration = SearchDuration.FROM_4_TO_20)))
        assertEquals("EgQIARAB", SearchParams.of(SearchFilters(date = UploadDate.HOUR, type = ResultType.VIDEO)))
        assertEquals("EgYIAhABGAE%3D", SearchParams.of(SearchFilters(UploadDate.TODAY, SearchDuration.UNDER_4, ResultType.VIDEO)))
        assertEquals("CAI%3D", SearchParams.of(SearchFilters(sort = SortBy.DATE)))
        assertEquals("CAMSAhAB", SearchParams.of(SearchFilters(type = ResultType.VIDEO, sort = SortBy.VIEWS)))
        assertEquals("CAESAhAB", SearchParams.of(SearchFilters(type = ResultType.VIDEO, sort = SortBy.RATING)))
    }

    @Test
    fun dateChoiceHasTheRecentPeriodsThenBeforeEachYear() {
        val date = searchFilterChoices(SearchFilters(), currentYear = 2026).first()
        assertTrue(date.isDefault)
        assertEquals(6 + (2026 - 2006 + 1), date.options.size)
        val before2022 = date.options.single { it.arg == 2022 }
        assertEquals(SearchFilters(beforeYear = 2022), before2022.filters)
        // "Before" substitui o período recente, e vice-versa.
        val picked = searchFilterChoices(SearchFilters(date = UploadDate.WEEK, beforeYear = null), 2026).first()
            .options.single { it.arg == 2022 }.filters
        assertEquals(UploadDate.ANY, picked.date)
        val back = searchFilterChoices(picked, 2026).first()
        assertEquals(2022, back.selected.arg)
        assertNull(back.options[3].filters.beforeYear)
        assertEquals(UploadDate.WEEK, back.options[3].filters.date)
    }

    @Test
    fun durationChoiceIncludesThirtyFortyFiveAndSixtyMinutes() {
        val duration = searchFilterChoices(SearchFilters())[1]
        assertEquals(
            listOf(
                SearchDuration.ANY, SearchDuration.UNDER_4, SearchDuration.FROM_4_TO_20, SearchDuration.OVER_20,
                SearchDuration.AT_LEAST_30, SearchDuration.AT_LEAST_45, SearchDuration.AT_LEAST_60,
            ),
            duration.options.map { it.filters.duration },
        )
        // Para o YouTube, "30 minutos ou mais" é "mais de 20"; o app corta o resto.
        assertEquals(SearchParams.of(SearchFilters(duration = SearchDuration.OVER_20)), SearchParams.of(SearchFilters(duration = SearchDuration.AT_LEAST_45)))
        assertTrue(SearchDuration.AT_LEAST_30.matches(30 * 60_000L))
        assertFalse(SearchDuration.AT_LEAST_30.matches(29 * 60_000L + 59_000))
        assertFalse(SearchDuration.AT_LEAST_60.matches(0))
        assertTrue(SearchDuration.AT_LEAST_60.checkedLocally)
        assertFalse(SearchDuration.OVER_20.checkedLocally)
    }

    @Test
    fun beforeYearGoesIntoTheQueryText() {
        assertEquals("minecraft before:2015-01-01", SearchParams.query("minecraft", SearchFilters(beforeYear = 2015)))
        assertEquals("minecraft", SearchParams.query("minecraft", SearchFilters()))
    }

    @Test
    fun feedChoicesLeaveOutTypeAndRating() {
        val feed = feedFilterChoices(SearchFilters(sort = SortBy.VIEWS), currentYear = 2026)
        assertEquals(3, feed.size)
        val sort = feed[2]
        assertFalse(sort.isDefault)
        assertEquals(listOf(SortBy.RELEVANCE, SortBy.DATE, SortBy.VIEWS), sort.options.map { it.filters.sort })
        assertEquals(SortBy.VIEWS, sort.selected.filters.sort)
    }
}
