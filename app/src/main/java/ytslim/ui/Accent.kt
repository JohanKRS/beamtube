package ytslim.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import ytslim.R

/**
 * A cor de destaque do Doorglow: a aba ativa, o progresso, o foco da TV, os botões ligados e a
 * barra do player. Escolhida em Settings, como a cor do ícone (pedido do Johan, 2026-11-03); o
 * âmbar da porta é o padrão. Todas são claras o bastante para o texto escuro em cima delas
 * ([Look.OnGlow]) e para aparecer no cinza do fundo.
 *
 * [glow] é a cor cheia; [soft], a versão clara (contorno do foco na TV, ícones ligados).
 * [playerStyle] é o tema que o player (Views) aplica para os drawables dele usarem a mesma cor.
 */
enum class Accent(@StringRes val label: Int, val glow: Long, val soft: Long, @StyleRes val playerStyle: Int) {
    AMBER(R.string.accent_amber, 0xFFFFC861, 0xFFFFE3A3, R.style.PlayerAccent_Amber),
    RED(R.string.accent_red, 0xFFFF6B5F, 0xFFFFB4AC, R.style.PlayerAccent_Red),
    PINK(R.string.accent_pink, 0xFFFF8CC6, 0xFFFFC7E3, R.style.PlayerAccent_Pink),
    PURPLE(R.string.accent_purple, 0xFFC3A2FF, 0xFFE2D3FF, R.style.PlayerAccent_Purple),
    BLUE(R.string.accent_blue, 0xFF7CB6FF, 0xFFC2DCFF, R.style.PlayerAccent_Blue),
    TEAL(R.string.accent_teal, 0xFF5FD3C3, 0xFFB3EDE5, R.style.PlayerAccent_Teal),
    GREEN(R.string.accent_green, 0xFF97D97E, 0xFFCDEDC0, R.style.PlayerAccent_Green),
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
