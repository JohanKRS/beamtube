package ytslim.player

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.Rect
import coil3.size.Size
import coil3.transform.Transformation

/**
 * A miniatura da tela de carregando só com as cores (pedido do Johan, 2026-11-14: "sempre ocupando a
 * tela toda, sem nenhum espaço preto [...] e dar um blur bem grandão"): sem as faixas pretas que
 * algumas miniaturas trazem dentro da imagem (a hqdefault de um vídeo 16:9 vem com faixas em cima e
 * embaixo; a de um vídeo em pé, dos lados), pequena e bem desfocada. A tela estica o resultado até as
 * bordas ([PlayerLoading]).
 *
 * Desde a 1.0.8 o desfoque é mais leve (pedido do Johan, 2026-11-15: "o blur não precisa ser tão
 * forte no início e no fim"): as formas grandes da miniatura aparecem, sem dar para ler nada. Na
 * 1.0.7 eram 48 px e raio 7 (o desfoque media uns 16% do lado menor, e sobrava só uma mancha); agora
 * eram 64 px e raio 4, uns 7%. Desde a 1.0.9, mais leve ainda (pedido do Johan: "quero que fique
 * levemente reconhecível o thumbnail quando está carregando"): 96 px e raio 2, uns 2,5%; dá para
 * reconhecer a miniatura, sem os detalhes. A mesma imagem fica no fundo da tela do fim ([EndScreen])
 * e cobre o vídeo ao arrastar para fechar ([PlayerLoading.drag]).
 *
 * Feito à mão, num bitmap pequeno, para sair igual em todo aparelho: o desfoque do Android
 * (RenderEffect) só existe do 12 em diante, e as TVs do Johan são Android 11, 32 bits e fracas. Com
 * [SHORT_SIDE] px no lado menor, são uns 16 mil pontos: o desfoque inteiro custa uns poucos
 * milissegundos, numa thread do carregador de imagens.
 */
internal object PosterBlur {
    /** O lado menor do bitmap desfocado, em px. A tela amplia; desfocado, não aparece serrilhado. */
    const val SHORT_SIDE = 96

    /** O raio de cada passada da média ([PASSES] passadas), em px do bitmap pequeno: o desfoque fica em uns 2,5% do lado menor. */
    const val RADIUS = 2

    /** Três médias seguidas dão um desfoque quase igual ao gaussiano, sem bordas duras. */
    private const val PASSES = 3

    /** Uma linha com no máximo esta fração de pontos claros é faixa preta. */
    private const val BAR_LIT_FRACTION = 0.02f

    /** Um ponto com os três canais até aqui é preto (o JPEG deixa as faixas um pouco acima de zero). */
    private const val BAR_DARK = 26

    /** No máximo isto de cada lado sai como faixa: uma imagem toda escura não some inteira. */
    private const val MAX_BAR_FRACTION = 0.35f

    /** A miniatura pronta para a tela de carregando: sem as faixas, pequena e desfocada. */
    fun blurred(source: Bitmap): Bitmap {
        val content = contentOf(source)
        val small = small(source, content)
        val w = small.width
        val h = small.height
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        blur(pixels, w, h, RADIUS)
        small.setPixels(pixels, 0, w, 0, 0, w, h)
        return small
    }

    /** A parte da miniatura sem as faixas pretas em volta (a imagem inteira, se não tiver). */
    fun contentOf(source: Bitmap): Rect {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        return contentOf(pixels, w, h)
    }

    /** O mesmo, sobre os pontos ARGB da imagem ([contentOf]). */
    fun contentOf(pixels: IntArray, w: Int, h: Int): Rect {
        fun dark(p: Int) = (p shr 16 and 0xFF) <= BAR_DARK && (p shr 8 and 0xFF) <= BAR_DARK && (p and 0xFF) <= BAR_DARK
        fun rowIsBar(y: Int, left: Int, right: Int): Boolean {
            var lit = 0
            for (x in left until right) if (!dark(pixels[y * w + x])) lit++
            return lit <= (right - left) * BAR_LIT_FRACTION
        }
        fun columnIsBar(x: Int, top: Int, bottom: Int): Boolean {
            var lit = 0
            for (y in top until bottom) if (!dark(pixels[y * w + x])) lit++
            return lit <= (bottom - top) * BAR_LIT_FRACTION
        }
        val maxRows = (h * MAX_BAR_FRACTION).toInt()
        val maxColumns = (w * MAX_BAR_FRACTION).toInt()
        var top = 0
        while (top < maxRows && rowIsBar(top, 0, w)) top++
        var bottom = h
        while (h - bottom < maxRows && rowIsBar(bottom - 1, 0, w)) bottom--
        var left = 0
        while (left < maxColumns && columnIsBar(left, top, bottom)) left++
        var right = w
        while (w - right < maxColumns && columnIsBar(right - 1, top, bottom)) right--
        return Rect(left, top, right, bottom)
    }

    /** O pedaço [content] da imagem, reduzido para [SHORT_SIDE] px no lado menor (sem ampliar). */
    private fun small(source: Bitmap, content: Rect): Bitmap {
        val cw = content.width()
        val ch = content.height()
        val scale = minOf(1f, SHORT_SIDE.toFloat() / minOf(cw, ch))
        val w = maxOf(1, (cw * scale).toInt())
        val h = maxOf(1, (ch * scale).toInt())
        val cut = Bitmap.createBitmap(source, content.left, content.top, cw, ch)
        val small = Bitmap.createScaledBitmap(cut, w, h, true)
        if (cut !== source && cut !== small) cut.recycle()
        // Precisa ser um bitmap que se possa mudar (o createScaledBitmap devolve a mesma imagem se o tamanho já servir).
        return if (small.isMutable && small !== source) small else small.copy(Bitmap.Config.ARGB_8888, true)
    }

    /**
     * Desfoca [pixels] (ARGB, [w] x [h]) no lugar: [PASSES] médias de raio [radius], na horizontal e
     * na vertical. Na borda, repete o último ponto (sem escurecer as beiradas).
     */
    fun blur(pixels: IntArray, w: Int, h: Int, radius: Int) {
        if (radius <= 0 || w == 0 || h == 0) return
        val temp = IntArray(pixels.size)
        repeat(PASSES) {
            average(pixels, temp, lines = h, length = w, lineStep = w, step = 1, radius = radius)
            average(temp, pixels, lines = w, length = h, lineStep = 1, step = w, radius = radius)
        }
    }

    /** Uma passada da média em cada linha (ou coluna), com a soma andando junto: o raio não pesa no custo. */
    private fun average(src: IntArray, dst: IntArray, lines: Int, length: Int, lineStep: Int, step: Int, radius: Int) {
        val count = 2 * radius + 1
        val last = length - 1
        for (line in 0 until lines) {
            val start = line * lineStep
            var r = 0
            var g = 0
            var b = 0
            for (k in -radius..radius) {
                val p = src[start + k.coerceIn(0, last) * step]
                r += p shr 16 and 0xFF
                g += p shr 8 and 0xFF
                b += p and 0xFF
            }
            for (i in 0..last) {
                dst[start + i * step] = OPAQUE or (r / count shl 16) or (g / count shl 8) or (b / count)
                val leaving = src[start + (i - radius).coerceAtLeast(0) * step]
                val entering = src[start + (i + radius + 1).coerceAtMost(last) * step]
                r += (entering shr 16 and 0xFF) - (leaving shr 16 and 0xFF)
                g += (entering shr 8 and 0xFF) - (leaving shr 8 and 0xFF)
                b += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }

    private const val OPAQUE = 0xFF shl 24

    /** O [blurred] no carregador de imagens: roda fora da thread da tela, e o resultado fica no cache dele. */
    object Coil : Transformation() {
        override val cacheKey = "beamtube.poster-blur.v3"

        override suspend fun transform(input: Bitmap, size: Size): Bitmap = blurred(input)
    }

    /** A miniatura é decodificada em no máximo isto de cada lado, antes de reduzir e desfocar. */
    const val DECODE_PX = 320

    /** O brilho da miniatura desfocada: 35% mais escura (eram 55% com ela nítida). */
    private const val BRIGHTNESS = 0.65f

    /** As cores 40% mais fortes que as da miniatura, para não ficarem lavadas depois do desfoque. */
    private const val SATURATION = 1.4f

    /**
     * O tom da miniatura desfocada, na tela de carregando e na do fim: um pouco escurecida, para o
     * círculo e os textos aparecerem por cima, e com as cores reforçadas (o desfoque mistura tudo e
     * deixa a imagem acinzentada).
     */
    fun tone(): ColorMatrix = ColorMatrix().apply {
        setSaturation(SATURATION)
        postConcat(ColorMatrix().apply { setScale(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 1f) })
    }
}
