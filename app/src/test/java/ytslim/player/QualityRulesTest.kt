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
        mime: String? = VP9,
    ) = VideoTrack(group, height, height, fps, bitrate, handled, exceeds, hardware, hdr, selected, mime)

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
    fun theButtonListsEveryPlayableHeight() {
        val tracks = tv + track(4320, handled = false, exceeds = false) + track(1080, group = 2)
        assertEquals(listOf(2160, 1440, 1080, 720, 360), QualityRules.heights(tracks))
    }

    @Test
    fun manualChoiceGoesPastTheCeilingAndPrefersHardware() {
        val software = track(2160, hardware = false, group = 2)
        val hardware = track(2160, handled = false, exceeds = true, group = 1)
        val tracks = listOf(software, hardware, track(360, selected = true))
        assertEquals(hardware, QualityRules.manual(tracks, 2160))
        assertNull(QualityRules.manual(tracks, 1080))
    }

    @Test
    fun unsupportedCodecsAreIgnored() {
        val tracks = listOf(track(2160, handled = false, exceeds = false), track(360, hardware = false, selected = true))
        assertNull(QualityRules.best(tracks, QualityRules.MAX_HEIGHT))
    }

    @Test
    fun fourKStartsWithTheLargestTrackUpTo1080pOfTheSameCodec() {
        // O grupo MP4 do YouTube mistura AVC (até 1080p) e AV1: a partida tem de ser do codec do alvo.
        val tracks = listOf(
            track(2160, mime = AV1),
            track(1440, mime = AV1),
            track(1080, mime = AVC, bitrate = 9_000_000),
            track(1080, mime = AV1),
            track(720, mime = AV1),
        )
        val target = QualityRules.best(tracks, QualityRules.MAX_HEIGHT)!!
        val start = QualityRules.start(tracks, target)!!
        assertEquals(1080, start.height)
        assertEquals(AV1, start.mime)
    }

    @Test
    fun theStartKeepsTheFrameRateAndTheHdrOfTheTarget() {
        val tracks = listOf(
            track(2160, fps = 60f),
            track(1080, fps = 30f),
            track(1080, fps = 60f, hdr = true),
            track(720, fps = 60f),
        )
        val start = QualityRules.start(tracks, tracks[0])!!
        assertEquals(720, start.height)
        assertEquals(60f, start.frameRate)
    }

    @Test
    fun theStartIsOnlyFromTheSameGroup() {
        val tracks = listOf(track(2160, group = 1), track(1080, group = 0), track(720, group = 1))
        assertEquals(720, QualityRules.start(tracks, tracks[0])?.height)
        assertNull(QualityRules.start(listOf(track(2160, group = 1), track(1080, group = 0)), track(2160, group = 1)))
    }

    @Test
    fun upTo1080pTheVideoStartsAtTheTarget() {
        val tracks = listOf(track(1080), track(720), track(480))
        assertNull(QualityRules.start(tracks, tracks[0]))
        // A TV que declara menos do que decodifica: a partida também vale acima do declarado.
        val declaresLess = listOf(track(2160, handled = false, exceeds = true), track(1080, handled = false, exceeds = true))
        assertEquals(1080, QualityRules.start(declaresLess, declaresLess[0])?.height)
    }

    private companion object {
        const val VP9 = "video/x-vnd.on2.vp9"
        const val AV1 = "video/av01"
        const val AVC = "video/avc"
    }
}
