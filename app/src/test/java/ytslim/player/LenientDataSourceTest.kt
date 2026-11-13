package ytslim.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceUtil
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@UnstableApi
@RunWith(RobolectricTestRunner::class)
class LenientDataSourceTest {
    private class Failing : DataSource {
        override fun addTransferListener(transferListener: TransferListener) = Unit
        override fun open(dataSpec: DataSpec): Long = throw IOException("HTTP 403")
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw IllegalStateException()
        override fun getUri(): Uri? = null
        override fun close() = Unit
    }

    /** Conta quantas vezes a legenda foi pedida de verdade. */
    private class Counting(private val data: ByteArray) : DataSource {
        private val source = ByteArrayDataSource(data)
        var opened = 0

        override fun addTransferListener(transferListener: TransferListener) = Unit
        override fun open(dataSpec: DataSpec): Long {
            opened++
            return source.open(dataSpec)
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = source.read(buffer, offset, length)
        override fun getUri(): Uri? = source.uri
        override fun close() = source.close()
    }

    private val caption = DataSpec(Uri.parse("https://www.youtube.com/api/timedtext?v=x"))

    @Test
    fun aFailedCaptionDownloadBecomesAnEmptyCaption() {
        val source = LenientDataSource(Failing(), LenientDataSource.emptyCaption("text/vtt"))
        source.open(caption)
        val text = String(DataSourceUtil.readToEnd(source))
        source.close()
        assertEquals("WEBVTT\n\n", text)
    }

    @Test
    fun openingDoesNotWaitForTheDownload() {
        val upstream = Counting("WEBVTT\n\n00:01.000 --> 00:02.000\nHello\n".toByteArray())
        val source = LenientDataSource(upstream, LenientDataSource.emptyCaption("text/vtt"))

        // Abrir não fala com a rede: o vídeo não espera a legenda para começar.
        assertEquals(C.LENGTH_UNSET.toLong(), source.open(caption))
        assertEquals(0, upstream.opened)
        assertEquals(caption.uri, source.uri)

        // A primeira leitura pede a legenda, uma vez só.
        val text = String(DataSourceUtil.readToEnd(source))
        assertEquals(1, upstream.opened)
        assertEquals("WEBVTT\n\n00:01.000 --> 00:02.000\nHello\n", text)
        source.close()
    }

    @Test
    fun aClosedSourceAsksAgainWhenReopened() {
        val upstream = Counting("WEBVTT\n\n".toByteArray())
        val source = LenientDataSource(upstream, LenientDataSource.emptyCaption("text/vtt"))
        source.open(caption)
        DataSourceUtil.readToEnd(source)
        source.close()
        source.open(caption)
        DataSourceUtil.readToEnd(source)
        source.close()
        assertEquals(2, upstream.opened)
    }
}
