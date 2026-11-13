package ytslim.data

import com.liskovsoft.mediaserviceinterfaces.data.FeedbackEndpoint
import com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelFeedbackTest {
    private fun video(id: String, channel: String?, channelId: String?, request: NotRecommend? = null) = Video(
        id = id, title = id, channelName = channel, channelId = channelId, thumbnailUrl = null, info = null,
        published = null, durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = -1, notRecommend = request,
    )

    @Test
    fun findsTheRequestByChannelIdOrName() {
        val registry = ChannelFeedback.Registry()
        val byId = NotRecommend.Token("by-id")
        // Os cards do Início costumam vir sem o ID do canal: só o nome (às vezes com o @).
        val byName = NotRecommend.Panel("panel", "params")
        assertTrue(registry.note(listOf(video("v1", "Veritasium", "UCHnyfMqiRRG1u-2MsSQLbXA", byId), video("v2", "Kurzgesagt • @kurzgesagt", null, byName))))

        assertEquals(byId, registry.find(BlockedChannel("UCHnyfMqiRRG1u-2MsSQLbXA", "Veritasium")))
        assertEquals(byName, registry.find(BlockedChannel("UCsXVk37bltHxD1rDPwtNM8Q", "Kurzgesagt")))
        // O nome compara como no bloqueio: sem maiúsculas nem acentos.
        assertEquals(byName, registry.find(BlockedChannel("UCother", "KURZGESAGT")))
        assertNull(registry.find(BlockedChannel("UCnone", "Someone else")))
    }

    @Test
    fun pagesWithoutRequestsChangeNothing() {
        val registry = ChannelFeedback.Registry()
        assertFalse(registry.note(listOf(video("v1", "Veritasium", "UC1"))))
        assertNull(registry.find(BlockedChannel("UC1", "Veritasium")))
    }

    @Test
    fun readsWhetherYouTubeAcceptedIt() {
        assertTrue(FeedbackResponse.processed(Json.parseToJsonElement("""{"responseContext":{},"feedbackResponses":[{"isProcessed":true}]}""")))
        // Um token inválido volta com 200 e isProcessed false (conferido no YouTube em 2026-11-13).
        assertFalse(FeedbackResponse.processed(Json.parseToJsonElement("""{"feedbackResponses":[{"isProcessed":false}]}""")))
        assertFalse(FeedbackResponse.processed(Json.parseToJsonElement("""{"responseContext":{}}""")))
    }

    @Test
    fun onlyHomeCardsCarryTheRequest() {
        // Como no SmartTube: o segundo pedido do menu é o "Don't recommend channel"; com o painel, o painel.
        val inline = object : YouTubeMediaItem() {
            override fun getFeedbackToken2() = "dont-recommend"
        }.apply { setVideoId("v1") }
        assertEquals(NotRecommend.Token("dont-recommend"), inline.toVideo(home = true)?.notRecommend)
        assertNull(inline.toVideo()?.notRecommend)

        val panel = object : YouTubeMediaItem() {
            override fun getFeedbackToken2() = null
            override fun getFeedbackEndpoint() = object : FeedbackEndpoint {
                override fun getPanelId() = "PAfeedback"
                override fun getParams() = "abc"
            }
        }.apply { setVideoId("v2") }
        assertEquals(NotRecommend.Panel("PAfeedback", "abc"), panel.toVideo(home = true)?.notRecommend)
    }

    @Test
    fun theRequestIsNotSavedAndOldBlockListsStillLoad() {
        val json = Json { ignoreUnknownKeys = true }
        val saved = json.encodeToString(Video.serializer(), video("v1", "Veritasium", "UC1", NotRecommend.Token("t")))
        assertFalse(saved.contains("notRecommend"))
        assertNull(json.decodeFromString(Video.serializer(), saved).notRecommend)

        // Listas guardadas antes desta versão não têm o onAccount.
        val old = json.decodeFromString(ListSerializer(BlockedChannel.serializer()), """[{"id":"UC1","name":"Veritasium"}]""")
        assertEquals(listOf(BlockedChannel("UC1", "Veritasium", onAccount = false)), old)
    }
}
