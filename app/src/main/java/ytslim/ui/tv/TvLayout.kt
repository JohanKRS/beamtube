package ytslim.ui.tv

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ytslim.R

/**
 * Como o início da TV se organiza (pedido do Johan, 2026-11-14): as abas na fileira de cima, com o
 * logotipo (o padrão, como até a 1.0.6), ou o menu lateral do tablet deitado, com os ícones das abas
 * à esquerda e o nome da aba aberta em cima.
 */
enum class TvLayout(@StringRes val label: Int) {
    TOP_TABS(R.string.tv_layout_top_tabs),
    SIDE_MENU(R.string.tv_layout_side_menu),
}

/**
 * Guarda a escolha no aparelho (preferências "look", como o idioma e a cor de destaque) e a deixa em
 * [current], que redesenha o início na hora em que muda.
 */
object TvLayouts {
    private const val KEY = "tv_layout"
    private var prefs: SharedPreferences? = null

    var current by mutableStateOf(TvLayout.TOP_TABS)
        private set

    fun init(context: Context) {
        val stored = context.getSharedPreferences("look", Context.MODE_PRIVATE).also { prefs = it }.getString(KEY, null)
        current = TvLayout.entries.firstOrNull { it.name == stored } ?: TvLayout.TOP_TABS
    }

    fun select(layout: TvLayout) {
        current = layout
        prefs?.edit()?.putString(KEY, layout.name)?.apply()
    }
}
