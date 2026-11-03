package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A linha embaixo do título de um vídeo: o canal e o resto (visualizações, data). Os dois ficam
 * na mesma linha quando cabem inteiros; senão o resto desce para a linha de baixo, em vez de os
 * dois saírem cortados ("Computerp… · 268K views · 9 …"). O [separator] só aparece na linha única.
 *
 * @param stacked sempre em duas linhas (grades de várias colunas e TV: os cartões lado a lado
 * ficam iguais, em vez de uns com uma linha e outros com duas).
 * @param lineGap a distância da linha de baixo até a de cima (negativa para comer o espaço de toque
 * em volta do canal).
 */
@Composable
fun MetaLine(
    channel: (@Composable () -> Unit)?,
    rest: (@Composable () -> Unit)?,
    separator: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    lineGap: Dp = 0.dp,
) {
    Layout(contents = listOf(channel ?: {}, separator, rest ?: {}), modifier = modifier) { (channelPart, separatorPart, restPart), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val max = constraints.maxWidth
        val ch = channelPart.firstOrNull()?.measure(loose)
        val restMeasurable = restPart.firstOrNull()
        if (ch == null || restMeasurable == null) {
            val only = ch ?: restMeasurable?.measure(loose)
            return@Layout layout(constraints.constrainWidth(only?.width ?: 0), constraints.constrainHeight(only?.height ?: 0)) { only?.place(0, 0) }
        }
        val sep = separatorPart.first().measure(Constraints())
        val restWidth = if (stacked) 0 else restMeasurable.maxIntrinsicWidth(Constraints.Infinity)
        if (!stacked && ch.width + sep.width + restWidth <= max) {
            val r = restMeasurable.measure(loose.copy(maxWidth = (max - ch.width - sep.width).coerceAtLeast(0)))
            val height = maxOf(ch.height, r.height, sep.height)
            layout(constraints.constrainWidth(ch.width + sep.width + r.width), constraints.constrainHeight(height)) {
                ch.place(0, (height - ch.height) / 2)
                sep.place(ch.width, (height - sep.height) / 2)
                r.place(ch.width + sep.width, (height - r.height) / 2)
            }
        } else {
            val r = restMeasurable.measure(loose)
            val y = (ch.height + lineGap.roundToPx()).coerceAtLeast(0)
            layout(constraints.constrainWidth(maxOf(ch.width, r.width)), constraints.constrainHeight(maxOf(ch.height, y + r.height))) {
                ch.place(0, 0)
                r.place(0, y)
            }
        }
    }
}
