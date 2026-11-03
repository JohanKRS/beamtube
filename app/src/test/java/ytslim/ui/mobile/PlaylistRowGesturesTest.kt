package ytslim.ui.mobile

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.Video
import ytslim.ui.VideoListContent

/**
 * Os gestos numa playlist do usuário, sem ícones na linha (pedido do Johan, 2026-11-03): tocar
 * abre, segurar e arrastar muda a ordem, deslizar para a direita leva ao topo e para a esquerda tira.
 */
@RunWith(RobolectricTestRunner::class)
// Sem qualificador de tela: com o do celular (xxhdpi) o toque do teste não chega à linha.
@Config(sdk = [35])
class PlaylistRowGesturesTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val videos = (1..4).map { i ->
        Video(
            id = "v$i", title = "Video number $i", channelName = "Channel", channelId = "c$i", thumbnailUrl = null,
            info = null, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
            isShorts = false, percentWatched = 0, playlistId = "PL", playlistIndex = i,
        )
    }

    private val played = mutableListOf<String>()
    private val moves = mutableListOf<Pair<Int, Int>>()
    private var drops = 0
    private val toTop = mutableListOf<String>()
    private val removed = mutableListOf<String>()

    private fun show() {
        val list = mutableStateOf(videos)
        compose.setContent {
            MobileUi.Theme {
                MobileUi.VideoRows(
                    content = VideoListContent(list.value, hasMore = false, loadingMore = false),
                    onNearEnd = {},
                    onPlay = { played += it.id },
                    onOpenChannel = {},
                    showSaved = false,
                    header = null,
                    onRemove = { removed += it.id },
                    onMove = { from, to ->
                        moves += from to to
                        list.value = list.value.toMutableList().apply { add(to, removeAt(from)) }
                    },
                    onDrop = { drops++ },
                    onMoveToTop = { toTop += it.id },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun tappingOpensTheVideo() {
        show()
        compose.onNodeWithText("Video number 2").performTouchInput { click() }
        compose.waitForIdle()
        assertEquals(listOf("v2"), played)
    }

    @Test
    fun holdingAndDraggingMovesTheVideoWithoutOpeningIt() {
        show()
        compose.onNodeWithText("Video number 1").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            repeat(20) { moveBy(Offset(0f, height * 0.12f)) }
            up()
        }
        compose.waitForIdle()
        assertTrue("moves: $moves", moves.isNotEmpty())
        assertEquals(0, moves.first().first)
        assertEquals(1, drops)
        assertEquals(emptyList<String>(), played)
    }

    @Test
    fun holdingWithoutMovingDoesNotOpenTheVideo() {
        show()
        compose.onNodeWithText("Video number 3").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            up()
        }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), played)
        assertEquals(emptyList<Pair<Int, Int>>(), moves)
    }

    @Test
    fun swipingRightSendsTheVideoToTheTop() {
        show()
        compose.onNodeWithText("Video number 3").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(listOf("v3"), toTop)
        assertEquals(emptyList<String>(), removed)
        assertEquals(emptyList<String>(), played)
    }

    @Test
    fun theFirstVideoIsAlreadyOnTop() {
        show()
        compose.onNodeWithText("Video number 1").performTouchInput { swipeRight() }
        compose.waitForIdle()
        assertEquals(emptyList<String>(), toTop)
    }

    @Test
    fun swipingLeftStillRemoves() {
        show()
        compose.onNodeWithText("Video number 2").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals(listOf("v2"), removed)
        assertEquals(emptyList<String>(), toTop)
    }
}
