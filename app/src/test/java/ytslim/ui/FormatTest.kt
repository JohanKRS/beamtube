package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun formatsDurations() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("0:59", formatDuration(59_000))
        assertEquals("1:00", formatDuration(60_000))
        assertEquals("12:34", formatDuration(754_000))
        assertEquals("59:59", formatDuration(3_599_000))
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
        assertEquals("10:00:00", formatDuration(36_000_000))
    }
}
