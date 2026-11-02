package ytslim.ui.mobile

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.channels.Channel

/**
 * Arrastar para reordenar uma [androidx.compose.foundation.lazy.LazyColumn] (o modo de reordenar
 * playlists no celular e no tablet). Segurar um vídeo, ou arrastar pela alça, pega o vídeo; ele
 * segue o dedo, os outros abrem espaço, e perto da borda a lista rola sozinha.
 *
 * Os itens que se movem têm como chave o ID do vídeo ([keys]); os outros itens (o aviso no topo)
 * ficam parados.
 */
@Stable
class DragReorderState internal constructor(
    private val list: LazyListState,
    private val keys: State<List<String>>,
    private val onMove: State<(Int, Int) -> Unit>,
) {
    /** O vídeo pego, ou null. */
    var draggingKey by mutableStateOf<String?>(null)
        private set

    private var dragged by mutableFloatStateOf(0f)
    private var initialOffset by mutableIntStateOf(0)

    internal val scroll = Channel<Float>(Channel.CONFLATED)

    private val draggingItem: LazyListItemInfo?
        get() = draggingKey?.let { key -> list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } }

    /** Quanto o vídeo pego está deslocado do lugar dele na lista (para o graphicsLayer). */
    val offset: Float
        get() = draggingItem?.let { initialOffset + dragged - it.offset } ?: 0f

    /** Pega o vídeo sob [y] (em pixels, a partir do topo da lista). */
    internal fun startAt(y: Float) {
        val at = y + list.layoutInfo.viewportStartOffset
        list.layoutInfo.visibleItemsInfo
            .firstOrNull { at.toInt() in it.offset..(it.offset + it.size) && it.key in keys.value }
            ?.let { start(it) }
    }

    /** Pega o vídeo [key] (arrastando pela alça). */
    internal fun start(key: String) {
        list.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }?.let { start(it) }
    }

    private fun start(item: LazyListItemInfo) {
        draggingKey = item.key as String
        initialOffset = item.offset
        dragged = 0f
    }

    internal fun stop() {
        draggingKey = null
        dragged = 0f
    }

    internal fun drag(dy: Float) {
        val item = draggingItem ?: return
        dragged += dy
        val top = item.offset + offset
        val bottom = top + item.size
        val middle = (top + bottom) / 2f
        val target = list.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != item.key && it.key in keys.value && middle.toInt() in it.offset..(it.offset + it.size)
        }
        if (target != null) {
            // A lista fica presa ao primeiro item visível: sem isso, mover esse item rola a lista junto.
            if (item.index == list.firstVisibleItemIndex || target.index == list.firstVisibleItemIndex) {
                list.requestScrollToItem(list.firstVisibleItemIndex, list.firstVisibleItemScrollOffset)
            }
            val ids = keys.value
            val from = ids.indexOf(item.key)
            val to = ids.indexOf(target.key)
            if (from >= 0 && to >= 0) onMove.value(from, to)
            return
        }
        val info = list.layoutInfo
        val overscroll = when {
            dragged > 0 -> (bottom - info.viewportEndOffset).coerceAtLeast(0f)
            dragged < 0 -> (top - info.viewportStartOffset).coerceAtMost(0f)
            else -> 0f
        }
        if (overscroll != 0f) scroll.trySend(overscroll)
    }
}

@Composable
fun rememberDragReorderState(list: LazyListState, keys: List<String>, onMove: (Int, Int) -> Unit): DragReorderState {
    val currentKeys = rememberUpdatedState(keys)
    val currentMove = rememberUpdatedState(onMove)
    val state = remember(list) { DragReorderState(list, currentKeys, currentMove) }
    LaunchedEffect(state) {
        for (diff in state.scroll) {
            list.scrollBy(diff)
            // A lista rolou: o vídeo pego pode ter passado por cima de outro.
            state.drag(0f)
        }
    }
    return state
}

/** Na lista: segurar um vídeo pega ele. */
fun Modifier.dragReorderOnLongPress(state: DragReorderState, enabled: Boolean): Modifier =
    if (!enabled) this else pointerInput(state) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.startAt(it.y) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = state::stop,
            onDragCancel = state::stop,
        )
    }

/** Na alça de um vídeo: arrastar pega ele na hora. */
fun Modifier.dragReorderHandle(state: DragReorderState, key: String, enabled: Boolean): Modifier =
    if (!enabled) this else pointerInput(state, key) {
        detectDragGestures(
            onDragStart = { state.start(key) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = state::stop,
            onDragCancel = state::stop,
        )
    }
