package ytslim.player

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import ytslim.R

/**
 * A barra de baixo do player (player_controls.xml), de borda a borda. Numa tela estreita (o
 * celular em pé) ela não cabe numa linha: o tempo e a barra sobem para uma linha só deles e os
 * botões ficam embaixo à direita. A activity cuida sozinha dos giros, então [fit] roda ao montar
 * a tela e a cada giro.
 *
 * Na TV ([tv]) são sempre duas linhas (0.5.6, achado pelo Johan na TV): na barra, esquerda e
 * direita avançam o vídeo, então numa linha só os botões depois dela não tinham como receber o
 * foco. Com a barra em cima, baixo leva aos botões e cima volta para a barra. Como sobra espaço
 * na linha dos botões, like, dislike, Save e Subscribe descem para ela, logo depois do tocar
 * (pedido do Johan, 2026-11-03), e o canto de cima fica só com o título.
 *
 * Numa live, o botão LIVE (player_live) toma o lugar do tempo e da duração ([PlayerActivity]).
 */
class PlayerIsland(root: View, private val tv: Boolean) {
    private val barRow: LinearLayout = root.findViewById(R.id.player_bar_row)
    private val buttonRow: LinearLayout = root.findViewById(R.id.player_button_row)
    private val divider: View = root.findViewById(R.id.player_island_divider)
    private val spacer: View = root.findViewById(R.id.player_island_spacer)
    private val time: List<View> = listOf(R.id.exo_position, R.id.exo_progress, R.id.exo_duration)
        .map { root.findViewById<View>(it) }
    private val live: View = root.findViewById(R.id.player_live)

    /** TV: o último botão da linha de baixo que teve o foco ([returnFocus]). */
    private var lastButton: View? = null

    init {
        if (tv) {
            bringActionsDown(root.findViewById(R.id.player_actions))
            buttonRow.viewTreeObserver.addOnGlobalFocusChangeListener { _, focused ->
                if (focused != null && focused.isInside(buttonRow)) lastButton = focused
            }
        }
    }

    /** Tira a pílula de cima e põe as ações soltas na linha dos botões, depois do tocar. */
    private fun bringActionsDown(actions: LinearLayout) {
        (actions.parent as? ViewGroup)?.removeView(actions)
        actions.background = null
        actions.setPadding(0, 0, 0, 0)
        val gap = actions.resources.getDimensionPixelSize(R.dimen.player_actions_gap)
        buttonRow.addView(
            actions,
            1,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = gap },
        )
    }

    /** O tempo e a barra numa linha só deles. null = ainda não arrumou. */
    private var twoRows: Boolean? = null

    fun fit(widthDp: Int) {
        val two = tv || widthDp < COMPACT_WIDTH_DP
        if (two == twoRows) return
        twoRows = two
        time.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        // Numa linha só, o tempo vem logo depois do tocar/pausar.
        if (two) time.forEach { barRow.addView(it) } else time.forEachIndexed { i, view -> buttonRow.addView(view, 1 + i) }
        // O botão LIVE: logo depois do tempo numa linha só; com duas, na linha dos botões depois do
        // tocar, onde o foco da TV chega (na linha da barra, direita avança o vídeo).
        (live.parent as? ViewGroup)?.removeView(live)
        buttonRow.addView(live, if (two) 1 else 1 + time.size)
        barRow.visibility = if (two) View.VISIBLE else View.GONE
        divider.visibility = if (two) View.GONE else View.VISIBLE
        spacer.visibility = if (two) View.VISIBLE else View.GONE
    }

    /**
     * Na TV, a linha dos botões dá a volta (1.0.8, pedido do Johan): esquerda no primeiro (o tocar)
     * vai para o último (a lua), e direita na lua volta para o tocar. Só quando o foco ([focused])
     * está na linha e a seta não acharia outro botão dela. true se o foco deu a volta.
     */
    fun wrapFocus(focused: View, direction: Int): Boolean {
        if (!tv || (direction != View.FOCUS_LEFT && direction != View.FOCUS_RIGHT)) return false
        if (!focused.isInside(buttonRow)) return false
        if (focused.focusSearch(direction)?.isInside(buttonRow) == true) return false
        val buttons = buttonRow.getFocusables(View.FOCUS_FORWARD).filter { it.isShown && it.isEnabled }
        val target = (if (direction == View.FOCUS_RIGHT) buttons.firstOrNull() else buttons.lastOrNull()) ?: return false
        return target !== focused && target.requestFocus()
    }

    /**
     * Na TV, descer de volta para a linha dos botões (da barra de tempo, ou do nome do canal passando
     * por ela) cai no botão que tinha o foco antes de subir (pedido do Johan depois da 1.0.9: o
     * Android escolhia o botão mais perto do meio da barra, o Subscribe). true se o foco foi para ele.
     */
    fun returnFocus(focused: View, direction: Int): Boolean {
        if (!tv || direction != View.FOCUS_DOWN || focused.isInside(buttonRow)) return false
        val next = focused.focusSearch(direction) ?: return false
        if (!next.isInside(buttonRow)) return false
        val last = lastButton?.takeIf { it.isShown && it.isEnabled && it.isFocusable && it.isInside(buttonRow) } ?: return false
        return last !== next && last.requestFocus()
    }

    private fun View.isInside(group: View) = generateSequence(this) { it.parent as? View }.any { it === group }

    companion object {
        /**
         * Abaixo desta largura a ilha vira duas linhas. Sem o botão Skip ahead (2026-11-03) cabe
         * numa linha a partir de 600 dp: o celular pequeno deitado (640 dp) já não perde altura.
         */
        const val COMPACT_WIDTH_DP = 600
    }
}
