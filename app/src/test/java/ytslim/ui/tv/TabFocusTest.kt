package ytslim.ui.tv

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.Video
import ytslim.ui.HomeTab
import ytslim.ui.VideoListContent

/**
 * Subir com o controle até as abas de cima para na aba da página aberta, e não na aba que estiver
 * mais perto do card (achado do Johan na TV, 2026-11-04: das Inscrições ia para os Ajustes).
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class TabFocusTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val videos = (1..12).map { i ->
        Video(
            id = "v$i", title = "Video number $i", channelName = "Channel", channelId = "c$i", thumbnailUrl = null,
            info = null, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
            isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = i,
        )
    }

    private fun grid() = VideoListContent(videos, hasMore = false, loadingMore = false)

    /** O foco no primeiro card da grade, como quando a página abre. */
    private fun focusFirstCard() {
        val focusable = SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab).not()
        val cards = compose.onAllNodes(focusable).fetchSemanticsNodes()
        val first = cards.indexOf(cards.minWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left })))
        compose.onAllNodes(focusable)[first].requestFocus()
        compose.waitForIdle()
    }

    private fun press(keyCode: Int, times: Int = 1) = repeat(times) {
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        compose.waitForIdle()
    }

    @Test
    fun goingUpFromTheGridStopsOnTheOpenTab() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        compose.setContent {
            TvUi.Theme {
                TvUi.Home(tab, onTab = { tab = it }, onReload = {}) {
                    TvUi.VideoGrid(grid(), "", {}, {}, null, takeFocus = true, onFocusTaken = {})
                }
            }
        }
        compose.waitForIdle()
        focusFirstCard()
        // O último card da primeira fileira, embaixo das abas da direita (History, Settings).
        press(KeyEvent.KEYCODE_DPAD_RIGHT, times = 3)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_UP, times = 4)
        assertEquals(HomeTab.SUBSCRIPTIONS, tab)
        compose.onNodeWithText("Subscriptions").assertIsFocused()
    }

    @Test
    fun alongTheTabsTheRemoteStillChangesThePage() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        compose.setContent {
            TvUi.Theme {
                TvUi.Home(tab, onTab = { tab = it }, onReload = {}) {
                    TvUi.VideoGrid(grid(), "", {}, {}, null, takeFocus = true, onFocusTaken = {})
                }
            }
        }
        compose.waitForIdle()
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_UP)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertEquals(HomeTab.RECOMMENDED, tab)
        // E descer e subir de novo volta para a aba nova.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_RIGHT, times = 3)
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertEquals(HomeTab.RECOMMENDED, tab)
        compose.onNodeWithText("Recommended").assertIsFocused()
    }

    @Test
    fun goingUpFromTheGridStopsOnTheChannelTabThatIsOpen() {
        var tab by mutableStateOf(0)
        compose.setContent {
            TvUi.Theme {
                androidx.compose.foundation.layout.Column {
                    TvUi.ChannelTabs(tab, onTab = { tab = it })
                    TvUi.VideoGrid(grid(), "", {}, {}, null, takeFocus = true, onFocusTaken = {})
                }
            }
        }
        compose.waitForIdle()
        focusFirstCard()
        // O segundo card fica embaixo de "Playlists".
        press(KeyEvent.KEYCODE_DPAD_RIGHT, times = 2)
        press(KeyEvent.KEYCODE_DPAD_UP, times = 2)
        assertEquals(0, tab)
        compose.onNodeWithText("Videos").assertIsFocused()
    }
}
