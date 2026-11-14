package ytslim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ytslim.player.SleepBrightness

/**
 * O brilho do modo dormir nos menus e diálogos (1.0.4): eles abrem numa janela à parte, que a camada
 * preta da tela ([SleepBrightness.applyTo]) não cobre. Devolve a opacidade dessa camada agora.
 */
@Composable
fun sleepDim(): Float {
    val level by SleepBrightness.level.collectAsStateWithLifecycle()
    return SleepBrightness.windowDim(level)
}

/** Cobre o que vem depois com a camada preta de opacidade [dim], no formato [shape] do menu ou diálogo. */
fun Modifier.sleepDim(dim: Float, shape: Shape = RectangleShape): Modifier =
    if (dim <= 0f) this else drawWithContent {
        drawContent()
        drawOutline(shape.createOutline(size, layoutDirection, this), Color.Black.copy(alpha = dim))
    }
