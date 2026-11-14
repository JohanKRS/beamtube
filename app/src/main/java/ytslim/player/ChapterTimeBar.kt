package ytslim.player

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Region
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.TimeBar
import ytslim.R
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.math.abs

/**
 * A barra de tempo do player com os capítulos: uma fresta na barra onde cada um começa, como no
 * YouTube (pedido do Johan, 2026-11-03). O resto é o DefaultTimeBar do Media3; a fresta é um
 * recorte feito antes de ele desenhar, e some quando o círculo da posição passa por ela.
 *
 * As medidas da barra (altura e o círculo arrastado, que define a margem dos lados) vêm dos mesmos
 * dimens do player_controls.xml, para a fresta cair no lugar certo.
 *
 * Pelo controle remoto (1.0.4, pedido do Johan), esquerda e direita só andam com o círculo e a
 * miniatura: o vídeo vai para o ponto escolhido só com OK. Sair da barra (descer, subir, voltar ou
 * os controles sumirem) desiste, e o vídeo continua de onde estava. No DefaultTimeBar, o ponto
 * valia sozinho 1 s depois da última tecla, e também ao sair da barra. O toque continua o dele.
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

    /** Os ouvintes da barra (os do DefaultTimeBar são privados): o arrastar pelas teclas avisa estes. */
    private val listeners = CopyOnWriteArraySet<TimeBar.OnScrubListener>()

    /** O arrastar pelo controle remoto está aberto (até o OK ou sair da barra). */
    var keyScrubbing = false
        private set
    private var keyIncrement = C.TIME_UNSET

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

    override fun addListener(listener: TimeBar.OnScrubListener) {
        listeners.add(listener)
        super.addListener(listener)
    }

    override fun removeListener(listener: TimeBar.OnScrubListener) {
        listeners.remove(listener)
        super.removeListener(listener)
    }

    override fun setKeyTimeIncrement(time: Long) {
        keyIncrement = time
        super.setKeyTimeIncrement(time)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!isEnabled || length <= 0) return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val step = if (keyIncrement != C.TIME_UNSET) keyIncrement else length / 20
                val from = if (keyScrubbing) scrubAt else shown
                val to = (from + if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -step else step).coerceIn(0, length)
                // Parado numa ponta, a tecla segue para o foco (como no DefaultTimeBar), mas não no
                // meio de um arrastar: lá ela não faz nada, para não desistir sem querer.
                if (to == from) return keyScrubbing || super.onKeyDown(keyCode, event)
                if (!keyScrubbing) {
                    keyScrubbing = true
                    listeners.forEach { it.onScrubStart(this, from) }
                }
                super.setPosition(to)
                listeners.forEach { it.onScrubMove(this, to) }
                invalidate()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (keyScrubbing) {
                    finishKeyScrub(canceled = false)
                    return true
                }
            }
            KeyEvent.KEYCODE_BACK -> {
                // Voltar desiste do ponto e fica na barra; o próximo voltar sai como antes.
                if (keyScrubbing) {
                    finishKeyScrub(canceled = true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    /** Desiste do ponto escolhido pelo controle remoto, se houver: o vídeo continua de onde estava. */
    fun cancelKeyScrub() {
        if (keyScrubbing) finishKeyScrub(canceled = true)
    }

    private fun finishKeyScrub(canceled: Boolean) {
        keyScrubbing = false
        val position = scrubAt
        if (canceled) super.setPosition(shown)
        listeners.forEach { it.onScrubStop(this, position, canceled) }
        invalidate()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        if (!gainFocus) cancelKeyScrub()
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!isVisible) cancelKeyScrub()
    }

    override fun onDetachedFromWindow() {
        cancelKeyScrub()
        super.onDetachedFromWindow()
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
        // No meio de um arrastar pelo controle, o círculo fica no ponto escolhido.
        if (!keyScrubbing) super.setPosition(position)
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
