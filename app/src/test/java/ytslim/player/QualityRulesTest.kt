package ytslim.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityRulesTest {
    private fun track(
        height: Int,
        fps: Float = 30f,
        bitrate: Int = height * 1000,
        handled: Boolean = true,
        exceeds: Boolean = false,
        hardware: Boolean = true,
        hdr: Boolean = false,
        selected: Boolean = false,
        group: Int = 0,
    ) = VideoTrack(group, height, height, fps, bitrate, handled, exceeds, hardware, hdr, selected)

    private val tv = listOf(
        // A TV de 32 bits: o decodificador de hardware diz que não dá conta de nada acima de 360p.
        track(2160, handled = false, exceeds = true, group = 1),
        track(1440, handled = false, exceeds = true, group = 1),
        track(1080, handled = false, exceeds = true),
        track(720, handled = false, exceeds = true),
        track(360, selected = true),
    )

    @Test
    fun triesFourKFirstEvenPastTheDeclaredLimits() {
        assertEquals(2160, QualityRules.best(tv, QualityRules.MAX_HEIGHT)?.height)
    }

    @Test
    fun stepsDownOneLevelAtATime() {
        var ceiling = QualityRules.MAX_HEIGHT
        val played = mutableListOf<Int>()
        repeat(4) {
            val height = QualityRules.best(tv, ceiling)!!.height
            played += height
            ceiling = QualityRules.lowerCeiling(height)
        }
        assertEquals(listOf(2160, 1440, 1080, 720), played)
    }

    @Test
    fun neverAboveFourK() {
        val tracks = listOf(track(4320, handled = false, exceeds = true), track(2160), track(1080, selected = true))
        assertEquals(2160, QualityRules.best(tracks, QualityRules.MAX_HEIGHT)?.height)
    }

    @Test
    fun neverForcesSoftwareDecodersHdrOrHighFrameRates() {
        val tracks = listOf(
            track(2160, handled = false, exceeds = true, hardware = false, group = 1),
            track(2160, handled = false, exceeds = true, hdr = true, group = 2),
            track(1080, fps = 120f, handled = false, exceeds = true),
            track(480, selected = true),
        )
        assertEquals(480, QualityRules.best(tracks, QualityRules.MAX_HEIGHT)?.height)
    }

    @Test
    fun hdrIsFineWhenTheDecoderAcceptsIt() {
        val tracks = listOf(track(2160, hdr = true), track(1080, selected = true))
        assertEquals(2160, QualityRules.best(tracks, QualityRules.MAX_HEIGHT)?.height)
    }

    @Test
    fun prefersTheTrackTheDecoderAcceptsAtTheSameHeight() {
        val accepted = track(2160, bitrate = 12_000_000, group = 1)
        val tracks = listOf(track(2160, bitrate = 20_000_000, handled = false, exceeds = true), accepted, track(360, selected = true))
        assertEquals(accepted, QualityRules.best(tracks, QualityRules.MAX_HEIGHT))
    }

    @Test
    fun unsupportedCodecsAreIgnored() {
        val tracks = listOf(track(2160, handled = false, exceeds = false), track(360, hardware = false, selected = true))
        assertNull(QualityRules.best(tracks, QualityRules.MAX_HEIGHT))
    }
}
