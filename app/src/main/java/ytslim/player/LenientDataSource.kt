package ytslim.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Para a legenda: se o download falhar (ex.: 403), entrega uma legenda vazia em vez de erro.
 * Sem isso, uma legenda quebrada derrubaria o vídeo inteiro, porque ela é parte da mesma fonte.
 */
@OptIn(UnstableApi::class)
internal class LenientDataSource(private val upstream: DataSource, private val fallback: ByteArray) : DataSource {
    private var current: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long = try {
        upstream.open(dataSpec).also { current = upstream }
    } catch (e: IOException) {
        runCatching { upstream.close() }
        val empty = ByteArrayDataSource(fallback)
        current = empty
        empty.open(DataSpec(dataSpec.uri))
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        checkNotNull(current).read(buffer, offset, length)

    override fun getUri(): Uri? = current?.uri

    override fun close() {
        current?.close()
        current = null
    }

    class Factory(private val upstream: DataSource.Factory, private val fallback: ByteArray) : DataSource.Factory {
        override fun createDataSource(): DataSource = LenientDataSource(upstream.createDataSource(), fallback)
    }

    companion object {
        /** Uma legenda válida e vazia no formato pedido. */
        fun emptyCaption(mimeType: String?): ByteArray = when {
            mimeType?.contains("ttml") == true -> """<tt xmlns="http://www.w3.org/ns/ttml"><body/></tt>"""
            else -> "WEBVTT\n\n"
        }.toByteArray()
    }
}
