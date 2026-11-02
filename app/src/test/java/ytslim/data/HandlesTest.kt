package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HandlesTest {
    @Test
    fun tvCardsLoseTheHandle() {
        // O menu dos cartões da TV manda "nome • @handle" como autor.
        assertEquals("Veritasium", Handles.channelName("Veritasium • @veritasium"))
        assertEquals("Veritasium", Handles.channelName("Veritasium"))
    }

    @Test
    fun aHandleAloneLosesTheAt() {
        assertEquals("veritasium", Handles.channelName("@veritasium"))
        assertNull(Handles.channelName("@"))
        assertNull(Handles.channelName(null))
    }

    @Test
    fun searchChannelLinesLoseTheHandle() {
        assertEquals("21.4M subscribers", Handles.strip("@veritasium • 21.4M subscribers"))
        assertNull(Handles.strip("@veritasium"))
    }

    @Test
    fun linesWithoutHandleStayAsTheyCame() {
        assertEquals("Veritasium • 10M views · 6 months ago", Handles.strip("Veritasium • 10M views · 6 months ago"))
        assertNull(Handles.strip(null))
    }
}
