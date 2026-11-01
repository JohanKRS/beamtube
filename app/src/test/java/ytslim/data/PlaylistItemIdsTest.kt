package ytslim.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistItemIdsTest {
    private fun parse(json: String) = PlaylistItemIds.parse(Json.parseToJsonElement(json))

    @Test
    fun readsTvTilesWithTheSetVideoIdInTheMenu() {
        val page = parse(
            """
            {"contents":{"tvBrowseRenderer":{"content":{"tvSurfaceContentRenderer":{"content":{"twoColumnRenderer":{
              "rightColumn":{"playlistVideoListRenderer":{"contents":[
                {"tileRenderer":{"contentId":"vid1","onSelectCommand":{"watchEndpoint":{"videoId":"vid1"}},
                  "menu":{"menuRenderer":{"items":[{"menuServiceItemRenderer":{"serviceEndpoint":{
                    "playlistEditEndpoint":{"actions":[{"action":"ACTION_REMOVE_VIDEO","setVideoId":"SET1"}]}}}}]}}}},
                {"tileRenderer":{"contentId":"vid2","menu":{"menuRenderer":{"items":[{"menuServiceItemRenderer":{
                  "serviceEndpoint":{"playlistEditEndpoint":{"actions":[{"setVideoId":"SET2"}]}}}}]}}}},
                {"tileRenderer":{"contentId":"noSet"}}
              ],
              "continuations":[{"nextContinuationData":{"continuation":"TOKEN_TV"}}]}}}}}}}}}
            """,
        )
        assertEquals(mapOf("vid1" to "SET1", "vid2" to "SET2"), page.setVideoIds)
        assertEquals("TOKEN_TV", page.continuation)
    }

    @Test
    fun readsWebPlaylistRenderersAndTheContinuationCommand() {
        val page = parse(
            """
            {"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[
              {"playlistVideoRenderer":{"videoId":"a","setVideoId":"SA","title":{"runs":[{"text":"A"}]}}},
              {"playlistVideoRenderer":{"videoId":"b","setVideoId":"SB"}},
              {"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"NEXT"}}}}
            ]}}]}
            """,
        )
        assertEquals(mapOf("a" to "SA", "b" to "SB"), page.setVideoIds)
        assertEquals("NEXT", page.continuation)
    }

    @Test
    fun aPublicPlaylistWithoutPositionsGivesNothing() {
        val page = parse("""{"contents":[{"playlistVideoRenderer":{"videoId":"a"}}]}""")
        assertEquals(emptyMap<String, String>(), page.setVideoIds)
        assertNull(page.continuation)
    }
}
