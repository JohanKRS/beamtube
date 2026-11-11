package ytslim.player

import androidx.media3.common.Player
import kotlin.math.roundToInt

/**
 * O mini player flutuante do celular e do tablet (picture-in-picture, pedido do Johan, 2026-11-11):
 * sair do app com o vídeo tocando deixa o vídeo numa janelinha por cima dos outros apps
 * ([PlayerActivity]).
 */
internal object MiniPlayerRules {
    /**
     * Sair do app abre o mini player quando o vídeo está tocando (ou carregando para tocar).
     * Pausado, no fim ou com erro, sair pausa, como antes; fechando o player (voltar), também não abre.
     */
    fun floats(touch: Boolean, playWhenReady: Boolean, playbackState: Int, finishing: Boolean): Boolean =
        touch && playWhenReady && !finishing &&
            playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED

    /**
     * A forma do mini player (largura, altura): a do vídeo, dentro do que o Android aceita (de
     * 1:2,39 a 2,39:1, senão ele recusa). Antes de o vídeo chegar, 16:9.
     */
    fun shape(width: Int, height: Int, pixelRatio: Float): Pair<Int, Int> {
        if (width <= 0 || height <= 0) return 16 to 9
        val shownWidth = (width * (pixelRatio.takeIf { it > 0f } ?: 1f)).roundToInt().coerceAtLeast(1)
        return when {
            shownWidth * 100 > height * WIDEST -> WIDEST to 100
            height * 100 > shownWidth * WIDEST -> 100 to WIDEST
            else -> shownWidth to height
        }
    }

    /** 2,39:1, em centésimos: o mais largo (e, deitado, o mais alto) que o Android aceita. */
    private const val WIDEST = 239
}
