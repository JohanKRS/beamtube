package ytslim.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Uma mudança na conta com prazo para desfazer (pedido do Johan, 2026-11-03): [commit] só roda
 * depois de [delayMs], e até lá [undo] cancela. Depois que o commit começa, [undo] não faz nada.
 */
class PendingEdit(scope: CoroutineScope, delayMs: Long, commit: suspend () -> Unit) {
    private var claimed = false

    private val job = scope.launch {
        delay(delayMs)
        if (claim()) commit()
    }

    /** true se desfez a tempo (a conta não vai mudar). */
    fun undo(): Boolean {
        if (!claim()) return false
        job.cancel()
        return true
    }

    private fun claim(): Boolean = synchronized(this) {
        if (claimed) return false
        claimed = true
        true
    }
}

/** Mexer na lista da tela: arrastar um vídeo e devolver um vídeo tirado. */
object ListEdits {
    /** [list] com o item da posição [from] levado para [to]. */
    fun <T> moved(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        return list.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** [list] com [item] de volta na posição [index] (ou no fim, se a lista encolheu). */
    fun <T> putBack(list: List<T>, item: T, index: Int): List<T> =
        list.toMutableList().apply { add(index.coerceIn(0, size), item) }
}
