package ytslim.player

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import ytslim.R

/**
 * A ilha de baixo do player (player_controls.xml). Numa tela estreita (o celular em pé) ela não
 * cabe numa linha: o tempo e a barra sobem para uma linha só deles, os botões ficam embaixo à
 * direita e o Skip ahead fica só com o ícone. A activity cuida sozinha dos giros, então [fit]
 * roda ao montar a tela e a cada giro.
 */
class PlayerIsland(root: View) {
    private val barRow: LinearLayout = root.findViewById(R.id.player_bar_row)
    private val buttonRow: LinearLayout = root.findViewById(R.id.player_button_row)
    private val divider: View = root.findViewById(R.id.player_island_divider)
    private val spacer: View = root.findViewById(R.id.player_island_spacer)
    private val skip: TextView = root.findViewById(R.id.player_skip)
    private val label = skip.text
    private val time: List<View> = listOf(R.id.exo_position, R.id.exo_progress, R.id.exo_duration)
        .map { root.findViewById<View>(it) }
    private val density = root.resources.displayMetrics.density

    var compact = false
        private set

    init {
        // Sem o texto, o leitor de tela ainda diz o que o botão faz.
        skip.contentDescription = label
    }

    fun fit(widthDp: Int) {
        val narrow = widthDp < COMPACT_WIDTH_DP
        if (narrow == compact) return
        compact = narrow
        time.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        // Numa linha só, o tempo vem logo depois do tocar/pausar.
        if (narrow) time.forEach { barRow.addView(it) } else time.forEachIndexed { i, view -> buttonRow.addView(view, 1 + i) }
        barRow.visibility = if (narrow) View.VISIBLE else View.GONE
        divider.visibility = if (narrow) View.GONE else View.VISIBLE
        spacer.visibility = if (narrow) View.VISIBLE else View.GONE
        skip.text = if (narrow) null else label
        skip.compoundDrawablePadding = if (narrow) 0 else dp(6)
        skip.setPaddingRelative(if (narrow) dp(8) else dp(12), 0, if (narrow) dp(8) else dp(14), 0)
    }

    private fun dp(value: Int): Int = (value * density).toInt()

    companion object {
        /** Abaixo desta largura a ilha vira duas linhas (um tablet grande em pé ainda cabe numa). */
        const val COMPACT_WIDTH_DP = 720
    }
}
