package ytslim.data

import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo

/** O que o player precisa para tocar um vídeo. */
sealed interface Stream {
    /** MPD gerado pelo motor a partir dos formatos adaptativos (vídeos normais). */
    class DashManifest(val xml: ByteArray) : Stream

    /** Manifest DASH do próprio YouTube (lives). */
    class DashUrl(val url: String) : Stream

    /** Manifest HLS do próprio YouTube (lives). */
    class HlsUrl(val url: String) : Stream

    /** Último recurso: arquivo único (qualidade baixa). */
    class Progressive(val url: String) : Stream
}

data class PlaybackInfo(
    val videoId: String,
    val title: String,
    val author: String?,
    val channelId: String?,
    val isLive: Boolean,
    /** Rótulo da faixa de áudio original como o motor escreve no MPD, ex. "en‐US (original)". */
    val originalAudioLabel: String?,
    val audioLabels: List<String>,
    val clientName: String?,
)

sealed interface PlaybackResult {
    data class Ready(val info: PlaybackInfo, val stream: Stream, val userAgent: String) : PlaybackResult
    data class Unplayable(val reason: String?) : PlaybackResult
}

/** Obtém os streams de um vídeo pelo motor e escolhe o formato que o Media3 toca. */
object PlaybackRepository {
    /** Quantos clientes do YouTube tentar quando um só entrega formatos que não tocamos. */
    private const val MAX_CLIENT_SWITCHES = 6

    val userAgent: String get() = DefaultHeaders.APP_USER_AGENT

    suspend fun load(videoId: String): PlaybackResult = io {
        var lastReason: String? = null
        repeat(MAX_CLIENT_SWITCHES) {
            val attempt = runCatching { Engine.mediaItems.getFormatInfo(videoId) }
            val info = attempt.getOrNull()
            when {
                info == null -> lastReason = attempt.exceptionOrNull()?.message ?: lastReason
                info.isUnplayable -> return@io PlaybackResult.Unplayable(info.playabilityReason)
                else -> {
                    val stream = runCatching { pickStream(info) }.getOrNull()
                    if (stream != null) {
                        return@io PlaybackResult.Ready(info.toPlaybackInfo(videoId), stream, userAgent)
                    }
                    lastReason = info.playabilityReason
                }
            }
            // Erro, só formatos SABR (o Media3 não toca) ou nada utilizável: tenta o próximo cliente.
            Engine.service.switchNextClientNow()
        }
        PlaybackResult.Unplayable(lastReason)
    }

    /** Chamado quando o player falha (ex.: HTTP 403): o próximo load usa outro cliente. */
    fun switchClient() {
        Engine.service.switchNextClient()
    }

    suspend fun saveProgress(videoId: String, positionMs: Long) {
        runCatching { io { Engine.mediaItems.updateHistoryPosition(videoId, positionMs / 1000f) } }
    }

    /** Mesma ordem de preferência do SmartTube, sem SABR. */
    internal fun pickStream(info: MediaItemFormatInfo): Stream? = when {
        info.isLive && info.containsHlsUrl() -> Stream.HlsUrl(info.hlsManifestUrl)
        info.isLive && info.containsDashUrl() -> Stream.DashUrl(info.dashManifestUrl)
        info.isLive -> null
        info.containsDashFormats() -> info.createMpdStream()?.use { Stream.DashManifest(it.readBytes()) }
        info.containsUrlFormats() -> info.createUrlList()?.firstOrNull()?.let { Stream.Progressive(it) }
        info.containsHlsUrl() -> Stream.HlsUrl(info.hlsManifestUrl)
        else -> null
    }

    private fun MediaItemFormatInfo.toPlaybackInfo(videoId: String): PlaybackInfo {
        val labels = adaptiveFormats.orEmpty()
            .filter { it.mimeType?.startsWith("audio/") == true }
            .mapNotNull { it.language }
            .distinct()
        return PlaybackInfo(
            videoId = videoId,
            title = title.orEmpty(),
            author = author,
            channelId = channelId,
            isLive = isLive,
            originalAudioLabel = AudioRules.originalLabel(labels),
            audioLabels = labels,
            clientName = clientInfo?.clientName,
        )
    }
}
