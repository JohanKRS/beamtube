package ytslim.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** As Inscrições guardadas no aparelho para o app abrir com elas na hora. */
@RunWith(RobolectricTestRunner::class)
class FeedCacheTest {
    private fun timed(id: String, at: Long) = Timed(
        Video(
            id = id, title = "Video $id", channelName = "Channel", channelId = "UC1", thumbnailUrl = null, info = "1K views",
            published = "3 hours ago", durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
            percentWatched = 40, playlistId = null, playlistIndex = 0, fromPlaylist = if (id == "p") "Lectures" else null,
        ),
        at,
    )

    @Before
    fun setUp() {
        FeedCache.init(ApplicationProvider.getApplicationContext())
        runBlocking { FeedCache.clear() }
    }

    @Test
    fun theFeedAndThePlaylistVideosComeBackAsSaved() = runBlocking {
        assertNull(FeedCache.read())
        // Só os vídeos das playlists, sem o feed, não abrem nada.
        FeedCache.saveExtras(listOf(timed("p", 1)))
        assertNull(FeedCache.read())

        val feed = listOf(timed("a", 3), timed("b", 2))
        FeedCache.saveFeed(feed)
        FeedCache.saveExtras(listOf(timed("p", 1)))
        val saved = FeedCache.read()!!
        assertEquals(feed, saved.feed)
        assertEquals(listOf(timed("p", 1)), saved.extras)

        // Um feed novo mantém os vídeos das playlists até eles chegarem de novo.
        FeedCache.saveFeed(listOf(timed("c", 4)))
        assertEquals(listOf("c"), FeedCache.read()!!.feed.map { it.video.id })
        assertEquals(listOf("p"), FeedCache.read()!!.extras.map { it.video.id })
    }

    @Test
    fun anOldFeedOrSigningOutShowsNothing() = runBlocking {
        FeedCache.saveFeed(listOf(timed("a", 1)))
        val later = System.currentTimeMillis() + FeedCache.MAX_AGE_MS + 60_000
        assertNull(FeedCache.read(nowMs = later))
        FeedCache.clear()
        assertNull(FeedCache.read())
    }
}
