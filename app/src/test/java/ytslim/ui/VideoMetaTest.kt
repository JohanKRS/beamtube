package ytslim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoMetaTest {
    @Test
    fun dropsTheChannelNameFromYouTubesLine() {
        assertEquals("12K views · 2 hours ago", metaAfterChannel("Bench Notes • 12K views • 2 hours ago", "Bench Notes", null))
    }

    @Test
    fun keepsTheLineWhenTheChannelIsNotInIt() {
        assertEquals("12K views · 2 hours ago", metaAfterChannel("12K views · 2 hours ago", "Bench Notes", null))
    }

    @Test
    fun fallsBackToThePublishedTime() {
        assertEquals("3 hours ago", metaAfterChannel("Bench Notes", "Bench Notes", "3 hours ago"))
        assertEquals("3 hours ago", metaAfterChannel(null, null, "3 hours ago"))
        assertNull(metaAfterChannel(null, "Bench Notes", null))
    }

    @Test
    fun monogramIsTheFirstLetterOrDigit() {
        assertEquals("B", monogramOf("bench notes"))
        assertEquals("3", monogramOf("  3Blue1Brown"))
        assertEquals("•", monogramOf(""))
        assertEquals("•", monogramOf(null))
    }
}
