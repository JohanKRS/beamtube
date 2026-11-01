package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test
    fun formatsDurations() {
        assertEquals("0:05", formatDuration(5_000))
        assertEquals("12:34", formatDuration(754_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }
}
