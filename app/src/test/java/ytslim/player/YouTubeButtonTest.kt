package ytslim.player

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** O app aprende o botão YouTube do controle: a tecla de app logo antes de o YouTube abrir. */
class YouTubeButtonTest {
    private val youTubeKey = 4065 // a da TCL, segundo relatos; cada controle tem a sua

    @Test
    fun learnsTheKeyPressedRightBeforeYouTubeOpens() {
        val button = YouTubeButton()
        assertFalse(button.isButton(youTubeKey))
        button.onKeyPassed(youTubeKey, now = 1_000)
        assertEquals(youTubeKey, button.onYouTubeShown(now = 2_500))
        assertTrue(button.isButton(youTubeKey))
        assertEquals(setOf(youTubeKey), button.learned)
    }

    @Test
    fun learnsOnlyOncePerPress() {
        val button = YouTubeButton()
        button.onKeyPassed(youTubeKey, now = 1_000)
        assertEquals(youTubeKey, button.onYouTubeShown(now = 1_500))
        // O YouTube abre mais de uma janela ao começar: só a primeira conta.
        assertNull(button.onYouTubeShown(now = 1_800))
    }

    @Test
    fun ignoresYouTubeOpeningLongAfterTheKey() {
        val button = YouTubeButton()
        button.onKeyPassed(youTubeKey, now = 1_000)
        assertNull(button.onYouTubeShown(now = 1_000 + YouTubeButton.LEARN_WINDOW_MS + 1))
        assertFalse(button.isButton(youTubeKey))
    }

    @Test
    fun aKeyInBetweenMeansTheUserOpenedYouTubeSomeOtherWay() {
        val button = YouTubeButton()
        // Uma tecla de app (Netflix, por exemplo) e depois o YouTube aberto pelo launcher com OK.
        button.onKeyPassed(youTubeKey, now = 1_000)
        button.onKeyPassed(KeyEvent.KEYCODE_DPAD_CENTER, now = 1_400)
        assertNull(button.onYouTubeShown(now = 2_000))
    }

    @Test
    fun neverLearnsOrdinaryKeys() {
        val button = YouTubeButton()
        for (key in listOf(
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_Y,
        )) {
            button.onKeyPassed(key, now = 1_000)
            assertNull("tecla $key", button.onYouTubeShown(now = 1_200))
        }
        assertTrue(button.learned.isEmpty())
    }

    @Test
    fun keepsWhatWasLearnedAndAddsANewRemote() {
        val button = YouTubeButton(learned = setOf(293))
        assertTrue(button.isButton(293))
        button.onKeyPassed(youTubeKey, now = 1_000)
        button.onYouTubeShown(now = 1_100)
        assertEquals(setOf(293, youTubeKey), button.learned)
    }

    @Test
    fun youTubeWithoutAnyKeyIsNotLearned() {
        // Um vídeo mandado do celular para a TV abre o YouTube sem tecla nenhuma.
        assertNull(YouTubeButton().onYouTubeShown(now = 5_000))
    }
}
