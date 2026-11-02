package ytslim.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TouchRulesTest {
    @Test
    fun sidesSeekAndTheMiddlePauses() {
        assertEquals(TouchRules.Zone.BACK, TouchRules.zone(100f, 1000))
        assertEquals(TouchRules.Zone.PLAY_PAUSE, TouchRules.zone(500f, 1000))
        assertEquals(TouchRules.Zone.FORWARD, TouchRules.zone(900f, 1000))
        assertEquals(TouchRules.Zone.PLAY_PAUSE, TouchRules.zone(10f, 0))
    }

    @Test
    fun seekingStaysInsideTheVideo() {
        assertEquals(50_000L, TouchRules.seekTarget(60_000, 600_000, TouchRules.Zone.BACK))
        assertEquals(70_000L, TouchRules.seekTarget(60_000, 600_000, TouchRules.Zone.FORWARD))
        assertEquals(0L, TouchRules.seekTarget(4_000, 600_000, TouchRules.Zone.BACK))
        assertEquals(600_000L, TouchRules.seekTarget(595_000, 600_000, TouchRules.Zone.FORWARD))
        // Duração ainda desconhecida (C.TIME_UNSET): avança sem teto.
        assertEquals(70_000L, TouchRules.seekTarget(60_000, Long.MIN_VALUE + 1, TouchRules.Zone.FORWARD))
    }
}
