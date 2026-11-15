package ytslim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A amostra de um fundo nos Ajustes ([Background]): um círculo na cor dele, com um contorno claro,
 * porque os três ficam quase iguais em cima do cartão escuro.
 */
@Composable
fun BackgroundSwatch(background: Background, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(background.tones.ground, CircleShape)
            .border(1.dp, Look.Outline, CircleShape),
    )
}
