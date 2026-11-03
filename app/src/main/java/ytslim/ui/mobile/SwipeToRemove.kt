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
import androidx.compose.material.icons.filled.VerticalAlignTop
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
 * Deslizar o vídeo para a esquerda tira ele da lista (Histórico e playlists do usuário).
 *
 * Com [onSwipeRight] (só nas playlists do usuário, que abrem fora das abas), deslizar para a
 * direita leva o vídeo para o topo (pedido do Johan, 2026-11-03, no lugar da seta na linha). Sem
 * ele, deslizar para a direita fica para as abas (o Histórico é a última delas, então não há aba
 * à direita para disputar o gesto).
 *
 * @param label o nome da ação de tirar para leitores de tela ("Remove from history").
 * @param rightLabel o nome da ação de [onSwipeRight] ("Move to top").
 */
@Composable
fun SwipeToRemove(
    enabled: Boolean,
    label: String,
    corner: Dp,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onSwipeRight: (() -> Unit)? = null,
    rightLabel: String = "",
    content: @Composable () -> Unit,
) {
    if (!enabled && onSwipeRight == null) {
        Box(modifier) { content() }
        return
    }
    val remove by rememberUpdatedState(onRemove)
    val right by rememberUpdatedState(onSwipeRight)
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .onSizeChanged { width = it.width }
            .semantics {
                customActions = buildList {
                    if (enabled) add(CustomAccessibilityAction(label) { remove(); true })
                    if (onSwipeRight != null) add(CustomAccessibilityAction(rightLabel) { right?.invoke(); true })
                }
            }
            .pointerInput(enabled, onSwipeRight != null) {
                val velocity = VelocityTracker()
                val canRight = onSwipeRight != null
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocity.resetTracking()
                    velocity.addPointerInputChange(down)
                    // Só pega os lados que têm ação (ou a volta, já com o vídeo deslocado).
                    val start = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        if ((over < 0f && enabled) || (over > 0f && canRight) || offset.value != 0f) change.consume()
                    } ?: return@awaitEachGesture
                    velocity.addPointerInputChange(start)
                    val ok = horizontalDrag(start.id) { change ->
                        val next = (offset.value + change.positionChange().x)
                            .coerceIn(if (enabled) -Float.MAX_VALUE else 0f, if (canRight) Float.MAX_VALUE else 0f)
                        scope.launch { offset.snapTo(next) }
                        velocity.addPointerInputChange(change)
                        change.consume()
                    }
                    val speed = velocity.calculateVelocity().x
                    scope.launch {
                        val toLeft = offset.value < 0f && (offset.value < -width * REMOVE_FRACTION || speed < -FLING_VELOCITY)
                        val toRight = offset.value > 0f && (offset.value > width * REMOVE_FRACTION || speed > FLING_VELOCITY)
                        when {
                            ok && toLeft && width > 0 -> {
                                offset.animateTo(-width.toFloat())
                                remove()
                            }
                            ok && toRight && width > 0 -> {
                                // O vídeo sobe para o topo: a linha volta ao lugar e a lista anda.
                                right?.invoke()
                                offset.snapTo(0f)
                            }
                            else -> offset.animateTo(0f)
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
        if (offset.value > 0f) {
            Box(
                Modifier.matchParentSize().clip(RoundedCornerShape(corner)).background(Look.Glow),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.Filled.VerticalAlignTop, contentDescription = null, tint = Look.OnGlow, modifier = Modifier.padding(start = 24.dp).size(24.dp))
            }
        }
        Box(Modifier.graphicsLayer { translationX = offset.value }) { content() }
    }
}

/** Passar de 40% da largura (ou soltar rápido) faz a ação; menos que isso, o vídeo volta. */
private const val REMOVE_FRACTION = 0.4f
private const val FLING_VELOCITY = 1_500f
