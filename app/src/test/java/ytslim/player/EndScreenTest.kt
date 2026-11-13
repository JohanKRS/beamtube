package ytslim.player

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ytslim.data.UpNext
import ytslim.data.Video

/**
 * A tela do fim do vídeo ([EndScreen], 1.0, pedido do Johan): volta sozinha depois de 15 s sem
 * ninguém mexer, cada toque recomeça a contagem, e com "Play a suggestion" a primeira toca antes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-land-television-xhdpi")
class EndScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun video(id: String) = Video(
        id = id, title = "Video $id", channelName = "Channel", channelId = "c", thumbnailUrl = null,
        info = null, published = null, durationMs = 600_000, isLive = false, isUpcoming = false,
        isShorts = false, percentWatched = 0, playlistId = null, playlistIndex = -1,
    )

    private var backs = 0
    private val played = mutableListOf<String>()

    private fun open(playNext: Boolean): EndScreen {
        val end = EndScreen(compose.activity, tv = true, onPlay = { played += it.id }, onBack = { backs++ })
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { end.show(listOf(video("a"), video("b")), playNext) }
        compose.setContent { end.Content() }
        compose.mainClock.advanceTimeByFrame()
        return end
    }

    private fun touch(end: EndScreen) {
        compose.runOnUiThread {
            end.touched()
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun goesBackByItselfAfterTheWait() {
        open(playNext = false)
        compose.onNodeWithText("Going back in 15 s").assertExists()
        compose.mainClock.advanceTimeBy(UpNext.BACK_AFTER_MS - 1_000)
        assertEquals(0, backs)
        compose.mainClock.advanceTimeBy(2_000)
        assertEquals(1, backs)
        assertTrue(played.isEmpty())
    }

    @Test
    fun aTouchStartsTheWaitAgain() {
        val end = open(playNext = false)
        compose.mainClock.advanceTimeBy(10_000)
        touch(end)
        compose.mainClock.advanceTimeBy(10_000)
        assertEquals(0, backs)
        compose.mainClock.advanceTimeBy(6_000)
        assertEquals(1, backs)
    }

    @Test
    fun playASuggestionPlaysTheFirstUnlessTouched() {
        open(playNext = true)
        compose.onNodeWithText("Next video in 8 s").assertExists()
        compose.mainClock.advanceTimeBy(UpNext.NEXT_AFTER_MS + 1_000)
        assertEquals(listOf("a"), played)
        assertEquals(0, backs)
    }

    @Test
    fun theFirstTouchSwitchesToGoingBack() {
        val end = open(playNext = true)
        touch(end)
        compose.onNodeWithText("Going back in 15 s").assertExists()
        compose.mainClock.advanceTimeBy(UpNext.BACK_AFTER_MS + 1_000)
        assertTrue(played.isEmpty())
        assertEquals(1, backs)
    }

    @Test
    fun aSuggestionOrBackCanBeChosen() {
        open(playNext = false)
        compose.onNodeWithText("Video b").performClick()
        assertEquals(listOf("b"), played)
        compose.onNodeWithText("Back").performClick()
        assertEquals(1, backs)
    }
}
