package ytslim.data

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayQueueTest {
    private fun video(id: String, upcoming: Boolean = false) = Video(
        id = id, title = "Title $id", channelName = "Channel", channelId = "UC1", thumbnailUrl = null, info = null,
        published = null, durationMs = 60_000L, isLive = false, isUpcoming = upcoming, isShorts = false,
        percentWatched = 0, playlistId = "PL1", playlistIndex = -1,
    )

    private val list = listOf(video("a"), video("b"), video("c"), video("d"))

    @After
    fun tearDown() = PlayQueue.clear()

    private fun drain(): List<String> = runBlocking { buildList { while (true) add(PlayQueue.next()?.id ?: break) } }

    @Test
    fun playsTheVideosAfterTheOneOpened() {
        PlayQueue.start(list, "b", page = null)
        assertTrue(PlayQueue.active.value)
        assertEquals(PlayQueue.ALL, PlayQueue.left.value)
        assertEquals(listOf("c", "d"), drain())
    }

    @Test
    fun theLimitStopsTheSequenceAndCountsDown() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(2)
        assertEquals("b", runBlocking { PlayQueue.next() }?.id)
        assertEquals(1, PlayQueue.left.value)
        assertEquals("c", runBlocking { PlayQueue.next() }?.id)
        assertEquals(0, PlayQueue.left.value)
        assertNull(runBlocking { PlayQueue.next() })
    }

    @Test
    fun noneMeansOnlyTheCurrentVideo() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(0)
        assertNull(runBlocking { PlayQueue.next() })
    }

    @Test
    fun aVideoThatFailedIsReplacedWithoutUsingTheLimit() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(1)
        assertEquals("b", runBlocking { PlayQueue.next() }?.id)
        // "b" não tocou: "c" toma o lugar dele.
        assertEquals("c", runBlocking { PlayQueue.next(counts = false) }?.id)
        assertNull(runBlocking { PlayQueue.next() })
    }

    @Test
    fun skipsScheduledAndRepeatedVideos() {
        PlayQueue.start(listOf(video("a"), video("b", upcoming = true), video("c"), video("a"), video("c")), "a", page = null)
        assertEquals(listOf("c"), drain())
    }

    @Test
    fun openingAnotherVideoClearsTheSequence() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(1)
        PlayQueue.clear()
        assertFalse(PlayQueue.active.value)
        assertEquals(PlayQueue.ALL, PlayQueue.left.value)
        assertNull(runBlocking { PlayQueue.next() })
    }

    @Test
    fun peekShowsTheNextWithoutAdvancing() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(2)
        assertEquals("b", PlayQueue.peek()?.id)
        assertEquals("b", PlayQueue.peek()?.id)
        assertEquals(2, PlayQueue.left.value)
        assertEquals("b", runBlocking { PlayQueue.next() }?.id)
        assertEquals("c", PlayQueue.peek()?.id)
    }

    @Test
    fun peekRespectsTheLimitAndAnEmptySequence() {
        PlayQueue.start(list, "a", page = null)
        PlayQueue.setLeft(0)
        assertNull(PlayQueue.peek())
        PlayQueue.clear()
        assertNull(PlayQueue.peek())
    }

    @Test
    fun unknownVideoStartsNoSequence() {
        PlayQueue.start(list, "zzz", page = null)
        assertFalse(PlayQueue.active.value)
    }
}
