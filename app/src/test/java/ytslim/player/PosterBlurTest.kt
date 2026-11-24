package ytslim.player

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * A miniatura da tela de carregando (pedido do Johan, 2026-11-14): sem as faixas pretas que vêm
 * dentro de algumas miniaturas, e bem desfocada, só com as cores.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PosterBlurTest {
    /** Uma miniatura [w] x [h] com a imagem em [content] (metade laranja, metade azul) e preto em volta. */
    private fun thumbnail(w: Int, h: Int, content: Rect): Bitmap {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val half = content.left + content.width() / 2
        canvas.drawRect(Rect(content.left, content.top, half, content.bottom), Paint().apply { color = ORANGE })
        canvas.drawRect(Rect(half, content.top, content.right, content.bottom), Paint().apply { color = BLUE })
        return bitmap
    }

    private fun bright(p: Int) = maxOf(Color.red(p), Color.green(p), Color.blue(p))

    @Test
    fun theBarsOfAWideVideoInA4By3ThumbnailGo() {
        // A hqdefault (480x360) de um vídeo 16:9: faixas de 45 px em cima e embaixo.
        val content = Rect(0, 45, 480, 315)
        assertEquals(content, PosterBlur.contentOf(thumbnail(480, 360, content)))
    }

    @Test
    fun theBarsOfAnUprightVideoGoToo() {
        // Um vídeo em pé dentro da hqdefault: faixas dos lados.
        val content = Rect(139, 0, 341, 360)
        assertEquals(content, PosterBlur.contentOf(thumbnail(480, 360, content)))
    }

    @Test
    fun aThumbnailWithoutBarsStaysWhole() {
        val whole = Rect(0, 0, 320, 180)
        assertEquals(whole, PosterBlur.contentOf(thumbnail(320, 180, whole)))
    }

    @Test
    fun aDarkThumbnailIsNotCutAway() {
        // Toda escura: no máximo 35% de cada lado sai, e sobra o meio.
        val dark = Bitmap.createBitmap(480, 360, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(8, 8, 10)) }
        val kept = PosterBlur.contentOf(dark)
        assertTrue(kept.width() >= 480 * 0.3 - 1 && kept.height() >= 360 * 0.3 - 1)
    }

    @Test
    fun theBlurredPosterIsSmallSoftAndHasNoBlackEdges() {
        val blurred = PosterBlur.blurred(thumbnail(480, 360, Rect(0, 45, 480, 315)))
        assertEquals(PosterBlur.SHORT_SIDE, minOf(blurred.width, blurred.height))
        // 16:9 de novo, sem as faixas.
        assertEquals(480f / 270f, blurred.width.toFloat() / blurred.height, 0.05f)
        // Nenhuma beirada escura (as faixas não entram no desfoque).
        for (x in 0 until blurred.width) {
            assertTrue(bright(blurred.getPixel(x, 0)) > 100)
            assertTrue(bright(blurred.getPixel(x, blurred.height - 1)) > 100)
        }
        // A divisa entre o laranja e o azul vira uma passagem suave: no meio, uma mistura dos dois.
        val middle = blurred.getPixel(blurred.width / 2, blurred.height / 2)
        assertTrue(Color.red(middle) in 60..200 && Color.blue(middle) in 60..200)
        // As pontas ficam com a cor de cada lado.
        assertTrue(Color.red(blurred.getPixel(0, blurred.height / 2)) > 200)
        assertTrue(Color.blue(blurred.getPixel(blurred.width - 1, blurred.height / 2)) > 200)
    }

    @Test
    fun oneColorStaysThatColor() {
        val pixels = IntArray(40 * 30) { ORANGE }
        PosterBlur.blur(pixels, 40, 30, PosterBlur.RADIUS)
        assertTrue(pixels.all { it == ORANGE })
    }

    @Test
    fun bitmapContentOfMatchesPixelArrayVersion() {
        val cases = listOf(
            Rect(0, 45, 480, 315),
            Rect(139, 0, 341, 360),
            Rect(40, 30, 280, 210),
            Rect(0, 0, 320, 180),
        )
        for (rect in cases) {
            val w = if (rect.right > 320) 480 else 320
            val h = if (rect.bottom > 240) 360 else if (rect.bottom > 180) 240 else 180
            val bmp = thumbnail(w, h, rect)
            val pixels = IntArray(w * h).also { bmp.getPixels(it, 0, w, 0, 0, w, h) }
            assertEquals(PosterBlur.contentOf(pixels, w, h), PosterBlur.contentOf(bmp))
        }
    }

    @Test
    fun lutBlurMatchesDirectDivisionReferenceBitForBit() {
        val w = 37
        val h = 29
        val input = IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            Color.rgb((x * 7 + y * 13) and 0xFF, (x * 19 + y * 3) and 0xFF, (x * 5 + y * 29) and 0xFF)
        }
        val actual = input.copyOf()
        PosterBlur.blur(actual, w, h, PosterBlur.RADIUS)

        // Referência direta com divisão inteira r/count, g/count, b/count.
        val expected = input.copyOf()
        val temp = IntArray(expected.size)
        val radius = PosterBlur.RADIUS
        val count = 2 * radius + 1
        fun refPass(src: IntArray, dst: IntArray, lines: Int, length: Int, lineStep: Int, step: Int) {
            val last = length - 1
            for (line in 0 until lines) {
                val start = line * lineStep
                for (i in 0..last) {
                    var r = 0; var g = 0; var b = 0
                    for (k in -radius..radius) {
                        val p = src[start + (i + k).coerceIn(0, last) * step]
                        r += p shr 16 and 0xFF; g += p shr 8 and 0xFF; b += p and 0xFF
                    }
                    dst[start + i * step] = (0xFF shl 24) or (r / count shl 16) or (g / count shl 8) or (b / count)
                }
            }
        }
        repeat(3) {
            refPass(expected, temp, h, w, w, 1)
            refPass(temp, expected, w, h, 1, w)
        }
        assertTrue(expected.contentEquals(actual))
    }

    private companion object {
        val ORANGE = Color.rgb(240, 140, 30)
        val BLUE = Color.rgb(30, 90, 230)
    }
}
