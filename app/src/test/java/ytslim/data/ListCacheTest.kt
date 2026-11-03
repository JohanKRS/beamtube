package ytslim.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Playlists e Histórico guardados no aparelho para abrirem na hora. */
@RunWith(RobolectricTestRunner::class)
class ListCacheTest {
    private fun video(id: String, index: Int = 0) = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "UC1", thumbnailUrl = null, info = "1K views",
        published = "3 hours ago", durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 40, playlistId = "WL", playlistIndex = index,
    )

    @Before
    fun setUp() {
        ListCache.init(ApplicationProvider.getApplicationContext())
        runBlocking { ListCache.clear() }
    }

    @Test
    fun eachListComesBackAsSaved() {
        runBlocking {
            assertNull(ListCache.read("playlist-WL"))
            val videos = listOf(video("a", 0), video("b", 1))
            ListCache.save("playlist-WL", "Watch later", videos)
            ListCache.save("history", null, listOf(video("h")))
            val saved = ListCache.read("playlist-WL")!!
            assertEquals(videos, saved.videos)
            assertEquals("Watch later", saved.title)
            assertEquals(listOf("h"), ListCache.read("history")!!.videos.map { it.id })
        }
    }

    @Test
    fun onlyTheStartOfAHugeListIsKept() {
        runBlocking {
            ListCache.save("playlist-WL", null, (0 until 700).map { video("v$it", it) })
            val saved = ListCache.read("playlist-WL")!!.videos
            assertEquals(ListCache.MAX_VIDEOS, saved.size)
            assertEquals("v0", saved.first().id)
        }
    }

    @Test
    fun anOldListOrSigningOutShowsNothing() {
        runBlocking {
            ListCache.save("history", null, listOf(video("a")))
            val later = System.currentTimeMillis() + ListCache.MAX_AGE_MS + 60_000
            assertNull(ListCache.read("history", nowMs = later))
            ListCache.clear()
            assertNull(ListCache.read("history"))
        }
    }
}
