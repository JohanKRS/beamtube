package ytslim.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Para a legenda, que é parte da mesma fonte do vídeo:
 *
 * - se o download falhar (ex.: 403), entrega uma legenda vazia em vez de erro. Sem isso, uma legenda
 *   quebrada derrubaria o vídeo inteiro;
 * - o download só começa na primeira leitura, e não no [open] (2026-11-13). O Media3 só escolhe as
 *   faixas e começa a baixar o vídeo depois que todas as partes da fonte abrem, e a legenda abria
 *   esperando a resposta do YouTube (uma conexão nova, com o vídeo parado atrás dela). Agora ela
 *   abre na hora, e o texto chega enquanto o vídeo já carrega.
 */
@OptIn(UnstableApi::class)
internal class LenientDataSource(private val upstream: DataSource, private val fallback: ByteArray) : DataSource {
    /** O que foi pedido no [open], até a primeira leitura. */
    private var pending: DataSpec? = null
    private var current: DataSource? = null
    private var uri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        pending = dataSpec
        uri = dataSpec.uri
        // O tamanho só se sabe na resposta: quem lê vai até o fim dos dados.
        return C.LENGTH_UNSET.toLong()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val source = current ?: connect(checkNotNull(pending) { "read before open" })
        return source.read(buffer, offset, length)
    }

    private fun connect(dataSpec: DataSpec): DataSource {
        val source = try {
            upstream.open(dataSpec)
            upstream
        } catch (e: IOException) {
            runCatching { upstream.close() }
            ByteArrayDataSource(fallback).also { it.open(DataSpec(dataSpec.uri)) }
        }
        current = source
        return source
    }

    override fun getUri(): Uri? = current?.uri ?: uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        current?.close()
        current = null
        pending = null
        uri = null
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
