package ytslim.ui.tv

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * O OK longo do controle remoto, medido pelo app. O Compose for TV conta o toque longo na primeira
 * repetição da tecla, e o sistema só repete depois de 0,4 s a 0,5 s (ou mais, conforme a TV); o
 * Johan achou demorado (2026-11-03). Aqui, segurar o OK por [holdMs] já chama [onLongClick], com o
 * botão ainda apertado; soltar antes chama [onClick].
 *
 * [onPress] liga e desliga o "apertado" do card (o encolher dele).
 */
class OkHold(
    private val scope: CoroutineScope,
    private val onPress: (Boolean) -> Unit,
    private val onClick: () -> Unit,
    private val onLongClick: () -> Unit,
    private val holdMs: Long = HOLD_MS,
) {
    private var timer: Job? = null

    /** Viu o OK descer aqui e ainda não viu subir. */
    private var active = false

    /** O OK desceu (só a primeira descida: as repetições do sistema não contam). */
    fun down() {
        reset()
        active = true
        onPress(true)
        timer = scope.launch {
            delay(holdMs)
            timer = null
            onPress(false)
            onLongClick()
        }
    }

    /** O OK subiu: é um clique se o toque longo ainda não aconteceu. */
    fun up() {
        if (!active) return
        active = false
        val waiting = timer ?: return
        waiting.cancel()
        timer = null
        onPress(false)
        onClick()
    }

    /** O foco saiu com o OK apertado: nem clique nem toque longo. */
    fun cancel() = reset()

    private fun reset() {
        if (!active) return
        active = false
        timer?.let {
            it.cancel()
            onPress(false)
        }
        timer = null
    }

    companion object {
        /** Quanto segurar o OK para ser um OK longo. */
        const val HOLD_MS = 300L
    }
}
