package ytslim.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ytslim.R

/**
 * A aparência do BeamTube (etapa 6, aprovada pelo Johan em 2026-11-02): um cinza bem escuro, que
 * não é preto, e a luz da fresta só no que importa (aba ativa, progresso, foco, salvo). A cor dessa
 * luz é a de destaque escolhida em Settings ([accent], âmbar por padrão).
 */
object Look {
    val Ground = Color(0xFF18191B)
    val Surface = Color(0xFF232427)
    val SurfaceHigh = Color(0xFF2C2D31)
    val Text = Color(0xFFF3EFE4)
    val Sub = Color(0xFFA7A8AD)
    /** A cor de destaque ([Accent]); trocar aqui redesenha as telas abertas. */
    var accent by mutableStateOf(Accent.DEFAULT)
    val Glow: Color get() = Color(accent.glow)
    val GlowSoft: Color get() = Color(accent.soft)
    val OnGlow = Color(0xFF18191B)
    val Hairline = Color(0x12F3EFE4)
    val Outline = Color(0x29F3EFE4)
    val Error = Color(0xFFF2A49A)

    /**
     * Vidro das barras, com o desfoque do que passa por baixo: um degradê do fundo, mais opaco na
     * borda da tela (94%) e menos junto da lista (76%). Pedido do Johan, 2026-11-03 (antes, 62%
     * por igual).
     */
    val GlassEdge = Color(0xF018191B)
    val GlassInner = Color(0xC218191B)

    /** Sem desfoque (Android 11 ou mais antigo), a barra fica quase opaca para o texto continuar legível. */
    val GlassFallbackEdge = Color(0xFA18191B)
    val GlassFallbackInner = Color(0xEB18191B)

    /** Avisos e menus flutuantes. */
    val Pill = Color(0xF22E2F33)

    /** Fundo do selo de duração e do marcador do Watch later em cima da miniatura. */
    val Badge = Color(0xC70A0A0C)
    val BadgeSoft = Color(0x8C0A0A0C)
    val Live = Color(0xE6C8372D)

    /**
     * A letra do logotipo, no app inteiro (2026-11-13, com a marca BeamTube): a Outfit, geométrica,
     * em cinco pesos. O contraste entre o peso cheio e o leve é o do nome ("beam" em bold, "tube"
     * em light) e volta nas abas, nos títulos e nos textos de apoio.
     */
    val Outfit = FontFamily(
        Font(R.font.outfit_light, FontWeight.Light),
        Font(R.font.outfit_regular, FontWeight.Normal),
        Font(R.font.outfit_medium, FontWeight.Medium),
        Font(R.font.outfit_semibold, FontWeight.SemiBold),
        Font(R.font.outfit_bold, FontWeight.Bold),
    )

    /** Cores dos avatares sem foto (canais em que o usuário não está inscrito). */
    private val monogramTones = listOf(
        Color(0xFF8C6A45), Color(0xFF6F5A85), Color(0xFF3F7A73),
        Color(0xFF5E7A4C), Color(0xFF4D5D8A), Color(0xFF8A5560),
    )

    fun monogramTone(name: String): Color = monogramTones[Math.floorMod(name.hashCode(), monogramTones.size)]
}

/** A letra do avatar de um canal sem foto: a primeira letra ou número do nome. */
fun monogramOf(name: String?): String =
    name.orEmpty().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
