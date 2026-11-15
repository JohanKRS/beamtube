package ytslim.ui.tv

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.Video
import ytslim.ui.HomeTab
import ytslim.ui.VideoListContent

/**
 * O menu lateral da TV (pedido do Johan, 2026-11-14): entrar nele pela esquerda para na aba aberta,
 * subir ou descer da grade não cai nele, andar por ele troca a página, e trocar o layout nos Ajustes
 * não tira o foco da linha escolhida.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class SideMenuFocusTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val videos = (1..12).map { i ->
        Video(
            id = "v$i", title = "Video number $i", channelName = "Channel", channelId = "c$i", thumbnailUrl = null,
            info = null, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
            isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = i,
        )
    }

    private fun grid() = VideoListContent(videos, hasMore = false, loadingMore = false)

    @Before
    fun sideMenu() = TvLayouts.select(TvLayout.SIDE_MENU)

    @After
    fun topTabs() = TvLayouts.select(TvLayout.TOP_TABS)

    /** O que pode ter o foco fora do menu lateral (os cards). */
    private fun cards(): List<androidx.compose.ui.semantics.SemanticsNode> {
        val menuEdge = with(compose.density) { 88.dp.toPx() }
        return compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused)).fetchSemanticsNodes()
            .filter { it.boundsInRoot.left > menuEdge }
    }

    /** O foco no primeiro card da grade, como quando a página abre. */
    private fun focusFirstCard() {
        val first = cards().minWith(compareBy({ it.boundsInRoot.top }, { it.boundsInRoot.left }))
        compose.onNode(SemanticsMatcher("id ${first.id}") { it.id == first.id }).requestFocus()
        compose.waitForIdle()
    }

    private fun focusedIsACard(): Boolean {
        val focused = compose.onAllNodes(isFocused()).fetchSemanticsNodes()
        return focused.size == 1 && focused[0].id in cards().map { it.id }
    }

    private fun press(keyCode: Int, times: Int = 1) = repeat(times) {
        compose.runOnUiThread {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
        compose.waitForIdle()
    }

    private fun home(tab: () -> HomeTab, onTab: (HomeTab) -> Unit, onReload: (HomeTab) -> Unit = {}) {
        compose.setContent {
            TvUi.Theme {
                TvUi.Home(tab(), onTab = onTab, onReload = onReload) {
                    TvUi.VideoGrid(grid(), "", {}, {}, null, takeFocus = true, onFocusTaken = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun leftFromTheGridStopsOnTheOpenTab() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        home({ tab }, { tab = it })
        focusFirstCard()
        // A segunda fileira fica ao lado de Recommended e Explore no menu.
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(HomeTab.SUBSCRIPTIONS, tab)
        compose.onNodeWithContentDescription("Subscriptions").assertIsFocused()
    }

    @Test
    fun upFromTheFirstRowStaysOnTheGrid() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        home({ tab }, { tab = it })
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_UP)
        assertEquals(HomeTab.SUBSCRIPTIONS, tab)
        assertTrue(focusedIsACard())
        compose.onNodeWithContentDescription("Subscriptions").assertIsNotFocused()
    }

    @Test
    fun alongTheMenuTheRemoteChangesThePage() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        home({ tab }, { tab = it })
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        assertEquals(HomeTab.RECOMMENDED, tab)
        press(KeyEvent.KEYCODE_DPAD_DOWN, times = 2)
        assertEquals(HomeTab.SEARCH, tab)
        // Os Ajustes ficam no pé do menu, depois do Histórico.
        press(KeyEvent.KEYCODE_DPAD_DOWN, times = 3)
        assertEquals(HomeTab.SETTINGS, tab)
        compose.onNodeWithContentDescription("Settings").assertIsFocused()
        // Para a direita volta para o conteúdo, e para a esquerda de novo para na aba aberta.
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue(focusedIsACard())
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(HomeTab.SETTINGS, tab)
        compose.onNodeWithContentDescription("Settings").assertIsFocused()
    }

    @Test
    fun okOnTheOpenTabReloadsIt() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        val reloads = mutableListOf<HomeTab>()
        home({ tab }, { tab = it }, onReload = { reloads += it })
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(listOf(HomeTab.SUBSCRIPTIONS), reloads)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_ENTER)
        assertEquals(listOf(HomeTab.SUBSCRIPTIONS, HomeTab.RECOMMENDED), reloads)
        compose.onNodeWithContentDescription("Recommended").assertIsFocused()
    }

    @Test
    fun backFromTheGridGoesToTheMenu() {
        var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)
        home({ tab }, { tab = it })
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_RIGHT, times = 2)
        press(KeyEvent.KEYCODE_BACK)
        assertEquals(HomeTab.SUBSCRIPTIONS, tab)
        compose.onNodeWithContentDescription("Subscriptions").assertIsFocused()
    }

    /** Trocar o layout nos Ajustes muda a moldura na hora, e o foco fica na linha que foi escolhida. */
    @Test
    fun switchingTheLayoutKeepsTheFocusedRow() {
        TvLayouts.select(TvLayout.TOP_TABS)
        compose.setContent {
            TvUi.Theme {
                TvUi.Home(HomeTab.SETTINGS, onTab = {}, onReload = {}) {
                    Column {
                        TvLayout.entries.forEach { layout ->
                            Button(onClick = { TvLayouts.select(layout) }) { Text(layout.name) }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("SIDE_MENU").requestFocus()
        compose.waitForIdle()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(TvLayout.SIDE_MENU, TvLayouts.current)
        compose.onNodeWithContentDescription("Settings").assertExists()
        compose.onNodeWithText("SIDE_MENU").assertIsFocused()
        // E de volta.
        press(KeyEvent.KEYCODE_DPAD_UP)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(TvLayout.TOP_TABS, TvLayouts.current)
        compose.onNodeWithText("TOP_TABS").assertIsFocused()
    }
}
