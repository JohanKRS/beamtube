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

    @Test
    fun stripRemovesInvisibleBidiMarksAndLeadingBullets() {
        assertEquals(
            "Veritasium • 16.1M views • 9 hours ago",
            Handles.strip("\u200EVeritasium\u200E • \u200E16.1M views\u200E • \u200E9 hours ago\u200E"),
        )
        assertEquals("Капитан Кенгуру", Handles.strip("• Капитан Кенгуру"))
    }

    @Test
    fun extractsChannelFromRecommendedInfoLine() {
        assertEquals("Veritasium", Handles.channelFromInfo("Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("Manual do Mundo", Handles.channelFromInfo("Manual do Mundo • 1,2\u00A0mi de visualizações • há 3 dias"))
        assertEquals("QuantumFracture", Handles.channelFromInfo("QuantumFracture • 500 k vistas • hace 2 días"))
        assertEquals("5 Minute Crafts", Handles.channelFromInfo("5 Minute Crafts • 1.2M views • 2 days ago"))
        assertEquals("5 Minute Crafts", Handles.channelFromInfo("5 Minute Crafts • 2 days ago"))
        assertEquals("Review Lab", Handles.channelFromInfo("Review Lab • 10K views • 1 week ago"))
        assertEquals("Киноконцерн", Handles.channelFromInfo("Киноконцерн • 4.9M views • 8 months ago"))
    }

    @Test
    fun channelPagesWithoutChannelNameReturnNull() {
        assertNull(Handles.channelFromInfo("16.1M views • 9 hours ago"))
        assertNull(Handles.channelFromInfo("No views • 5 minutes ago"))
        assertNull(Handles.channelFromInfo("Nenhuma visualização • há 5 minutos"))
        assertNull(Handles.channelFromInfo("Sin visualizaciones • hace 5 minutos"))
        assertNull(Handles.channelFromInfo("Sin vistas • hace 1 hora"))
        assertNull(Handles.channelFromInfo("9 hours ago"))
        assertNull(Handles.channelFromInfo("Streamed 2 minutes ago"))
        assertNull(Handles.channelFromInfo("105 watching"))
        assertNull(Handles.channelFromInfo("1,2 mil assistindo agora"))
        assertNull(Handles.channelFromInfo("Scheduled for 8/22/25, 5:00 PM"))
        assertNull(Handles.channelFromInfo("4K • 16.1M views • 9 hours ago"))
    }

    @Test
    fun resolvesChannelNameWhenAuthorIsMissingOrABadge() {
        assertEquals("Veritasium", Handles.resolveChannelName(null, "Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("Veritasium", Handles.resolveChannelName("4K", "Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("Veritasium", Handles.resolveChannelName("New", "Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("Veritasium", Handles.resolveChannelName("Novo", "Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("Veritasium", Handles.resolveChannelName("Paid promotion", "Veritasium • 16.1M views • 9 hours ago"))
        assertEquals("3Blue1Brown", Handles.resolveChannelName("3Blue1Brown • @3blue1brown", "3Blue1Brown and PolyaMath • 500K views • 1 day ago"))
        assertEquals("Veritasium", Handles.resolveChannelName("Veritasium", "16.1M views • 9 hours ago"))
        assertNull(Handles.resolveChannelName("4K", "16.1M views • 9 hours ago"))
        assertNull(Handles.resolveChannelName(null, "16.1M views • 9 hours ago"))
    }
}
