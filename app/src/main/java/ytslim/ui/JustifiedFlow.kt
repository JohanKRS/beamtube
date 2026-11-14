package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp

/**
 * Itens em linhas, como um FlowRow, mas cada linha (menos a última) cresce até a borda direita: a sobra
 * dela é dividida igualmente entre os itens, sem buraco no fim da linha. A última linha fica com os
 * itens no tamanho natural, à esquerda, como a última linha de um texto justificado.
 *
 * A quebra das linhas vem da largura natural (intrínseca) de cada item. Para as linhas não mudarem
 * quando um item muda de estado (uma sugestão marcada), ele precisa ter a mesma largura nos dois.
 */
@Composable
fun JustifiedFlow(spacing: Dp, lineSpacing: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val gap = spacing.roundToPx()
        val lineGap = lineSpacing.roundToPx()
        val natural = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(maxWidth) }

        val lines = mutableListOf<IntRange>()
        var start = 0
        var used = 0
        natural.forEachIndexed { i, width ->
            if (i > start && used + gap + width > maxWidth) {
                lines += start until i
                start = i
                used = width
            } else {
                used += if (i > start) gap + width else width
            }
        }
        if (natural.isNotEmpty()) lines += start until natural.size

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        val lineHeights = lines.mapIndexed { index, line ->
            val count = line.last - line.first + 1
            val taken = line.sumOf { natural[it] } + gap * (count - 1)
            val slack = if (index == lines.lastIndex) 0 else (maxWidth - taken).coerceAtLeast(0)
            line.forEachIndexed { k, i ->
                val width = natural[i] + slack / count + if (k < slack % count) 1 else 0
                placeables[i] = measurables[i].measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
            }
            line.maxOf { placeables[it]!!.height }
        }
        val height = lineHeights.sum() + lineGap * (lines.size - 1).coerceAtLeast(0)
        val width = if (constraints.hasBoundedWidth) maxWidth else lines.maxOfOrNull { line -> line.sumOf { placeables[it]!!.width } + gap * (line.last - line.first) } ?: 0

        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            lines.forEachIndexed { index, line ->
                var x = 0
                line.forEach { i ->
                    val placeable = placeables[i]!!
                    placeable.placeRelative(x, y + (lineHeights[index] - placeable.height) / 2)
                    x += placeable.width + gap
                }
                y += lineHeights[index] + lineGap
            }
        }
    }
}
