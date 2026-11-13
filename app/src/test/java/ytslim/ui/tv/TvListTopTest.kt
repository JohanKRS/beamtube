package ytslim.ui.tv

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.Video
import ytslim.ui.HomeTab
import ytslim.ui.VideoListContent

/**
 * Na TV, as Inscrições atualizadas com vídeos novos em cima: a grade mostra os novos, e o foco que
 * estava num card vai para o primeiro deles (pedido do Johan, 2026-11-13: ao religar a TV, os vídeos
 * novos não apareciam).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class TvListTopTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun video(id: String) = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "c", thumbnailUrl = null,
        info = null, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
        isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private val old = (1..24).map { video("old$it") }
    private val new = (1..5).map { video("new$it") }

    private var content by mutableStateOf(VideoListContent(old, hasMore = false, loadingMore = false))

    private fun open() {
        compose.setContent {
            TvUi.Theme {
                TvUi.Home(HomeTab.SUBSCRIPTIONS, onTab = {}, onReload = {}) {
                    TvUi.VideoGrid(content, "", {}, {}, null, takeFocus = false, onFocusTaken = {})
                }
            }
        }
        compose.waitForIdle()
    }

    /** O foco no primeiro card da grade, como quando a página abre. */
    private fun focusFirstCard() {
        val card = SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab).not()
        val cards = compose.onAllNodes(card).fetchSemanticsNodes()
        val first = cards.indexOf(cards.minWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left })))
        compose.onAllNodes(card)[first].requestFocus()
        compose.waitForIdle()
    }

    private fun press(keyCode: Int, times: Int = 1) = repeat(times) {
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        compose.waitForIdle()
    }

    /** O card focado é o do vídeo [title]: o texto fica logo abaixo dele, na mesma coluna. */
    private fun assertFocusedCardIs(title: String) {
        val focused = compose.onNode(isFocused()).fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
        check(text.top >= focused.bottom - 1 && text.left in (focused.left - 8)..(focused.left + 8)) {
            "the focused card ($focused) isn't the one above \"$title\" ($text)"
        }
    }

    @Test
    fun newVideosShowUpOnTopAndTakeTheFocus() {
        open()
        focusFirstCard()
        content = VideoListContent(new + old, hasMore = false, loadingMore = false)
        compose.waitForIdle()
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
    }

    @Test
    fun afterTimeAwayTheGridGoesBackToTheFirstVideo() {
        open()
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_DOWN, times = 4)
        content = VideoListContent(new + old, hasMore = false, loadingMore = false, jumpToTop = 1)
        compose.waitForIdle()
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
    }
}
