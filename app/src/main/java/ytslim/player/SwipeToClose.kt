package ytslim.player

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs

/** Quando um arrastar no vídeo vira "fechar o player" ([SwipeToClose]). Distâncias em px. */
internal object SwipeRules {
    /** Começa a arrastar depois de descer isto vezes o "touch slop" do aparelho: um toque tremido não conta. */
    const val START_SLOPS = 2

    /** Mais para baixo do que para o lado, com folga: um arrastar torto para o lado não fecha nada. */
    const val VERTICAL_RATIO = 1.5f

    /** Soltar depois de descer esta fração da altura fecha... */
    const val CLOSE_FRACTION = 0.2f

    /** ...ou esta distância, se for menor (no celular em pé, 20% da altura seria um arrastar comprido). */
    const val CLOSE_MAX_DP = 160

    /** Um arrastar rápido para baixo fecha mesmo curto, desde que tenha descido isto. */
    const val FLING_MIN_DP = 32
    const val FLING_DP_PER_S = 1000

    /** O quanto o vídeo encolhe enquanto desce, na altura toda (a 20%, fica com 94%). */
    const val SHRINK = 0.3f
    const val MIN_SCALE = 0.7f

    /** Para baixo, de verdade, e não um toque nem um arrastar para o lado. */
    fun starts(dx: Float, dy: Float, slop: Int): Boolean = dy > slop * START_SLOPS && dy > abs(dx) * VERTICAL_RATIO

    /** Soltou: fecha se desceu o bastante, ou desceu um pouco num movimento rápido para baixo. */
    fun closes(distance: Float, height: Int, velocity: Float, density: Float): Boolean {
        val far = minOf(height * CLOSE_FRACTION, CLOSE_MAX_DP * density)
        val flung = velocity >= FLING_DP_PER_S * density && distance >= FLING_MIN_DP * density
        return distance >= far || flung
    }

    /** O tamanho do vídeo com o dedo [distance] abaixo de onde começou. */
    fun scale(distance: Float, height: Int): Float =
        if (height <= 0) 1f else (1f - SHRINK * distance / height).coerceIn(MIN_SCALE, 1f)
}

/**
 * Arrastar o vídeo para baixo fecha o player, no celular e no tablet (pedido do Johan, 2026-11-14:
 * ele esconde a barra de navegação e até o indicador, e para sair do vídeo só tinha o gesto de
 * voltar). O vídeo segue o dedo, encolhendo um pouco; soltar longe o bastante (ou num movimento
 * rápido) fecha ([onClose]: desde a 1.0.8, a miniatura desfocada volta por cima do vídeo e o player
 * fecha encolhendo, o inverso da abertura). Soltar antes disso devolve o vídeo ao lugar.
 *
 * Só para baixo: para cima e para os lados nada muda. Os toques continuam como antes: um toque
 * mostra os controles, dois pulam 10 s; os botões e a barra de tempo pegam os toques deles antes.
 */
internal class SwipeToClose(private val content: View, private val onClose: () -> Unit) {
    private val slop = ViewConfiguration.get(content.context).scaledTouchSlop
    private val density = content.resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f

    /** Onde o arrastar começou de fato (depois da folga), para o vídeo não pular ao começar. */
    private var anchorY = 0f
    private var tracker: VelocityTracker? = null

    /** O toque atual virou arrastar: daqui até soltar, ele é deste gesto. */
    var dragging = false
        private set

    /** Recebe cada toque no vídeo. true: o toque é deste gesto (os outros gestos não o veem mais). */
    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                content.animate().cancel()
                settle(animated = false)
                downX = event.rawX
                downY = event.rawY
                dragging = false
                tracker?.recycle()
                tracker = VelocityTracker.obtain()
                track(event)
            }
            MotionEvent.ACTION_MOVE -> {
                track(event)
                if (!dragging && SwipeRules.starts(event.rawX - downX, event.rawY - downY, slop)) {
                    dragging = true
                    anchorY = event.rawY
                }
                if (dragging) follow(event.rawY - anchorY)
            }
            MotionEvent.ACTION_UP -> {
                track(event)
                val wasDragging = dragging
                if (dragging) release(event.rawY - anchorY)
                end()
                return wasDragging
            }
            MotionEvent.ACTION_CANCEL -> {
                val wasDragging = dragging
                if (dragging) settle(animated = true)
                end()
                return wasDragging
            }
        }
        return dragging
    }

    private fun follow(distance: Float) {
        val down = distance.coerceAtLeast(0f)
        val scale = SwipeRules.scale(down, content.height)
        content.translationY = down
        content.scaleX = scale
        content.scaleY = scale
    }

    private fun release(distance: Float) {
        val velocity = tracker?.run {
            computeCurrentVelocity(1000)
            yVelocity
        } ?: 0f
        if (SwipeRules.closes(distance.coerceAtLeast(0f), content.height, velocity, density)) {
            // O vídeo fica onde o dedo soltou; a janela do player continua descendo e some.
            onClose()
        } else {
            settle(animated = true)
        }
    }

    /** O vídeo de volta ao lugar. */
    private fun settle(animated: Boolean) {
        if (!animated) {
            content.translationY = 0f
            content.scaleX = 1f
            content.scaleY = 1f
            return
        }
        content.animate().translationY(0f).scaleX(1f).scaleY(1f)
            .setDuration(SETTLE_MS).setInterpolator(DecelerateInterpolator())
    }

    private fun end() {
        dragging = false
        tracker?.recycle()
        tracker = null
    }

    /**
     * A velocidade, medida em coordenadas da tela: o vídeo anda junto com o dedo, e nas coordenadas
     * dele o dedo pareceria parado (a velocidade sairia zero).
     */
    private fun track(event: MotionEvent) {
        val tracker = tracker ?: return
        val onScreen = MotionEvent.obtain(event)
        onScreen.setLocation(event.rawX, event.rawY)
        tracker.addMovement(onScreen)
        onScreen.recycle()
    }

    private companion object {
        const val SETTLE_MS = 180L
    }
}
