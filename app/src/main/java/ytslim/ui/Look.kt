package ytslim.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
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
    /**
     * O tom do fundo e das superfícies, escolhido em Settings > Background ([Backgrounds]; o padrão
     * é o do aparelho: [Tones.PHONE] no celular e no tablet, [Tones.TV] na TV). Trocar aqui redesenha
     * as telas abertas.
     */
    var tones by mutableStateOf(Tones.PHONE)

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
 * etapa 6 (pedido do Johan, 2026-11-02: não é preto). A TV tem um mais escuro ([TV]).
 */
@Immutable
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
         * Na TV, quase preto (escolha do Johan entre quatro tons, 2026-11-15). A mesma cor do
         * celular aparecia bem mais clara na TV: a tela LCD, com a luz de fundo sempre acesa, e os
         * modos de imagem clareiam os cinzas escuros que o OLED do celular quase apaga. As
         * superfícies sobem do fundo no mesmo passo das do celular (+11, +20, +22 e +16).
         */
        val TV = Tones(
            ground = Color(0xFF0A0A0C),
            surface = Color(0xFF151518),
            surfaceHigh = Color(0xFF1E1E22),
            pill = Color(0xF2202024),
            floating = Color(0xF51A1A1D),
        )

        /**
         * Preto puro (1.0.8, pedido do Johan: "um preto 100% OLED"): nas telas OLED, o fundo apaga
         * os pixels. Perto do preto, o mesmo passo das outras deixaria os cartões quase invisíveis;
         * aqui eles sobem um pouco mais, para ficarem tão visíveis quanto no celular.
         */
        val OLED = Tones(
            ground = Color(0xFF000000),
            surface = Color(0xFF0F0F11),
            surfaceHigh = Color(0xFF19191C),
            pill = Color(0xF21B1B1E),
            floating = Color(0xF5141416),
        )
    }
}

/**
 * O fundo do app, à escolha em Settings em todo aparelho (1.0.8, pedido do Johan: "deixe essa cor
 * que está agora no mobile e a cor da TV como opcional em todas as instalações, e um preto 100%
 * OLED também"). Cada aparelho começa no dele, como até a 1.0.7.
 */
enum class Background(@StringRes val label: Int, val tones: Tones) {
    GRAPHITE(R.string.background_graphite, Tones.PHONE),
    NEAR_BLACK(R.string.background_near_black, Tones.TV),
    BLACK(R.string.background_black, Tones.OLED),
    ;

    companion object {
        /** O de cada aparelho, enquanto nada foi escolhido. */
        fun default(tv: Boolean): Background = if (tv) NEAR_BLACK else GRAPHITE
    }
}

/**
 * Guarda o fundo escolhido no aparelho (preferências "look", como a cor de destaque: vale para
 * todos os usuários) e o põe em [Look.tones], que redesenha as telas na hora.
 */
object Backgrounds {
    private const val KEY = "background"
    private var prefs: SharedPreferences? = null
    private var tv = false

    var current by mutableStateOf(Background.GRAPHITE)
        private set

    fun init(context: Context, tv: Boolean) {
        this.tv = tv
        val stored = context.getSharedPreferences("look", Context.MODE_PRIVATE).also { prefs = it }.getString(KEY, null)
        apply(Background.entries.firstOrNull { it.name == stored } ?: Background.default(tv))
    }

    fun select(background: Background) {
        apply(background)
        prefs?.edit()?.putString(KEY, background.name)?.apply()
    }

    /** O padrão do aparelho, sem gravar nada (para as fotos de tela). */
    fun reset(tv: Boolean) {
        this.tv = tv
        apply(Background.default(tv))
    }

    private fun apply(background: Background) {
        current = background
        Look.tones = background.tones
    }
}
