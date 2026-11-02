package ytslim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

/** A "fresta" de luz da porta, que marca a aba ativa: uma barrinha âmbar com um brilho suave em volta. */
@Composable
fun LightSlit(modifier: Modifier) {
    Box(
        modifier
            .drawBehind {
                // Um brilho redondo, achatado na direção curta da barrinha.
                val spread = 9.dp.toPx()
                val radius = max(size.width, size.height) / 2 + spread
                val squash = (min(size.width, size.height) / 2 + spread) / radius
                withTransform({
                    if (size.width >= size.height) scale(1f, squash, center) else scale(squash, 1f, center)
                }) {
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Look.Glow.copy(alpha = 0.42f), Look.Glow.copy(alpha = 0f)), center, radius),
                        radius = radius,
                        center = center,
                    )
                }
            }
            .background(Look.Glow, RoundedCornerShape(2.dp)),
    )
}

/** A porta entreaberta do ícone: só a fresta de luz, sem o fundo. */
@Composable
fun DoorGlyph(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            val w = size.width
            val h = size.height
            drawRoundRect(Look.Glow.copy(alpha = 0.08f), Offset.Zero, Size(w, h), CornerRadius(w * 0.45f))
            drawRoundRect(Look.Glow.copy(alpha = 0.16f), Offset(w * 0.17f, h * 0.05f), Size(w * 0.66f, h * 0.9f), CornerRadius(w * 0.3f))
            drawRoundRect(Look.GlowSoft, Offset(w * 0.33f, h * 0.08f), Size(w * 0.34f, h * 0.84f), CornerRadius(w * 0.08f))
        },
    )
}
