package ytslim.player

import android.graphics.Color
import android.text.Layout
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class CaptionRulesTest {
    @Test
    fun captionsAreCenteredButKeepTheirLine() {
        // Como vem a legenda automática do YouTube: alinhada à esquerda, presa à borda.
        val youtube = Cue.Builder()
            .setText("hello there")
            .setTextAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setPosition(0f)
            .setPositionAnchor(Cue.ANCHOR_TYPE_START)
            .setSize(0.8f)
            .setLine(-2f, Cue.LINE_TYPE_NUMBER)
            .build()
        val cue = CaptionRules.centered(listOf(youtube)).single()
        assertEquals(Layout.Alignment.ALIGN_CENTER, cue.textAlignment)
        assertEquals(Cue.DIMEN_UNSET, cue.position)
        assertEquals(Cue.TYPE_UNSET, cue.positionAnchor)
        assertEquals(Cue.DIMEN_UNSET, cue.size)
        // A segunda linha da legenda continua acima da primeira.
        assertEquals(-2f, cue.line)
        assertEquals(Cue.LINE_TYPE_NUMBER, cue.lineType)
    }

    @Test
    fun whiteTextWithABlackOutlineAndNoBackground() {
        val style = CaptionRules.style(null)
        assertEquals(Color.WHITE, style.foregroundColor)
        assertEquals(Color.TRANSPARENT, style.backgroundColor)
        assertEquals(Color.TRANSPARENT, style.windowColor)
        assertEquals(CaptionStyleCompat.EDGE_TYPE_OUTLINE, style.edgeType)
        assertEquals(Color.BLACK, style.edgeColor)
    }
}
