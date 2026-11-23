package ytslim.data

import com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SearchItemTest {
    private fun item(videoId: String? = null, channelId: String? = null, playlistId: String? = null) = YouTubeMediaItem().apply {
        setVideoId(videoId)
        setChannelId(channelId)
        setPlaylistId(playlistId)
        setTitle("Title")
        setCardImageUrl("//yt3.ggpht.com/photo")
    }

    @Test
    fun videosChannelsAndPlaylistsAreTellApart() {
        val video = item(videoId = "abc", channelId = "UCx").toSearchItem()
        assertTrue(video is SearchItem.VideoItem)
        assertEquals("abc", (video as SearchItem.VideoItem).video.id)

        val channel = item(channelId = "UCchannel").toSearchItem()
        assertTrue(channel is SearchItem.ChannelItem)
        assertEquals("UCchannel", (channel as SearchItem.ChannelItem).channel.id)
        assertEquals("https://yt3.ggpht.com/photo", channel.channel.thumbnailUrl)

        val playlist = item(channelId = "VLPL123").toSearchItem()
        assertTrue(playlist is SearchItem.PlaylistItem)
        assertEquals("PL123", (playlist as SearchItem.PlaylistItem).playlist.playlistId)

        // A busca manda a playlist junto com o primeiro vídeo dela.
        val withFirstVideo = item(videoId = "fNk_zzaMoSs", playlistId = "PLZHQ").apply { setBadgeText("16 lessons") }.toSearchItem()
        assertTrue(withFirstVideo is SearchItem.PlaylistItem)
        assertEquals("VLPLZHQ", (withFirstVideo as SearchItem.PlaylistItem).playlist.browseId)
        assertEquals("16 lessons", withFirstVideo.playlist.info)

        assertNull(item().toSearchItem())
    }

    @Test
    fun toVideoRecoversChannelNameFromInfoWhenAuthorIsMissingOrBadge() {
        val noAuthor = item(videoId = "rec1").apply {
            setAuthor(null)
            setSecondTitle("\u200EVeritasium\u200E • \u200E16.1M views\u200E • \u200E9 hours ago\u200E")
        }.toVideo(home = true)!!
        assertEquals("Veritasium", noAuthor.channelName)
        assertEquals("Veritasium • 16.1M views • 9 hours ago", noAuthor.info)

        val badgeAuthor = item(videoId = "rec2").apply {
            setAuthor("4K")
            setSecondTitle("3Blue1Brown • 500K views • 1 day ago")
        }.toVideo(home = true)!!
        assertEquals("3Blue1Brown", badgeAuthor.channelName)
    }
}
