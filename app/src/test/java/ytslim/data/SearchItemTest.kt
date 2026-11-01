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
}
