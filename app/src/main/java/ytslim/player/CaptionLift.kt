package ytslim.player

import android.view.View
import kotlin.math.roundToInt

/**
 * A legenda não fica atrás da barra de baixo (pedido do Johan, 2026-11-03). Com os controles
 * abertos ela sobe só o que cobriria da barra (o nome do capítulo e a ilha), e fica acima dela com a
 * mesma margem pequena que tem da borda da tela quando os controles estão fechados (o padding de
 * baixo da legenda, player_captions_bottom). Quando a legenda já termina acima da barra, como no
 * celular em pé com o vídeo no meio, ela não se mexe.
 */
internal class CaptionLift(
    private val captions: View,
    private val bar: List<View>,
) {
    private var open = false

    init {
        val relayout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> if (open) follow(animate = false) }
        bar.forEach { it.addOnLayoutChangeListener(relayout) }
        captions.addOnLayoutChangeListener(relayout)
    }

    /** Os controles abriram ou fecharam. */
    fun setOpen(open: Boolean) {
        this.open = open
        follow(animate = true)
        // Os controles acabaram de aparecer: a barra ainda pode não ter medida.
        if (open) captions.post { if (this.open) follow(animate = false) }
    }

    /** Quanto a legenda sobe agora (em px, para cima): zero com os controles fechados. */
    fun lift(): Int = if (open) overlap() else 0

    private fun follow(animate: Boolean) {
        val target = -lift().toFloat()
        captions.animate().cancel()
        if (animate && captions.isLaidOut) {
            captions.animate().translationY(target).setDuration(DURATION_MS).start()
        } else {
            captions.translationY = target
        }
    }

    /** O quanto a área da legenda (sem contar a subida) passa do topo da barra. */
    private fun overlap(): Int {
        val parent = captions.parent as? View ?: return 0
        val barTop = bar.filter { it.height > 0 && visible(it) }.minOfOrNull { top(it) } ?: return 0
        val captionsBottom = top(parent) + captions.bottom
        return (captionsBottom - barTop).roundToInt().coerceAtLeast(0)
    }

    private fun lineage(view: View) = generateSequence(view) { it.parent as? View }

    private fun visible(view: View) = lineage(view).all { it.visibility == View.VISIBLE }

    /** O topo da view na raiz da tela, com as subidas (translationY) de quem está acima dela. */
    private fun top(view: View) = lineage(view).sumOf { (it.top + it.translationY).toDouble() }.toFloat()

    private companion object {
        const val DURATION_MS = 180L
    }
}
