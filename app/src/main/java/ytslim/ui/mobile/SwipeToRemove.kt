package ytslim.ui.mobile

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ytslim.ui.Look

/**
 * Deslizar o vídeo para a esquerda tira ele da lista (Histórico e playlists do usuário). Só para a
 * esquerda: deslizar para a direita fica para as abas (o Histórico é a última delas, então não
 * há aba à direita para disputar o gesto).
 *
 * @param label o nome da ação para leitores de tela ("Remove from history").
 */
@Composable
fun SwipeToRemove(
    enabled: Boolean,
    label: String,
    corner: Dp,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier) { content() }
        return
    }
    val remove by rememberUpdatedState(onRemove)
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .semantics { customActions = listOf(CustomAccessibilityAction(label) { remove(); true }) }
            .pointerInput(Unit) {
                val velocity = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocity.resetTracking()
                    velocity.addPointerInputChange(down)
                    // Só pega o gesto para a esquerda (ou de volta, já com o vídeo deslocado).
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if (over < 0f || offset.value < 0f) change.consume()
                    } ?: return@awaitEachGesture
                    velocity.addPointerInputChange(start)
                    val ok = horizontalDrag(start.id) { change ->
                        val next = (offset.value + change.positionChange().x).coerceAtMost(0f)
                        scope.launch { offset.snapTo(next) }
                        velocity.addPointerInputChange(change)
                        change.consume()
                    }
                    val speed = velocity.calculateVelocity().x
                    scope.launch {
                        val far = offset.value < -width * REMOVE_FRACTION
                        val flung = speed < -FLING_VELOCITY && offset.value < 0f
                        if (ok && (far || flung) && width > 0) {
                            offset.animateTo(-width.toFloat())
                            remove()
                        } else {
                            offset.animateTo(0f)
                        }
                    }
                }
            },
    ) {
        if (offset.value < 0f) {
            Box(
                Modifier.matchParentSize().clip(RoundedCornerShape(corner)).background(Look.Live),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = Look.Text, modifier = Modifier.padding(end = 24.dp).size(24.dp))
            }
        }
        Box(Modifier.graphicsLayer { translationX = offset.value }) { content() }
    }
}

/** Passar de 40% da largura (ou soltar rápido) tira o vídeo; menos que isso, ele volta. */
private const val REMOVE_FRACTION = 0.4f
private const val FLING_VELOCITY = 1_500f
