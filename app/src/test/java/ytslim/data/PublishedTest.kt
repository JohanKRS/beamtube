package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ytslim.data.Published.Age
import ytslim.data.Published.Span

/** Há quanto tempo o vídeo foi publicado, na linha do canal do player (pedido do Johan, 2026-11-13). */
class PublishedTest {
    private fun video(published: String?, info: String?) = Video(
        id = "v", title = "t", channelName = "Veritasium", channelId = "c", thumbnailUrl = null, info = info,
        published = published, durationMs = 0, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    @Test
    fun theCardTimeComesFirst() {
        assertEquals("3 hours ago", Published.of(video("3 hours ago", "Veritasium • 1.2M views • 2 days ago")))
    }

    @Test
    fun withoutItTheTimeComesFromTheVideoLine() {
        assertEquals("2 days ago", Published.of(video(null, "Veritasium • 1.2M views • 2 days ago")))
        assertEquals("Streamed 5 days ago", Published.of(video(" ", "Veritasium · 12K views · Streamed 5 days ago")))
    }

    @Test
    fun viewsAndLivesAreNotATime() {
        assertNull(Published.of(video(null, "Veritasium • 1.2M views")))
        assertNull(Published.of(video(null, "Veritasium • 1.2K watching")))
        assertNull(Published.of(video(null, null)))
    }

    private val nov13 = Published.epochDay(2026, 11, 13)

    @Test
    fun epochDayMatchesTheCalendar() {
        assertEquals(0L, Published.epochDay(1970, 1, 1))
        assertEquals(20_405L, Published.epochDay(2025, 11, 13))
        assertEquals(20_036L, Published.epochDay(2024, 11, 9))
        assertEquals(19_782L, Published.epochDay(2024, 2, 29))
    }

    @Test
    fun theVideoPageDateBecomesAnAge() {
        assertEquals(Age(0, Span.DAY), Published.age("Nov 13, 2026", nov13))
        assertEquals(Age(3, Span.DAY), Published.age("Nov 10, 2026", nov13))
        assertEquals(Age(2, Span.WEEK), Published.age("Oct 30, 2026", nov13))
        assertEquals(Age(1, Span.MONTH), Published.age("Oct 1, 2026", nov13))
        assertEquals(Age(10, Span.MONTH), Published.age("Dec 20, 2025", nov13))
        assertEquals(Age(1, Span.YEAR), Published.age("Nov 13, 2025", nov13))
        assertEquals(Age(6, Span.YEAR), Published.age("Sep 1, 2020", nov13))
    }

    @Test
    fun wordsAroundTheDateAreIgnored() {
        assertEquals(Age(3, Span.DAY), Published.age("Premiered Nov 10, 2026", nov13))
        assertEquals(Age(3, Span.DAY), Published.age("Streamed live on November 10, 2026", nov13))
        assertEquals(Age(3, Span.DAY), Published.age("10 Nov 2026", nov13))
    }

    @Test
    fun aDateFromTomorrowIsToday() {
        // A data do YouTube vem no fuso dele: perto da meia-noite, ela pode estar um dia à frente.
        assertEquals(Age(0, Span.DAY), Published.age("Nov 14, 2026", nov13))
    }

    @Test
    fun notADate() {
        assertNull(Published.age("3 days ago", nov13))
        assertNull(Published.age("13 de nov. de 2026", nov13))
        assertNull(Published.age(null, nov13))
    }
}
