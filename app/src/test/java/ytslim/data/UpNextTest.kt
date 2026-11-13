package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpNextTest {
    private fun video(
        id: String,
        title: String = "Video $id",
        channel: String = "Other",
        channelId: String = "UC_OTHER",
        watched: Int = 0,
        shorts: Boolean = false,
        live: Boolean = false,
    ) = Video(
        id = id, title = title, channelName = channel, channelId = channelId, thumbnailUrl = null, info = null,
        published = null, durationMs = 600_000, isLive = live, isUpcoming = false, isShorts = shorts,
        percentWatched = watched, playlistId = null, playlistIndex = -1,
    )

    @Test
    fun picksTheNextFromTheOriginThenTheSameChannelThenRelated() {
        val related = listOf(video("r1"), video("r2"), video("same", channel = "Mine", channelId = "UC_MINE"), video("r3"))
        val after = listOf(video("a1"), video("a2"))
        val picked = UpNext.pick("now", "UC_MINE", "Mine", related, after, ContentFilters())
        assertEquals(listOf("a1", "same", "r1", "r2", "r3"), picked.map { it.id })
    }

    @Test
    fun leavesOutTheVideoItselfShortsLivesWatchedAndRepeats() {
        val related = listOf(
            video("now"), video("short", shorts = true), video("live", live = true), video("seen", watched = 95),
            video("half", watched = 50), video("a1"), video("ok"),
        )
        val picked = UpNext.pick("now", null, null, related, listOf(video("a1")), ContentFilters())
        assertEquals(listOf("a1", "half", "ok"), picked.map { it.id })
    }

    @Test
    fun followsTheTopicsAndBlockedChannels() {
        val filters = ContentFilters(
            excluded = listOf(Topic("Politics", listOf("election"))),
            preferred = listOf(Topic("Space", listOf("rocket"))),
            blocked = listOf(BlockedChannel("UC_BAD", "Bad")),
        )
        val related = listOf(
            video("r1"), video("vote", title = "Election night"), video("bad", channelId = "UC_BAD", channel = "Bad"),
            video("rocket", title = "A rocket launch"),
        )
        val picked = UpNext.pick("now", null, null, related, emptyList(), filters)
        assertEquals(listOf("rocket", "r1"), picked.map { it.id })
    }

    @Test
    fun stopsAtTheMaximum() {
        val related = (1..20).map { video("r$it") }
        assertEquals(UpNext.MAX, UpNext.pick("now", null, null, related, emptyList(), ContentFilters()).size)
    }

    @Test
    fun remembersOnlyTheListOfTheLastOpenedVideo() {
        val list = (1..6).map { video("v$it") }
        UpNext.opened(list, list[1])
        assertEquals(listOf("v3", "v4", "v5"), UpNext.after("v2").map { it.id })
        assertTrue(UpNext.after("v3").isEmpty())
        UpNext.opened(list, list.last())
        assertTrue(UpNext.after("v6").isEmpty())
    }

    @Test
    fun sleepModeKeepsTheOldEnd() {
        assertFalse(UpNext.sleeping(brightness = 100, queueLimited = false, turnOff = false))
        assertTrue(UpNext.sleeping(brightness = 40, queueLimited = false, turnOff = false))
        assertTrue(UpNext.sleeping(brightness = 100, queueLimited = true, turnOff = false))
        assertTrue(UpNext.sleeping(brightness = 100, queueLimited = false, turnOff = true))
    }
}
