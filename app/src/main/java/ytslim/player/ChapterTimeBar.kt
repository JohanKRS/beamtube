package ytslim.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Region
import android.os.Build
import android.util.AttributeSet
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import ytslim.R
import kotlin.math.abs

/**
 * A barra de tempo do player com os capítulos: uma fresta na barra onde cada um começa, como no
 * YouTube (pedido do Johan, 2026-11-03). O resto é o DefaultTimeBar do Media3; a fresta é um
 * recorte feito antes de ele desenhar, e some quando o círculo da posição passa por ela.
 *
 * As medidas da barra (altura e o círculo arrastado, que define a margem dos lados) vêm dos mesmos
 * dimens do player_controls.xml, para a fresta cair no lugar certo.
 */
@UnstableApi
class ChapterTimeBar(context: Context, attrs: AttributeSet?) : DefaultTimeBar(context, attrs) {
    private var starts = LongArray(0)
    private var length = C.TIME_UNSET
    private var shown = 0L
    private var scrubbing = false
    private var scrubAt = 0L

    private val barHeight = resources.getDimensionPixelSize(R.dimen.player_bar_height)

    /** O mesmo cálculo do DefaultTimeBar: metade do maior círculo, arredondada para cima. */
    val inset = (resources.getDimensionPixelSize(R.dimen.player_scrubber_dragged) + 1) / 2
    private val gap = resources.getDimension(R.dimen.player_chapter_gap)

    /** Chamado quando a posição mostrada muda (o PlayerControlView atualiza enquanto os controles estão na tela). */
    var onPosition: ((Long) -> Unit)? = null

    init {
        addListener(
            object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    scrubbing = true
                    scrubAt = position
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    scrubAt = position
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    scrubbing = false
                }
            },
        )
    }

    /** Onde começa cada capítulo (o primeiro, no 0, não tem fresta). */
    fun setChapters(startsMs: List<Long>) {
        starts = startsMs.filter { it > 0 }.toLongArray()
        invalidate()
    }

    override fun setDuration(duration: Long) {
        length = duration
        super.setDuration(duration)
    }

    override fun setPosition(position: Long) {
        val changed = position != shown
        shown = position
        super.setPosition(position)
        if (changed) onPosition?.invoke(position)
    }

    /** O x (nesta view) de [positionMs] na barra. */
    fun xOf(positionMs: Long): Float {
        val left = paddingLeft + inset
        val width = (this.width - paddingRight - inset - left).coerceAtLeast(0)
        val duration = length
        if (duration <= 0) return left.toFloat()
        return left + width * (positionMs.coerceIn(0, duration).toFloat() / duration)
    }

    override fun onDraw(canvas: Canvas) {
        if (starts.isEmpty() || length <= 0 || width <= 0) return super.onDraw(canvas)
        val top = (height - barHeight) / 2f
        val bottom = top + barHeight
        val head = xOf(if (scrubbing) scrubAt else shown)
        canvas.save()
        for (start in starts) {
            if (start >= length) continue
            val x = xOf(start)
            if (abs(x - head) < inset) continue
            clipOut(canvas, x - gap / 2, top, x + gap / 2, bottom)
        }
        super.onDraw(canvas)
        canvas.restore()
    }

    private fun clipOut(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        if (Build.VERSION.SDK_INT >= 26) {
            canvas.clipOutRect(left, top, right, bottom)
        } else {
            @Suppress("DEPRECATION")
            canvas.clipRect(left, top, right, bottom, Region.Op.DIFFERENCE)
        }
    }
}
