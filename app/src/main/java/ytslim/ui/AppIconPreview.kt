package ytslim.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ytslim.data.AppIcon

/**
 * Amostra do ícone para a lista de cores das Configurações (celular e TV).
 * O Compose não desenha ícones adaptativos, então monta o mesmo ícone com o fundo e a frente.
 */
@Composable
fun AppIconPreview(icon: AppIcon, size: Dp = 40.dp) {
    val colors = listOf(colorResource(icon.top), colorResource(icon.bottom))
    val brush = if (icon.diagonal) Brush.linearGradient(colors, Offset.Zero, Offset.Infinite) else Brush.verticalGradient(colors)
    Box(Modifier.size(size).clip(CircleShape).background(brush)) {
        // A frente usa a grade de 108dp, e o launcher mostra só os 72dp do meio.
        Image(painterResource(icon.foreground), contentDescription = null, modifier = Modifier.fillMaxSize().scale(108f / 72f))
    }
}
