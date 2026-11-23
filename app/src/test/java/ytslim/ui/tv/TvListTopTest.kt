package ytslim.ui.tv

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import org.junit.After
import org.junit.Assert.assertTrue
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

    @After
    fun resetLayout() = TvLayouts.select(TvLayout.TOP_TABS)

    private fun video(id: String, channelName: String? = "Channel", info: String? = null) = Video(
        id = id, title = "Video $id", channelName = channelName, channelId = "c", thumbnailUrl = null,
        info = info, published = "1 day ago", durationMs = 60_000, isLive = false, isUpcoming = false,
        isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = 0,
    )

    private val old = (1..32).map { video("old$it") }
    private val new4 = (1..4).map { video("new$it") }
    private val new = (1..8).map { video("new$it") }
    private val fresh = (1..24).map { video("fresh$it") }

    private var content by mutableStateOf(VideoListContent(old, hasMore = false, loadingMore = false))
    private var tab by mutableStateOf(HomeTab.SUBSCRIPTIONS)

    private fun open(sideMenu: Boolean = false, saveTabs: Boolean = false) {
        TvLayouts.select(if (sideMenu) TvLayout.SIDE_MENU else TvLayout.TOP_TABS)
        compose.setContent {
            TvUi.Theme {
                val tabStates = rememberSaveableStateHolder()
                TvUi.Home(tab, onTab = { tab = it }, onReload = {}) { current ->
                    if (saveTabs) {
                        tabStates.SaveableStateProvider(current.name) {
                            if (current == HomeTab.SUBSCRIPTIONS) {
                                TvUi.VideoGrid(content, "", {}, {}, null, takeFocus = false, onFocusTaken = {})
                            } else {
                                TvUi.VideoGrid(VideoListContent(fresh, hasMore = false, loadingMore = false), "", {}, {}, null, takeFocus = false, onFocusTaken = {})
                            }
                        }
                    } else {
                        TvUi.VideoGrid(content, "", {}, {}, null, takeFocus = false, onFocusTaken = {})
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    /** O foco no primeiro card da grade, como quando a página abre. */
    private fun focusFirstCard(sideMenu: Boolean = false) {
        val card = SemanticsMatcher.keyIsDefined(SemanticsProperties.Focused) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab).not() and
            // A foto do usuário, no fim da linha das abas, não é um card.
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Switch user")).not() and
            SemanticsMatcher("outside side menu") { !sideMenu || it.boundsInRoot.left > 200f }
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

    /**
     * Com o foco fora da grade (nas abas de cima) quando o refresh chega, o focusRestorer não
     * deve puxar a volta para o card antigo (agora 2 fileiras abaixo): descer cai no primeiro novo.
     */
    @Test
    fun refreshWhileFocusIsOnTopTabsEntersAtTheFirstNewVideo() {
        open(sideMenu = false)
        focusFirstCard()
        val topY = compose.onNodeWithText("Video old1").fetchSemanticsNode().boundsInRoot.top
        press(KeyEvent.KEYCODE_DPAD_UP)
        content = VideoListContent(new + old, hasMore = false, loadingMore = false, jumpToTop = 1)
        compose.waitForIdle()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
        assertTrue(compose.onNodeWithText("Video new1").fetchSemanticsNode().boundsInRoot.top >= topY - 4f)
    }

    /** No menu lateral, mesmo quando o card antigo ainda está visível na fileira 1, voltar cai no primeiro novo. */
    @Test
    fun refreshWhileFocusIsOnSideMenuEntersAtTheFirstNewVideo() {
        open(sideMenu = true)
        focusFirstCard(sideMenu = true)
        val topY = compose.onNodeWithText("Video old1").fetchSemanticsNode().boundsInRoot.top
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        content = VideoListContent(new4 + old, hasMore = false, loadingMore = false, jumpToTop = 1)
        compose.waitForIdle()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
        assertTrue(compose.onNodeWithText("Video new1").fetchSemanticsNode().boundsInRoot.top >= topY - 4f)
    }

    /**
     * Descer e voltar para a primeira fileira pelo D-pad deixa um scrollOffset igual ao GridTop
     * (8 dp). Vídeos novos chegando sem jumpToTop ainda devem abrir no topo.
     */
    @Test
    fun returningToRowZeroStillCountsAsTopWhenNewVideosArrive() {
        open()
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_UP)
        content = VideoListContent(new + old, hasMore = false, loadingMore = false, jumpToTop = 0)
        compose.waitForIdle()
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
    }

    /** Se o card focado sumiu da lista nova, o foco continua na grade (no primeiro vídeo novo). */
    @Test
    fun refreshReplacingFocusedCardKeepsFocusOnFirstNewVideo() {
        open()
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_DOWN, times = 4)
        content = VideoListContent(fresh, hasMore = false, loadingMore = false, jumpToTop = 1)
        compose.waitForIdle()
        compose.onNodeWithText("Video fresh1").assertIsDisplayed()
        assertFocusedCardIs("Video fresh1")
    }

    /**
     * Aba guardada no SaveableStateHolder enquanto jumpToTop muda fora da tela (volta às
     * Inscrições depois do refresh ou volta aos Recomendados depois de tirar as fileiras antigas).
     */
    @Test
    fun offScreenJumpToTopStartsSavedTabBackAtTop() {
        open(saveTabs = true)
        focusFirstCard()
        press(KeyEvent.KEYCODE_DPAD_DOWN, times = 3)
        // Voltar leva às abas a partir da fileira 3; troca para outra aba e atualiza esta fora da tela.
        press(KeyEvent.KEYCODE_BACK)
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        check(tab == HomeTab.RECOMMENDED) { "expected tab to switch to RECOMMENDED, was $tab" }
        content = VideoListContent(new + old, hasMore = false, loadingMore = false, jumpToTop = 1)
        compose.waitForIdle()
        // Volta para a primeira aba e entra na grade: começa em new1, no topo.
        press(KeyEvent.KEYCODE_DPAD_LEFT)
        check(tab == HomeTab.SUBSCRIPTIONS) { "expected tab to switch back to SUBSCRIPTIONS, was $tab" }
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        compose.onNodeWithText("Video new1").assertIsDisplayed()
        assertFocusedCardIs("Video new1")
    }

    /**
     * Nos Recomendados, o motor muitas vezes não entrega author separado (ou traz um selo "4K"),
     * deixando tudo em info: o card na TV ainda mostra o canal na 1ª linha e views · data na 2ª.
     */
    @Test
    fun recommendedCardWithoutAuthorSplitsChannelAndViewsIntoTwoLines() {
        content = VideoListContent(
            listOf(
                video("r1", channelName = null, info = "\u200EVeritasium\u200E • \u200E16.1M views\u200E \u200E•\u200E \u200E9 hours ago\u200E"),
                video("r2", channelName = "4K", info = "3Blue1Brown • 500K views • 2 days ago"),
            ),
            hasMore = false,
            loadingMore = false,
        )
        open()
        val ch1 = compose.onNodeWithText("Veritasium").fetchSemanticsNode().boundsInRoot
        val meta1 = compose.onNodeWithText("16.1M views · 9 hours ago").fetchSemanticsNode().boundsInRoot
        assertTrue("expected views line below channel line: ch=$ch1 meta=$meta1", meta1.top >= ch1.bottom - 1f)

        val ch2 = compose.onNodeWithText("3Blue1Brown").fetchSemanticsNode().boundsInRoot
        val meta2 = compose.onNodeWithText("500K views · 2 days ago").fetchSemanticsNode().boundsInRoot
        assertTrue("expected views line below channel line: ch=$ch2 meta=$meta2", meta2.top >= ch2.bottom - 1f)
    }
}
