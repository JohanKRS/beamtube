package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ytslim.data.ContentFilters
import ytslim.data.TopicRules
import ytslim.data.Video

/**
 * A lista nova entra logo abaixo do que a pessoa já viu, e ao sair da aba fica só ela (1.0.11,
 * ideia do Johan). As páginas passam pelo [TopicRules.recommend], como nas Recomendações.
 */
class FreshBelowTest {
    private fun video(id: String) = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "UC1", thumbnailUrl = null, info = null,
        published = "1 day ago", durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private val saved = (1..20).map { video("old$it") }

    // A lista da conta repete dois vídeos da guardada: um que estava à vista e um de mais abaixo.
    private val fresh = listOf(video("new1"), video("old3"), video("new2"), video("old15"), video("new3"))

    private fun screen(pages: List<List<Video>>) = TopicRules.recommend(pages, ContentFilters()).map { it.id }

    @Test
    fun theSeenRowsStayAndTheNewListComesRightBelow() {
        val pages = listOf(FreshBelow.keep(saved, seen = 8)) + listOf(fresh)
        // As oito vistas no mesmo lugar; abaixo, a lista nova, sem repetir a que já está à vista.
        assertEquals((1..8).map { "old$it" } + listOf("new1", "new2", "old15", "new3"), screen(pages))
    }

    @Test
    fun leavingTheTabLeavesOnlyTheNewList() {
        val pages = listOf(FreshBelow.keep(saved, seen = 8)) + listOf(fresh)
        assertEquals(listOf("new1", "old3", "new2", "old15", "new3"), screen(pages.drop(1)))
    }

    @Test
    fun withoutKnowingWhatWasSeenTheFirstRowsStay() {
        assertEquals((1..FreshBelow.FIRST_ROWS).map { "old$it" }, FreshBelow.keep(saved, seen = 0).map { it.id })
        // Uma lista guardada curta fica inteira.
        assertEquals(listOf("old1", "old2"), FreshBelow.keep(saved.take(2), seen = 8).map { it.id })
    }
}
