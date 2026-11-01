package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedRulesTest {
    private fun video(
        id: String,
        published: String? = null,
        live: Boolean = false,
        upcoming: Boolean = false,
        shorts: Boolean = false,
    ) = Video(
        id = id, title = id, channelName = "c", channelId = "UC$id", thumbnailUrl = null, info = null,
        published = published, durationMs = 600_000, isLive = live, isUpcoming = upcoming, isShorts = shorts,
        percentWatched = 0, playlistId = null, playlistIndex = -1,
    )

    @Test
    fun shortsNeverShowAndLivesFollowTheToggle() {
        assertFalse(FeedRules.keep(video("a", shorts = true), showLives = true))
        assertFalse(FeedRules.keep(video("b", live = true), showLives = false))
        assertFalse(FeedRules.keep(video("c", upcoming = true), showLives = false))
        assertTrue(FeedRules.keep(video("d", live = true), showLives = true))
        assertTrue(FeedRules.keep(video("e"), showLives = false))
    }

    @Test
    fun readsRelativeTimesInEnglishAndPortuguese() {
        val hour = 3_600_000L
        assertEquals(3 * hour, RelativeTime.agoMs("3 hours ago"))
        assertEquals(3 * hour, RelativeTime.agoMs("há 3 horas"))
        assertEquals(2 * 24 * hour, RelativeTime.agoMs("Streamed 2 days ago"))
        assertEquals(24 * hour, RelativeTime.agoMs("1 dia atrás"))
        assertEquals(2 * 30 * 24 * hour, RelativeTime.agoMs("há 2 meses"))
        assertEquals(5 * 60_000L, RelativeTime.agoMs("5 minutes ago"))
        // Visualizações não são tempo.
        assertEquals(4 * 7 * 24 * hour, RelativeTime.agoMs("Canal • 12 mil visualizações • há 4 semanas"))
        assertEquals(hour, RelativeTime.agoMs("Channel • 1.2M views • 1 hour ago"))
        assertNull(RelativeTime.agoMs("Channel • 12K views"))
        assertNull(RelativeTime.agoMs(null))
    }

    @Test
    fun estimateKeepsYouTubeOrder() {
        val now = 1_000_000_000L
        val timed = FeedMerge.estimate(
            listOf(video("a", "1 hour ago"), video("b", null), video("c", "30 minutes ago"), video("d", "2 days ago")),
            now,
        )
        assertEquals(now - 3_600_000, timed[0].atMs)
        assertEquals(timed[0].atMs, timed[1].atMs) // sem data: herda a anterior
        assertEquals(timed[1].atMs, timed[2].atMs) // "30 min" depois de "1 h" não sobe na lista
        assertTrue(timed[3].atMs < timed[2].atMs)
    }

    @Test
    fun playlistVideosWaitForTheirPageAndNeverDuplicate() {
        val now = 10_000_000_000L
        val feed = FeedMerge.estimate(listOf(video("a", "1 hour ago"), video("b", "1 day ago")), now)
        val fresh = Timed(video("p1"), now - 2 * 3_600_000) // entre a e b
        val old = Timed(video("p2"), now - 5L * 24 * 3_600_000) // mais antigo que a página
        val dup = Timed(video("a"), now)

        val partial = FeedMerge.merge(feed, listOf(fresh, old, dup), feedComplete = false)
        assertEquals(listOf("a", "p1", "b"), partial.map { it.id })

        val complete = FeedMerge.merge(feed, listOf(fresh, old, dup), feedComplete = true)
        assertEquals(listOf("a", "p1", "b", "p2"), complete.map { it.id })
    }
}
