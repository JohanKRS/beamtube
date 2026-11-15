package ytslim.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
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

    /**
     * A camada só do vídeo fica na tela em todo brilho abaixo de 100%, mesmo quando não escurece nada
     * ([videoDim] zero, de 50% a 10%): a faixa clara no pé do vídeo (achado do Johan na TV, 1.0.8 e
     * 1.0.9). O vídeo é uma camada (SurfaceView) por baixo da janela, e o Android só junta a janela
     * por cima dele onde alguma view desenha; no resto, a janela vale como vazia e nem é mostrada,
     * com a camada preta junto. Por cima do vídeo ficava só a legenda, que não chega ao pé da tela (a
     * margem de baixo dela e a subida acima da barra): ali o vídeo aparecia com o brilho normal. A
     * anotação muda a cada layout, então dependia de os controles estarem na tela: no primeiro vídeo,
     * com o brilho escolhido no painel, a barra de baixo cobria o pé; nos seguintes, os controles não
     * abrem sozinhos com o brilho baixo. Na tela, esta camada cobre o vídeo inteiro.
     */
    fun coversVideo(level: Int): Boolean = level < 100

    /** Põe a camada preta da janela, por cima de tudo o que ela desenha (sem pegar toques nem teclas). */
    fun applyTo(window: Window, level: Int = this.level.value) {
        val alpha = windowDim(level)
        window.decorView.foreground = if (alpha > 0f) Dim(Color.argb((alpha * 255).roundToInt(), 0, 0, 0)) else null
    }

    /**
     * A camada preta: pinta tudo o que a janela desenha, sem depender dos limites dela (1.0.8): o
     * player abre já com o brilho escolhido, e os limites são medidos antes de a janela chegar ao
     * tamanho final. Não era isso que deixava a faixa clara no pé do vídeo ([coversVideo]).
     */
    internal class Dim(val color: Int) : Drawable() {
        override fun draw(canvas: Canvas) = canvas.drawColor(color)

        override fun setAlpha(alpha: Int) = Unit

        override fun setColorFilter(colorFilter: ColorFilter?) = Unit

        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
    }
}
