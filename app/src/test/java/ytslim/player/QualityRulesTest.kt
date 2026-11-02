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
        selected: Boolean = false,
        group: Int = 0,
    ) = VideoTrack(group, height, height, fps, bitrate, handled, exceeds, hardware, selected)

    @Test
    fun keepsTheChoiceWhenItIsAlreadyTheBest() {
        val tracks = listOf(track(1080, selected = true), track(720), track(360))
        assertNull(QualityRules.upgrade(tracks, 1080))
    }

    @Test
    fun goesPastDecoderLimitsThatAreTooLow() {
        // A TV de 32 bits: o decodificador de hardware diz que não dá conta de nada acima de 360p.
        val tracks = listOf(
            track(1080, exceeds = true, handled = false),
            track(720, exceeds = true, handled = false),
            track(360, selected = true),
        )
        assertEquals(1080, QualityRules.upgrade(tracks, 1080)?.height)
    }

    @Test
    fun staysWithinTheScreenAnd1080p() {
        val tracks = listOf(
            track(2160, exceeds = true, handled = false),
            track(1080, exceeds = true, handled = false),
            track(720, exceeds = true, handled = false),
            track(360, selected = true),
        )
        assertEquals(1080, QualityRules.upgrade(tracks, 1080)?.height)
        assertEquals(720, QualityRules.upgrade(tracks, 720)?.height)
    }

    @Test
    fun neverForcesSoftwareDecodersOrHighFrameRates() {
        val tracks = listOf(
            track(1080, exceeds = true, handled = false, hardware = false, group = 1),
            track(1080, fps = 120f, exceeds = true, handled = false),
            track(480, selected = true),
        )
        assertNull(QualityRules.upgrade(tracks, 1080))
    }

    @Test
    fun prefersTheTrackTheDecoderAcceptsAtTheSameHeight() {
        val accepted = track(1080, bitrate = 2_000_000, group = 1)
        val tracks = listOf(
            track(1080, bitrate = 4_000_000, exceeds = true, handled = false),
            accepted,
            track(360, selected = true),
        )
        assertEquals(accepted, QualityRules.upgrade(tracks, 1080))
    }

    @Test
    fun unsupportedCodecsAreIgnored() {
        val tracks = listOf(track(1080, handled = false, exceeds = false), track(360, selected = true))
        assertNull(QualityRules.upgrade(tracks, 1080))
    }
}
