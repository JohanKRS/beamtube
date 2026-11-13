package ytslim.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * O ícone e o banner da TV de cada cor abrem pelo carregador de desenhos do próprio Android, como o
 * launcher faz (os recursos do app, sem tema). As fotos de tela usam o leitor do Compose, que aceita
 * o que o Android recusa: na 1.0.1, um número malformado no caminho ("--1.15463e-14") fazia o banner
 * falhar fora do app, e a TV ficava sem o cartão do BeamTube na tela inicial.
 */
@RunWith(RobolectricTestRunner::class)
class LauncherArtTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun load(type: String, name: String): Drawable {
        val res = context.packageManager.getResourcesForApplication(context.packageName)
        val id = res.getIdentifier(name, type, context.packageName)
        assertTrue("$type/$name não existe", id != 0)
        return res.getDrawable(id, null)
    }

    /** Desenha e confere que saiu um desenho (não um quadro vazio nem de uma cor só). */
    private fun assertDraws(drawable: Drawable, width: Int, height: Int, what: String) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(Canvas(bitmap))
        val pixels = IntArray(width * height).also { bitmap.getPixels(it, 0, width, 0, 0, width, height) }
        assertTrue("$what saiu vazio", pixels.distinct().size > 50)
    }

    private fun assertAllOpen() {
        AppIcon.entries.forEach { icon ->
            val key = icon.name.lowercase()
            assertDraws(load("drawable", "tv_banner_$key"), 640, 360, "tv_banner_$key")
            assertDraws(load("mipmap", "ic_launcher_$key"), 192, 192, "ic_launcher_$key")
        }
    }

    /** A Hisense do Johan. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [30], qualifiers = "television-xhdpi")
    fun android11Tv() = assertAllOpen()

    /** A TCL do Johan. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [34], qualifiers = "television-xhdpi")
    fun android14Tv() = assertAllOpen()

    /** O celular. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(sdk = [35], qualifiers = "xxhdpi")
    fun android15Phone() = assertAllOpen()

    /** O Android 6 usa os desenhos chapados (drawable/, sem degradê), com o leitor antigo, em Java. */
    @Test
    @Config(sdk = [23], qualifiers = "xhdpi")
    fun android6FlatVersionsParse() = AppIcon.entries.forEach { icon ->
        val key = icon.name.lowercase()
        load("drawable", "tv_banner_$key")
        load("drawable", "ic_launcher_${key}_foreground")
    }
}
