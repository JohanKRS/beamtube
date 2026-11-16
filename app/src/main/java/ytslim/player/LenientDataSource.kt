package ytslim.player

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ByteArrayDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Para a legenda, que é parte da mesma fonte do vídeo:
 *
 * - se o download falhar (ex.: 403), tenta as [alternates] e, sem nenhuma, entrega uma legenda vazia
 *   em vez de erro. Sem isso, uma legenda quebrada derrubaria o vídeo inteiro;
 * - o download só começa na primeira leitura, e não no [open] (2026-11-13). O Media3 só escolhe as
 *   faixas e começa a baixar o vídeo depois que todas as partes da fonte abrem, e a legenda abria
 *   esperando a resposta do YouTube (uma conexão nova, com o vídeo parado atrás dela). Agora ela
 *   abre na hora, e o texto chega enquanto o vídeo já carrega.
 */
@OptIn(UnstableApi::class)
internal class LenientDataSource(
    private val upstream: DataSource,
    private val fallback: ByteArray,
    /** Para o log (tag BeamTube): quantos bytes chegaram, ou por que falhou. null: sem log. */
    private val label: String? = null,
    /** Outras URLs para tentar, em ordem, se a pedida falhar (ex.: a tradução automática, [ytslim.data.SubtitleRules.fallbacks]). */
    private val alternates: List<Uri> = emptyList(),
) : DataSource {
    /** O que foi pedido no [open], até a primeira leitura. */
    private var pending: DataSpec? = null
    private var current: DataSource? = null
    private var uri: Uri? = null

    override fun addTransferListener(transferListener: TransferListener) = upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        pending = dataSpec
        bytes = 0
        failure = null
        using = null
        uri = dataSpec.uri
        // O tamanho só se sabe na resposta: quem lê vai até o fim dos dados.
        return C.LENGTH_UNSET.toLong()
    }

    /** Bytes lidos desde o [open]. */
    private var bytes = 0L

    /** O download falhou e entrou a legenda vazia: o motivo, para o log. */
    private var failure: String? = null

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val source = current ?: connect(checkNotNull(pending) { "read before open" })
        return source.read(buffer, offset, length).also { if (it > 0 && source === upstream) bytes += it }
    }

    private fun connect(dataSpec: DataSpec): DataSource {
        // A URL pedida e, se ela falhar, as [alternates], uma por uma; sem nenhuma, a legenda vazia.
        val uris = listOf(dataSpec.uri) + alternates
        for ((index, uri) in uris.withIndex()) {
            try {
                upstream.open(if (index == 0) dataSpec else dataSpec.withUri(uri))
                if (index > 0) using = uri
                current = upstream
                return upstream
            } catch (e: IOException) {
                if (failure == null) {
                    failure = (e as? HttpDataSource.InvalidResponseCodeException)?.let { "HTTP ${it.responseCode}" } ?: e.javaClass.simpleName
                }
                runCatching { upstream.close() }
            }
        }
        return ByteArrayDataSource(fallback).also {
            it.open(DataSpec(dataSpec.uri))
            current = it
        }
    }

    /** A alternativa que entrou no lugar da URL pedida, para o log. */
    private var using: Uri? = null

    override fun getUri(): Uri? = current?.uri ?: uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        // Uma legenda que chega vazia (0 bytes) não aparece, e o vídeo segue sem erro: só o log diz.
        if (label != null && current != null) {
            val result = when {
                failure == null -> "$bytes bytes"
                using != null -> "failed ($failure), using ${using?.getQueryParameter("lang") ?: "another"}: $bytes bytes"
                else -> "failed ($failure), shown empty"
            }
            Log.i("BeamTube", "$label: $result")
        }
        current?.close()
        current = null
        pending = null
        uri = null
    }

    class Factory(
        private val upstream: DataSource.Factory,
        private val fallback: ByteArray,
        private val label: String? = null,
        private val alternates: List<Uri> = emptyList(),
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = LenientDataSource(upstream.createDataSource(), fallback, label, alternates)
    }

    companion object {
        /** Uma legenda válida e vazia no formato pedido. */
        fun emptyCaption(mimeType: String?): ByteArray = when {
            mimeType?.contains("ttml") == true -> """<tt xmlns="http://www.w3.org/ns/ttml"><body/></tt>"""
            else -> "WEBVTT\n\n"
        }.toByteArray()
    }
}
