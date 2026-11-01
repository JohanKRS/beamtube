package ytslim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.floor

/** Um QR code de [text], quadrado, em preto no branco (com a margem que os leitores pedem). */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    // Largura 0: um pixel por módulo; a escala fica por conta do desenho.
    val matrix = remember(text) {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, mapOf(EncodeHintType.MARGIN to 2))
    }
    Canvas(modifier.aspectRatio(1f)) {
        drawRect(Color.White)
        // Módulos com tamanho inteiro de pixels, para não aparecer fresta entre eles.
        val cell = floor(size.minDimension / matrix.width).coerceAtLeast(1f)
        val origin = Offset((size.width - cell * matrix.width) / 2, (size.height - cell * matrix.height) / 2)
        val module = Size(cell, cell)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) drawRect(Color.Black, origin + Offset(x * cell, y * cell), module)
            }
        }
    }
}
