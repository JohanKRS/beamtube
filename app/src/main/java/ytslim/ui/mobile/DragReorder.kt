package ytslim.ui.mobile

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.channels.Channel

/**
 * Arrastar para mudar a ordem numa [androidx.compose.foundation.lazy.grid.LazyVerticalGrid] (as
 * playlists do usuário no celular e no tablet). Segurar o vídeo pega ele (sem alça nem ícone na
 * linha: pedido do Johan, 2026-11-03); ele segue o dedo, os outros abrem espaço, e perto da borda
 * a lista rola sozinha. Ao soltar, [onDrop] manda a posição nova para a conta (sem um modo de
 * reordenar antes).
 *
 * Os itens que se movem têm como chave a mesma de [keys], na ordem da lista; os outros itens (o
 * fim da lista) ficam parados.
 */
@Stable
class DragReorderState internal constructor(
    private val grid: LazyGridState,
    private val keys: State<List<Any>>,
    private val onMove: State<(Int, Int) -> Unit>,
    private val onDrop: State<() -> Unit>,
) {
    /** O vídeo pego, ou null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var dragged by mutableStateOf(Offset.Zero)
    private var initialOffset by mutableStateOf(IntOffset.Zero)

    internal val scroll = Channel<Float>(Channel.CONFLATED)

    private val draggingItem: LazyGridItemInfo?
        get() = draggingKey?.let { key -> grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } }

    /** Quanto o vídeo pego está deslocado do lugar dele na grade (para o graphicsLayer). */
    val offset: Offset
        get() = draggingItem?.let {
            Offset(initialOffset.x + dragged.x - it.offset.x, initialOffset.y + dragged.y - it.offset.y)
        } ?: Offset.Zero

    /** Pega o vídeo [key]. */
    internal fun start(key: Any) {
        val item = grid.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        draggingKey = key
        initialOffset = item.offset
        dragged = Offset.Zero
    }

    /** Solta o vídeo onde ele está. */
    internal fun stop() {
        if (draggingKey == null) return
        draggingKey = null
        dragged = Offset.Zero
        onDrop.value()
    }

    internal fun drag(delta: Offset) {
        val item = draggingItem ?: return
        dragged += delta
        val shift = offset
        val left = item.offset.x + shift.x
        val top = item.offset.y + shift.y
        val bottom = top + item.size.height
        val centerX = (left + item.size.width / 2f).toInt()
        val centerY = (top + item.size.height / 2f).toInt()
        val target = grid.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != item.key && it.key in keys.value &&
                centerX in it.offset.x..(it.offset.x + it.size.width) &&
                centerY in it.offset.y..(it.offset.y + it.size.height)
        }
        if (target != null) {
            // A grade fica presa ao primeiro item visível: sem isso, mover esse item rola a lista junto.
            if (item.index == grid.firstVisibleItemIndex || target.index == grid.firstVisibleItemIndex) {
                grid.requestScrollToItem(grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset)
            }
            val ids = keys.value
            val from = ids.indexOf(item.key)
            val to = ids.indexOf(target.key)
            if (from >= 0 && to >= 0) onMove.value(from, to)
            return
        }
        val info = grid.layoutInfo
        val overscroll = when {
            dragged.y > 0 -> (bottom - info.viewportEndOffset).coerceAtLeast(0f)
            dragged.y < 0 -> (top - info.viewportStartOffset).coerceAtMost(0f)
            else -> 0f
        }
        if (overscroll != 0f) scroll.trySend(overscroll)
    }
}

@Composable
fun rememberDragReorderState(
    grid: LazyGridState,
    keys: List<Any>,
    onMove: (Int, Int) -> Unit,
    onDrop: () -> Unit,
): DragReorderState {
    val currentKeys = rememberUpdatedState(keys)
    val currentMove = rememberUpdatedState(onMove)
    val currentDrop = rememberUpdatedState(onDrop)
    val state = remember(grid) { DragReorderState(grid, currentKeys, currentMove, currentDrop) }
    LaunchedEffect(state) {
        for (diff in state.scroll) {
            grid.scrollBy(diff)
            // A lista rolou: o vídeo pego pode ter passado por cima de outro.
            state.drag(Offset.Zero)
        }
    }
    return state
}

/**
 * Na linha de um vídeo: segurar pega ele ([onLift] avisa, para a vibração), e arrastar move. Fica
 * depois do clique da linha na cadeia de modificadores, para soltar sem arrastar não abrir o vídeo.
 * Rolar a lista ou deslizar para o lado antes do tempo do toque longo cancela a pegada.
 */
fun Modifier.dragReorderOnLongPress(state: DragReorderState, key: Any, onLift: () -> Unit): Modifier =
    pointerInput(state, key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                onLift()
                state.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount)
            },
            onDragEnd = state::stop,
            onDragCancel = state::stop,
        )
    }
