package ytslim.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** O mini player flutuante (pedido do Johan, 2026-11-11): só com o vídeo tocando, no celular e no tablet. */
class MiniPlayerRulesTest {
    @Test
    fun leavingWhileTheVideoPlaysFloats() {
        assertTrue(MiniPlayerRules.floats(touch = true, playWhenReady = true, playbackState = Player.STATE_READY, finishing = false))
        // Carregando para tocar também.
        assertTrue(MiniPlayerRules.floats(touch = true, playWhenReady = true, playbackState = Player.STATE_BUFFERING, finishing = false))
    }

    @Test
    fun aPausedOrFinishedVideoDoesNotFloat() {
        assertFalse(MiniPlayerRules.floats(touch = true, playWhenReady = false, playbackState = Player.STATE_READY, finishing = false))
        assertFalse(MiniPlayerRules.floats(touch = true, playWhenReady = true, playbackState = Player.STATE_ENDED, finishing = false))
        // Com erro (o player para).
        assertFalse(MiniPlayerRules.floats(touch = true, playWhenReady = true, playbackState = Player.STATE_IDLE, finishing = false))
    }

    @Test
    fun closingThePlayerDoesNotFloat() {
        assertFalse(MiniPlayerRules.floats(touch = true, playWhenReady = true, playbackState = Player.STATE_READY, finishing = true))
    }

    @Test
    fun notOnTheTv() {
        assertFalse(MiniPlayerRules.floats(touch = false, playWhenReady = true, playbackState = Player.STATE_READY, finishing = false))
    }

    @Test
    fun theWindowHasTheShapeOfTheVideo() {
        assertEquals(1920 to 1080, MiniPlayerRules.shape(1920, 1080, 1f))
        // Vertical.
        assertEquals(1080 to 1920, MiniPlayerRules.shape(1080, 1920, 1f))
        // Pixels não quadrados: 1440x1080 mostrado em 16:9.
        assertEquals(1920 to 1080, MiniPlayerRules.shape(1440, 1080, 4f / 3f))
    }

    @Test
    fun beforeTheVideoArrivesItIsWidescreen() {
        assertEquals(16 to 9, MiniPlayerRules.shape(0, 0, 1f))
    }

    @Test
    fun tooWideOrTooTallStaysInsideWhatAndroidAccepts() {
        assertEquals(239 to 100, MiniPlayerRules.shape(3000, 1000, 1f))
        assertEquals(100 to 239, MiniPlayerRules.shape(1000, 3000, 1f))
    }
}
