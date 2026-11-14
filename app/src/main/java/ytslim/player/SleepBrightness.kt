package ytslim.player

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/**
 * O brilho escolhido no modo dormir do player (100, 50, 25, 10 ou 0%), guardado só na memória: vale
 * para os próximos vídeos até o app fechar (0.5.6), e o app aberto de novo começa claro.
 *
 * Desde a 1.0.4 (pedido do Johan, vendo vídeos de noite com o brilho em 10%: voltar para a lista
 * ofuscava) o brilho vale também para as telas do app e para os controles do player, numa camada
 * preta por cima da janela inteira. As telas nunca ficam abaixo de [SCREENS_MIN]: com o vídeo em 0%,
 * o resto fica em 10%, para continuar dando para ler e navegar.
 */
object SleepBrightness {
    /** As opções do painel de dormir, em %. */
    val LEVELS = listOf(100, 50, 25, 10, 0)

    /** O brilho mínimo das telas e dos controles. */
    const val SCREENS_MIN = 10

    private val _level = MutableStateFlow(100)
    val level: StateFlow<Int> = _level.asStateFlow()

    fun set(level: Int) {
        _level.value = level.coerceIn(0, 100)
    }

    /** O brilho das telas e dos controles do player quando o vídeo está em [level]. */
    fun screens(level: Int): Int = if (level >= 100) 100 else level.coerceAtLeast(SCREENS_MIN)

    /** A opacidade da camada preta por cima da janela inteira (telas e controles). */
    fun windowDim(level: Int): Float = 1f - screens(level) / 100f

    /**
     * A opacidade da camada que escurece só o vídeo, por baixo dos controles: o que falta, depois
     * da camada da janela, para o vídeo ficar em [level]. Com 0%, ela é preta e o vídeo some.
     */
    fun videoDim(level: Int): Float = 1f - level.coerceIn(0, 100).toFloat() / screens(level)

    /** Põe a camada preta da janela, por cima de tudo o que ela desenha (sem pegar toques nem teclas). */
    fun applyTo(window: Window, level: Int = this.level.value) {
        val alpha = windowDim(level)
        window.decorView.foreground = if (alpha > 0f) ColorDrawable(Color.argb((alpha * 255).roundToInt(), 0, 0, 0)) else null
    }
}
