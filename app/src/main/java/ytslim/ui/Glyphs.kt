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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/**
 * A "fresta" que marca a aba ativa: uma barrinha na cor de destaque, sem brilho em volta (pedido
 * do Johan, 2026-11-03: o brilho fica só no ícone do canto, o [DoorGlyph]).
 */
@Composable
fun LightSlit(modifier: Modifier) {
    Box(modifier.background(Look.Glow, RoundedCornerShape(2.dp)))
}

/**
 * A porta entreaberta do ícone, sem o fundo: o brilho em volta, a fresta com o miolo mais claro
 * e a luz no chão sumindo aos poucos. As medidas são as da grade de 108 do ícone
 * (tools/make_icons.py), da altura 24 à 84, centralizadas na largura.
 */
@Composable
fun DoorGlyph(modifier: Modifier) {
    Box(
        modifier.drawBehind {
            val s = size.height / 60f
            fun x(v: Float) = size.width / 2 + (v - 54f) * s
            fun y(v: Float) = (v - 24f) * s
            val center = Offset(x(54f), y(52f))
            withTransform({ scale(1f, 1.5f, center) }) {
                drawCircle(
                    Brush.radialGradient(
                        0f to Look.Glow.copy(alpha = 0.55f),
                        0.45f to Look.Glow.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                        center = center,
                        radius = 18f * s,
                    ),
                    radius = 18f * s,
                    center = center,
                )
            }
            val floor = Path().apply {
                moveTo(x(51f), y(74f))
                lineTo(x(57f), y(74f))
                lineTo(x(71f), y(84f))
                lineTo(x(37f), y(84f))
                close()
            }
            drawPath(
                floor,
                Brush.verticalGradient(
                    0f to Look.GlowSoft.copy(alpha = 0.42f),
                    0.5f to Look.GlowSoft.copy(alpha = 0.14f),
                    1f to Color.Transparent,
                    startY = y(74f),
                    endY = y(84f),
                ),
            )
            drawRoundRect(Look.GlowSoft, Offset(x(51f), y(30f)), Size(6f * s, 44f * s), CornerRadius(s))
            drawRoundRect(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.25f to SlitCore.copy(alpha = 0.8f),
                    0.75f to SlitCore.copy(alpha = 0.8f),
                    1f to Color.Transparent,
                    startY = y(33f),
                    endY = y(71f),
                ),
                Offset(x(53.4f), y(33f)),
                Size(1.2f * s, 38f * s),
                CornerRadius(0.6f * s),
            )
        },
    )
}

/** O miolo da fresta: a luz misturada com branco, como no ícone. */
private val SlitCore = Color(0xFFFFF4DA)
