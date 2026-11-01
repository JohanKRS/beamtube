package ytslim.player

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
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

    @Test
    fun aFailedCaptionDownloadBecomesAnEmptyCaption() {
        val source = LenientDataSource(Failing(), LenientDataSource.emptyCaption("text/vtt"))
        val length = source.open(DataSpec(Uri.parse("https://www.youtube.com/api/timedtext?v=x")))
        val buffer = ByteArray(length.toInt())
        source.read(buffer, 0, buffer.size)
        source.close()
        assertEquals("WEBVTT\n\n", String(buffer))
    }
}
