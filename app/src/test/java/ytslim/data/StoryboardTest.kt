package ytslim.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StoryboardTest {
    // O tamanho que o motor escolhe na maioria dos vídeos: folhas de 5 x 5 quadros, um a cada 2 s.
    private val board = Storyboard(cols = 5, rows = 5, intervalMs = 2_000) { "https://i.ytimg.com/sb/x/storyboard3_L2/M$it.jpg" }

    @Test
    fun findsTheFrameOfAPosition() {
        assertEquals(Storyboard.Frame(sheet = 0, col = 0, row = 0), board.frameAt(0))
        assertEquals(Storyboard.Frame(sheet = 0, col = 1, row = 0), board.frameAt(2_500))
        assertEquals(Storyboard.Frame(sheet = 0, col = 0, row = 1), board.frameAt(10_000))
        assertEquals(Storyboard.Frame(sheet = 0, col = 4, row = 4), board.frameAt(49_999))
        assertEquals(Storyboard.Frame(sheet = 1, col = 0, row = 0), board.frameAt(50_000))
        assertEquals(Storyboard.Frame(sheet = 0, col = 0, row = 0), board.frameAt(-5))
    }

    @Test
    fun countsTheSheetsOfAVideo() {
        assertEquals(0, board.sheetCount(0))
        assertEquals(1, board.sheetCount(50_000))
        assertEquals(2, board.sheetCount(50_001))
        assertEquals(19, board.sheetCount(917_000))
    }
}
