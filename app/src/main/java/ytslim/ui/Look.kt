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
    /** O tom do fundo e das superfícies; a TV troca pelo dela ao abrir ([Tones]). */
    var tones = Tones.PHONE

    val Ground: Color get() = tones.ground
    val Surface: Color get() = tones.surface
    val SurfaceHigh: Color get() = tones.surfaceHigh
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
    val GlassEdge: Color get() = Ground.copy(alpha = 0xF0 / 255f)
    val GlassInner: Color get() = Ground.copy(alpha = 0xC2 / 255f)

    /** Sem desfoque (Android 11 ou mais antigo), a barra fica quase opaca para o texto continuar legível. */
    val GlassFallbackEdge: Color get() = Ground.copy(alpha = 0xFA / 255f)
    val GlassFallbackInner: Color get() = Ground.copy(alpha = 0xEB / 255f)

    /** Avisos e menus flutuantes. */
    val Pill: Color get() = tones.pill

    /** Menus e avisos da TV, que não tem vidro. */
    val Floating: Color get() = tones.floating

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

/**
 * O fundo e as superfícies, do mais escuro ao mais claro. O do celular é o cinza bem escuro da
 * etapa 6 (pedido do Johan, 2026-11-02: não é preto).
 */
class Tones(
    val ground: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val pill: Color,
    val floating: Color,
) {
    companion object {
        val PHONE = Tones(
            ground = Color(0xFF18191B),
            surface = Color(0xFF232427),
            surfaceHigh = Color(0xFF2C2D31),
            pill = Color(0xF22E2F33),
            floating = Color(0xF528292D),
        )

        /**
         * Um tom com as superfícies no mesmo passo das do celular: o cartão 11 acima do fundo, o
         * destaque 20, o menu da TV 16 e o aviso 22.
         */
        fun from(ground: Int): Tones {
            fun lift(step: Int, alpha: Int = 0xFF): Color {
                val r = (ground shr 16 and 0xFF) + step
                val g = (ground shr 8 and 0xFF) + step
                val b = (ground and 0xFF) + step + step / 10
                return Color(alpha shl 24 or (r shl 16) or (g shl 8) or b)
            }
            return Tones(lift(0), lift(11), lift(20), lift(22, 0xF2), lift(16, 0xF5))
        }
    }
}
