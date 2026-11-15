package ytslim.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test
import ytslim.data.Video

/**
 * As abas carregadas por trás (1.0.11, pedido do Johan): uma de cada vez, e de cada lista só as
 * miniaturas da primeira tela.
 */
class TabWarmupTest {
    private fun video(id: String, thumbnail: String? = "https://x/$id.jpg") = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "UC1", thumbnailUrl = thumbnail, info = null,
        published = "1 day ago", durationMs = 600_000, isLive = false, isUpcoming = false, isShorts = false,
        percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private fun ready(videos: List<Video>): UiState<VideoListContent> = UiState.Ready(VideoListContent(videos, hasMore = true, loadingMore = false))

    @Test
    fun theNextTabWaitsForTheOneBefore() {
        runBlocking {
            val first = MutableStateFlow<UiState<*>>(UiState.Loading)
            val second = MutableStateFlow<UiState<*>>(UiState.Loading)
            val opened = mutableListOf<String>()
            val job = launch {
                TabWarmup.oneAtATime(
                    listOf<() -> StateFlow<UiState<*>>>({ opened += "first"; first }, { opened += "second"; second }, { opened += "third"; second }),
                    timeoutMs = 60_000,
                )
            }
            repeat(5) { yield() }
            assertEquals(listOf("first"), opened)
            // Um erro também libera a seguinte: sem rede, uma aba não segura as outras.
            first.value = UiState.Error("offline")
            repeat(5) { yield() }
            assertEquals(listOf("first", "second"), opened)
            second.value = UiState.Ready(Unit)
            job.join()
            assertEquals(listOf("first", "second", "third"), opened)
            // Depois da última não espera: uma aba presa no fim não segura o que vem depois.
            TabWarmup.oneAtATime(listOf<() -> StateFlow<UiState<*>>>({ MutableStateFlow(UiState.Loading) }), timeoutMs = 60_000)
        }
    }

    @Test
    fun aTabThatTakesTooLongDoesNotHoldTheOthers() {
        runBlocking {
            val stuck = MutableStateFlow<UiState<*>>(UiState.Loading)
            val opened = mutableListOf<String>()
            TabWarmup.oneAtATime(listOf<() -> StateFlow<UiState<*>>>({ opened += "stuck"; stuck }, { opened += "next"; stuck }), timeoutMs = 20)
            assertEquals(listOf("stuck", "next"), opened)
        }
    }

    @Test
    fun onlyTheFirstScreenIsFetchedAhead() {
        runBlocking {
            val saved = (1..30).map { video("s$it") }
            val fresh = listOf(video("f1"), video("f2", thumbnail = null)) + saved
            val states = listOf(
                UiState.Loading,
                ready(saved),
                // Mais uma página no fim: a primeira tela é a mesma, nada de novo a baixar.
                ready(saved + video("more")),
                ready(fresh),
            )
            val screens = TabWarmup.firstScreen(kotlinx.coroutines.flow.flowOf(*states.toTypedArray())).toList()
            assertEquals(3, screens.size)
            assertEquals(emptyList<String>(), screens[0])
            assertEquals((1..TabWarmup.FIRST_SCREEN).map { "https://x/s$it.jpg" }, screens[1])
            assertEquals(listOf("https://x/f1.jpg") + (1..6).map { "https://x/s$it.jpg" }, screens[2])
        }
    }
}
