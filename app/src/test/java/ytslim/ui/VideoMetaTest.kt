package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ytslim.data.Video

class VideoMetaTest {
    private fun video(channelName: String?, info: String?) = Video(
        id = "v1", title = "Title", channelName = channelName, channelId = null, thumbnailUrl = null,
        info = info, published = null, durationMs = 60_000, isLive = false, isUpcoming = false,
        isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = -1,
    )

    @Test
    fun dropsTheChannelNameFromYouTubesLine() {
        assertEquals("12K views · 2 hours ago", metaAfterChannel("Bench Notes • 12K views • 2 hours ago", "Bench Notes", null))
        assertEquals(
            "16.1M views · 9 hours ago",
            metaAfterChannel("\u200EVeritasium\u200E • \u200E16.1M views\u200E \u200E•\u200E \u200E9 hours ago\u200E", "Veritasium", null),
        )
    }

    @Test
    fun dropsCollaborationPrefixWhenChannelIsAlreadyShown() {
        assertEquals(
            "500K views · 1 day ago",
            metaAfterChannel("3Blue1Brown and PolyaMath • 500K views • 1 day ago", "3Blue1Brown", null),
        )
    }

    @Test
    fun keepsTheLineWhenTheChannelIsNotInIt() {
        assertEquals("12K views · 2 hours ago", metaAfterChannel("12K views · 2 hours ago", "Bench Notes", null))
    }

    @Test
    fun fallsBackToThePublishedTime() {
        assertEquals("3 hours ago", metaAfterChannel("Bench Notes", "Bench Notes", "3 hours ago"))
        assertEquals("3 hours ago", metaAfterChannel(null, null, "3 hours ago"))
        assertNull(metaAfterChannel(null, "Bench Notes", null))
    }

    @Test
    fun channelNameOfRecoversRecommendedChannelAndIgnoresChannelPageMeta() {
        assertEquals("Veritasium", channelNameOf(video(null, "Veritasium • 16.1M views • 9 hours ago")))
        assertEquals("Veritasium", channelNameOf(video("4K", "Veritasium • 16.1M views • 9 hours ago")))
        assertNull(channelNameOf(video(null, "16.1M views • 9 hours ago")))
    }

    @Test
    fun monogramIsTheFirstLetterOrDigit() {
        assertEquals("B", monogramOf("bench notes"))
        assertEquals("3", monogramOf("  3Blue1Brown"))
        assertEquals("•", monogramOf(""))
        assertEquals("•", monogramOf(null))
    }
}
