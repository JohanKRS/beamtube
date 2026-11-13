package ytslim.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Os estilos de texto do app, iguais na TV e no toque (2026-11-13, com a marca BeamTube): a Outfit
 * do logotipo em pesos que contrastam, como no nome. Títulos de página em bold com as letras um
 * pouco mais juntas; títulos de vídeo e botões em semibold e medium; o texto de apoio em regular,
 * sem o espaçamento extra do Material (a Outfit já é larga). As entrelinhas são mais curtas que as
 * do Material, para duas linhas de título lerem como um bloco.
 */
object TypeScale {
    private fun style(weight: FontWeight, size: Int, line: Int, tracking: Float = 0f) = TextStyle(
        fontFamily = Look.Outfit,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.em,
    )

    val displayLarge = style(FontWeight.Bold, 57, 62, -0.02f)
    val displayMedium = style(FontWeight.Bold, 45, 50, -0.02f)
    val displaySmall = style(FontWeight.Bold, 36, 42, -0.02f)
    val headlineLarge = style(FontWeight.Bold, 32, 38, -0.02f)
    val headlineMedium = style(FontWeight.Bold, 28, 34, -0.02f)
    val headlineSmall = style(FontWeight.SemiBold, 24, 30, -0.015f)
    val titleLarge = style(FontWeight.SemiBold, 22, 28, -0.01f)
    val titleMedium = style(FontWeight.SemiBold, 16, 22)
    val titleSmall = style(FontWeight.SemiBold, 14, 19)
    val bodyLarge = style(FontWeight.Normal, 16, 23)
    val bodyMedium = style(FontWeight.Normal, 14, 20)
    val bodySmall = style(FontWeight.Normal, 12, 16, 0.01f)
    val labelLarge = style(FontWeight.Medium, 14, 20)
    val labelMedium = style(FontWeight.Medium, 12, 16, 0.01f)
    val labelSmall = style(FontWeight.Medium, 11, 15, 0.02f)
}
