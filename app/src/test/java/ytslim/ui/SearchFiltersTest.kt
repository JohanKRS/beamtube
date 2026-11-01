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
    fun choicesCycleThroughTheValuesAndBackToAny() {
        val date = searchFilterChoices(SearchFilters()).first()
        assertTrue(date.isDefault)
        var filters = SearchFilters()
        val seen = mutableListOf<UploadDate>()
        repeat(UploadDate.entries.size) {
            filters = searchFilterChoices(filters).first().next()
            seen += filters.date
        }
        assertEquals(UploadDate.entries.drop(1) + UploadDate.ANY, seen)
        val sort = searchFilterChoices(SearchFilters(sort = SortBy.VIEWS))[3]
        assertFalse(sort.isDefault)
        assertEquals(SortBy.VIEWS, sort.options.single { it.selected }.filters.sort)
    }
}
