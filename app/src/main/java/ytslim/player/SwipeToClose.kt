package ytslim.player

import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
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

    /** Para baixo, de verdade, e não um toque nem um arrastar para o lado. */
    fun starts(dx: Float, dy: Float, slop: Int): Boolean = dy > slop * START_SLOPS && dy > abs(dx) * VERTICAL_RATIO

    /** O quanto descer para fechar ao soltar. */
    fun closeDistance(height: Int, density: Float): Float = minOf(height * CLOSE_FRACTION, CLOSE_MAX_DP * density)

    /** Soltou: fecha se desceu o bastante, ou desceu um pouco num movimento rápido para baixo. */
    fun closes(distance: Float, height: Int, velocity: Float, density: Float): Boolean {
        val flung = velocity >= FLING_DP_PER_S * density && distance >= FLING_MIN_DP * density
        return distance >= closeDistance(height, density) || flung
    }

    /** O quanto do caminho até fechar o dedo já desceu: 0 no começo, 1 onde soltar já fecha. */
    fun progress(distance: Float, height: Int, density: Float): Float {
        val far = closeDistance(height, density)
        return if (far <= 0f) 1f else (distance / far).coerceIn(0f, 1f)
    }
}

/**
 * Arrastar para baixo no vídeo fecha o player, no celular e no tablet (pedido do Johan, 2026-11-14:
 * ele esconde a barra de navegação e até o indicador, e para sair do vídeo só tinha o gesto de
 * voltar).
 *
 * Desde a 1.0.9 (pedido do Johan: "quando puxar para baixo, dar um blurzinho e daí já fechar. Um
 * blur, um fade to black"), o vídeo não sai do lugar: o dedo descendo vai cobrindo o vídeo com a
 * miniatura desfocada e escurecendo a tela ([onDrag], de 0 a 1 no ponto em que soltar fecha).
 * Soltar longe o bastante (ou num movimento rápido) fecha ([onClose]); soltar antes disso devolve
 * o vídeo ([onCancel]). Até a 1.0.8 o vídeo descia junto com o dedo, encolhendo.
 *
 * Só para baixo: para cima e para os lados nada muda. Os toques continuam como antes: um toque
 * mostra os controles, dois pulam 10 s; os botões e a barra de tempo pegam os toques deles antes.
 */
internal class SwipeToClose(
    private val area: View,
    private val onDrag: (progress: Float) -> Unit,
    private val onCancel: () -> Unit,
    private val onClose: () -> Unit,
) {
    private val slop = ViewConfiguration.get(area.context).scaledTouchSlop
    private val density = area.resources.displayMetrics.density
    private var downX = 0f
    private var downY = 0f

    /** Onde o arrastar começou de fato (depois da folga), para o desfoque não pular ao começar. */
    private var anchorY = 0f
    private var tracker: VelocityTracker? = null

    /** O toque atual virou arrastar: daqui até soltar, ele é deste gesto. */
    var dragging = false
        private set

    /** Recebe cada toque no vídeo. true: o toque é deste gesto (os outros gestos não o veem mais). */
    fun onTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
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
                if (dragging) onDrag(SwipeRules.progress((event.rawY - anchorY).coerceAtLeast(0f), area.height, density))
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
                if (dragging) onCancel()
                end()
                return wasDragging
            }
        }
        return dragging
    }

    private fun release(distance: Float) {
        val velocity = tracker?.run {
            computeCurrentVelocity(1000)
            yVelocity
        } ?: 0f
        if (SwipeRules.closes(distance.coerceAtLeast(0f), area.height, velocity, density)) onClose() else onCancel()
    }

    private fun end() {
        dragging = false
        tracker?.recycle()
        tracker = null
    }

    /** A velocidade, medida em coordenadas da tela (o que conta é o dedo na tela). */
    private fun track(event: MotionEvent) {
        val tracker = tracker ?: return
        val onScreen = MotionEvent.obtain(event)
        onScreen.setLocation(event.rawX, event.rawY)
        tracker.addMovement(onScreen)
        onScreen.recycle()
    }
}
