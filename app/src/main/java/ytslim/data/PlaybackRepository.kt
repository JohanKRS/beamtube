package ytslim.data

import android.os.SystemClock
import android.util.Log
import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

/** O que o player precisa para tocar um vídeo. */
sealed interface Stream {
    /** MPD gerado pelo motor a partir dos formatos adaptativos (vídeos normais). */
    class DashManifest(val xml: ByteArray) : Stream

    /** Manifest DASH do próprio YouTube (lives). */
    class DashUrl(val url: String) : Stream

    /** Manifest HLS do próprio YouTube (lives). */
    class HlsUrl(val url: String) : Stream

    /** Último recurso: arquivo único (o YouTube só entrega assim em 360p). */
    class Progressive(val url: String) : Stream
}

data class PlaybackInfo(
    val videoId: String,
    /** O cliente de TV do YouTube (o primeiro que o app usa) manda o vídeo sem título nem autor. */
    val title: String?,
    val author: String?,
    val channelId: String?,
    val isLive: Boolean,
    /** Rótulo da faixa de áudio original como o motor escreve no MPD, ex. "en‐US (original)". */
    val originalAudioLabel: String?,
    val audioLabels: List<String>,
    val clientName: String?,
    val captions: List<Caption>,
    /** As miniaturas do preview ao arrastar a barra; null se o cliente do YouTube não mandou. */
    val storyboard: Storyboard? = null,
)

sealed interface PlaybackResult {
    data class Ready(val info: PlaybackInfo, val stream: Stream, val userAgent: String) : PlaybackResult
    data class Unplayable(val reason: String?) : PlaybackResult
}

/** Obtém os streams de um vídeo pelo motor e escolhe o formato que o Media3 toca. */
object PlaybackRepository {
    private const val TAG = "Doorglow"

    /** Quantos clientes do YouTube tentar quando um só entrega formatos que não tocamos. */
    private const val MAX_CLIENT_SWITCHES = 6

    /** Quanto tempo um vídeo pedido no toque ([prefetch]) espera a tela do player pegá-lo. */
    private const val PREFETCH_MAX_AGE_MS = 60_000L

    val userAgent: String get() = DefaultHeaders.APP_USER_AGENT

    /** O vídeo pedido no toque, enquanto a tela do player abre. */
    private class Prefetch(val videoId: String, val atMs: Long, val result: Deferred<PlaybackResult>)

    private var prefetched: Prefetch? = null

    /**
     * Começa a pedir o vídeo ao motor já no toque do card: abrir a tela do player leva de 0,2 a
     * 0,5 s (mais na TV), e o pedido corre nesse tempo. O [load] seguinte do mesmo vídeo usa este.
     */
    fun prefetch(videoId: String) {
        val job = AppScope.async { fetch(videoId) }
        synchronized(this) {
            prefetched?.result?.cancel()
            prefetched = Prefetch(videoId, SystemClock.elapsedRealtime(), job)
        }
    }

    /**
     * Pede o vídeo ao motor ou, se ele foi pedido no toque há pouco ([prefetch]), espera aquele
     * pedido. O adiantado vale uma vez só: um novo load (ex.: depois de trocar de cliente) pede de novo.
     */
    suspend fun load(videoId: String): PlaybackResult {
        val early = synchronized(this) {
            prefetched?.takeIf { it.videoId == videoId }?.also { prefetched = null }
        }?.takeIf { SystemClock.elapsedRealtime() - it.atMs < PREFETCH_MAX_AGE_MS }
        if (early != null) {
            try {
                val waitedFrom = SystemClock.elapsedRealtime()
                return early.result.await().also {
                    Log.i(TAG, "video $videoId: requested early, waited ${SystemClock.elapsedRealtime() - waitedFrom} ms more")
                }
            } catch (e: CancellationException) {
                // Se quem foi cancelado é o pedido adiantado (outro toque), e não quem espera, pede de novo.
                if (!currentCoroutineContext().isActive) throw e
            }
        }
        return fetch(videoId)
    }

    /**
     * Pede o vídeo ao motor, trocando de cliente do YouTube até um entregar DASH ou HLS. Um
     * cliente que só entrega o arquivo único (360p) fica guardado como último recurso: antes,
     * o app tocava esse arquivo assim que aparecia, mesmo com outro cliente entregando 1080p.
     */
    private suspend fun fetch(videoId: String): PlaybackResult = io {
        var lastReason: String? = null
        var lowQuality: PlaybackResult.Ready? = null
        repeat(MAX_CLIENT_SWITCHES) {
            val attempt = runCatching { Engine.mediaItems.getFormatInfo(videoId) }
            val info = attempt.getOrNull()
            val client = info?.clientInfo?.clientName
            when {
                info == null -> lastReason = attempt.exceptionOrNull()?.message ?: lastReason
                info.isUnplayable -> return@io PlaybackResult.Unplayable(info.playabilityReason)
                else -> {
                    val stream = runCatching { pickStream(info) }.getOrNull()
                    Log.i(TAG, "video $videoId, client $client: ${stream?.let { it::class.simpleName } ?: "nothing playable (sabr=${info.containsSabrFormats()})"}")
                    when (stream) {
                        null -> lastReason = info.playabilityReason
                        is Stream.Progressive -> if (lowQuality == null) {
                            lowQuality = PlaybackResult.Ready(info.toPlaybackInfo(videoId), stream, userAgent)
                        }
                        else -> return@io PlaybackResult.Ready(info.toPlaybackInfo(videoId), stream, userAgent)
                    }
                }
            }
            // Erro, só SABR (o Media3 não toca), só o arquivo único ou nada utilizável: próximo cliente.
            Engine.service.switchNextClientNow()
        }
        lowQuality ?: PlaybackResult.Unplayable(lastReason)
    }

    /** Chamado quando o player falha (ex.: HTTP 403): o próximo load usa outro cliente. */
    fun switchClient() {
        Engine.service.switchNextClient()
    }

    suspend fun saveProgress(videoId: String, positionMs: Long) {
        runCatching { io { Engine.mediaItems.updateHistoryPosition(videoId, positionMs / 1000f) } }
    }

    /**
     * A ordem de preferência do SmartTube, sem SABR: DASH, depois o HLS (que também tem todas as
     * qualidades) e só por último o arquivo único.
     */
    internal fun pickStream(info: MediaItemFormatInfo): Stream? = when {
        info.isLive && info.containsHlsUrl() -> Stream.HlsUrl(info.hlsManifestUrl)
        info.isLive && info.containsDashUrl() -> Stream.DashUrl(info.dashManifestUrl)
        info.isLive -> null
        info.containsDashFormats() -> info.createMpdStream()?.use { Stream.DashManifest(it.readBytes()) }
        info.containsHlsUrl() -> Stream.HlsUrl(info.hlsManifestUrl)
        info.containsUrlFormats() -> info.createUrlList()?.firstOrNull()?.let { Stream.Progressive(it) }
        else -> null
    }

    private fun MediaItemFormatInfo.toPlaybackInfo(videoId: String): PlaybackInfo {
        val labels = adaptiveFormats.orEmpty()
            .filter { it.mimeType?.startsWith("audio/") == true }
            .mapNotNull { it.language }
            .distinct()
        return PlaybackInfo(
            videoId = videoId,
            title = title?.takeIf { it.isNotBlank() },
            author = author?.takeIf { it.isNotBlank() },
            channelId = channelId,
            isLive = isLive,
            originalAudioLabel = AudioRules.originalLabel(labels),
            audioLabels = labels,
            clientName = clientInfo?.clientName,
            captions = subtitles.orEmpty().mapNotNull { sub ->
                Caption(
                    url = sub.baseUrl ?: return@mapNotNull null,
                    mimeType = sub.mimeType,
                    language = sub.languageCode,
                    name = sub.name,
                    auto = sub.type == "asr" || sub.vssId?.startsWith("a.") == true,
                )
            },
            storyboard = if (isLive) null else Storyboard.from(runCatching { createStoryboard() }.getOrNull()),
        )
    }
}
