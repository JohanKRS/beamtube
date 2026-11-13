package ytslim.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import ytslim.R

/**
 * A cor de destaque do BeamTube: a aba ativa, o progresso, o foco da TV, os botões ligados e a
 * barra do player. Escolhida em Settings, como a cor do ícone (pedido do Johan, 2026-11-03).
 *
 * São as cores do ícone, na mesma ordem ([ytslim.data.AppIcon]); onde o ícone é escuro demais para
 * aparecer no cinza do fundo, um tom próximo e mais claro (pedido do Johan, 2026-11-03): o âmbar é a
 * luz da fresta do ícone azul-noite (o padrão), o prata faz par com o preto, e o verde-azulado, a
 * ameixa e a mostarda são o fundo do ícone clareado. O "vivo" é o meio do degradê do ícone, e o
 * vermelho é o do YouTube. Todas aparecem no fundo e aceitam o texto escuro em cima
 * ([Look.OnGlow]), com contraste de pelo menos 4,4:1.
 *
 * [glow] é a cor cheia; [soft], a versão clara (contorno do foco na TV, ícones ligados).
 * [playerStyle] é o tema que o player (Views) aplica para os drawables dele usarem a mesma cor.
 */
enum class Accent(@StringRes val label: Int, val glow: Long, val soft: Long, @StyleRes val playerStyle: Int) {
    AMBER(R.string.accent_amber, 0xFFFFC861, 0xFFFFE3A3, R.style.PlayerAccent_Amber),
    SILVER(R.string.accent_silver, 0xFFBDC7DB, 0xFFDEE3ED, R.style.PlayerAccent_Silver),
    TEAL(R.string.accent_teal, 0xFF3BBFCE, 0xFF9DDFE6, R.style.PlayerAccent_Teal),
    PLUM(R.string.accent_plum, 0xFFCC90D5, 0xFFE6C8EA, R.style.PlayerAccent_Plum),
    MUSTARD(R.string.accent_mustard, 0xFFDD962C, 0xFFEECB95, R.style.PlayerAccent_Mustard),
    VIVID(R.string.accent_vivid, 0xFFE873C7, 0xFFF3B9E3, R.style.PlayerAccent_Vivid),
    RED(R.string.accent_red, 0xFFFF0000, 0xFFFF8080, R.style.PlayerAccent_Red),
    ;

    companion object {
        val DEFAULT = AMBER
    }
}

/** Guarda a cor escolhida no aparelho e a põe em [Look.accent] ao abrir o app. */
object Accents {
    private const val KEY = "accent"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("look", Context.MODE_PRIVATE)
        Look.accent = prefs.getString(KEY, null)?.let { name -> Accent.entries.firstOrNull { it.name == name } } ?: Accent.DEFAULT
    }

    fun select(accent: Accent) {
        Look.accent = accent
        if (::prefs.isInitialized) prefs.edit().putString(KEY, accent.name).apply()
    }
}
