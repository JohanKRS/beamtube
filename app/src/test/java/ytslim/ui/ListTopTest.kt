package ytslim.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.Video

/**
 * A grade quando a lista muda ([ListTop]): parada no começo, ela mostra os vídeos novos que chegam
 * em cima; e volta ao começo quando as Inscrições se atualizam sozinhas depois de um tempo fora
 * (pedido do Johan, 2026-11-13).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ListTopTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun video(id: String) = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "c", thumbnailUrl = null,
        info = null, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
        isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private val old = (1..30).map { video("old$it") }
    private val new = (1..3).map { video("new$it") }

    private fun content(videos: List<Video>, jump: Int = 0) = VideoListContent(videos, hasMore = false, loadingMore = false, jumpToTop = jump)

    private lateinit var grid: LazyGridState

    private fun show(first: VideoListContent, withListTop: Boolean = true): (VideoListContent) -> Unit {
        var content by mutableStateOf(first)
        // A lista chega como parâmetro, como nas telas do app (a grade só a vê depois de recompor).
        compose.setContent { Grid(content, withListTop) }
        compose.waitForIdle()
        return { next ->
            content = next
            compose.waitForIdle()
        }
    }

    @Composable
    private fun Grid(content: VideoListContent, withListTop: Boolean) {
        grid = rememberLazyGridState()
        if (withListTop) ListTop(grid, content)
        LazyVerticalGrid(state = grid, columns = GridCells.Fixed(1), modifier = Modifier.height(400.dp)) {
            items(content.videos, key = { it.id + it.playlistIndex }) { Box(Modifier.height(100.dp)) { Text(it.title) } }
        }
    }

    @Test
    fun withoutItTheGridHoldsTheOldFirstVideoAndHidesTheNewOnes() {
        // Como o Compose faz sozinho: é isso que o ListTop corrige.
        val update = show(content(old), withListTop = false)
        update(content(new + old))
        assertEquals(new.size, grid.firstVisibleItemIndex)
    }

    @Test
    fun atTheTopNewVideosShowUpOnTop() {
        val update = show(content(old))
        update(content(new + old))
        assertEquals(0, grid.firstVisibleItemIndex)
        compose.onNodeWithText("Video new1").assertIsDisplayed()
    }

    @Test
    fun scrolledDownTheListStaysOnTheSameVideo() {
        val update = show(content(old))
        compose.runOnUiThread { runBlocking { grid.scrollToItem(10) } }
        compose.waitForIdle()
        update(content(new + old))
        // O mesmo vídeo de antes no topo, agora 3 posições adiante.
        assertEquals(10 + new.size, grid.firstVisibleItemIndex)
        compose.onNodeWithText("Video old11").assertIsDisplayed()
    }

    @Test
    fun afterTimeAwayTheListGoesBackToTheFirstVideo() {
        val update = show(content(old))
        compose.runOnUiThread { runBlocking { grid.scrollToItem(10) } }
        compose.waitForIdle()
        update(content(new + old, jump = 1))
        assertEquals(0, grid.firstVisibleItemIndex)
        compose.onNodeWithText("Video new1").assertIsDisplayed()
    }
}
