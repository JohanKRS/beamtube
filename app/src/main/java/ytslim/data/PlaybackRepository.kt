package ytslim.data

import android.os.SystemClock
import android.util.Log
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
    private const val TAG = "BeamTube"

    /** Quantos clientes do YouTube tentar quando um só entrega formatos que não tocamos. */
    private const val MAX_CLIENT_SWITCHES = 6

    /**
     * Quanto tempo um vídeo pedido antes da hora espera a tela do player pegá-lo. Os endereços que o
     * YouTube entrega valem por horas; dez minutos cobrem quem para num card, olha os outros e volta.
     */
    private const val EARLY_MAX_AGE_MS = 10 * 60_000L

    /** Quantos pedidos adiantados ficam guardados (cada um tem o MPD do vídeo, dezenas de kB). */
    private const val EARLY_KEPT = 3

    /**
     * TV: quanto o foco fica parado num card antes de o vídeo dele ser pedido ([focused]). Eram
     * 0,6 s; o Johan achou pouco (2026-11-13: "não sou tão ágil assim", pediria vídeo demais só de
     * passar pelos cards com calma). Com 2 s, só é adiantado o card em que ele parou para ler.
     */
    private const val FOCUS_DWELL_MS = 2_000L

    /** TV: depois de um pedido adiantado que não trouxe o vídeo, quanto tempo sem adiantar outros. */
    private const val GUESS_PAUSE_MS = 5 * 60_000L

    val userAgent: String get() = DefaultHeaders.APP_USER_AGENT

    /** Os vídeos pedidos antes de a tela do player precisar deles ([EarlyRequests]). */
    private val early = EarlyRequests<PlaybackResult>(
        scope = AppScope,
        now = SystemClock::elapsedRealtime,
        maxAgeMs = EARLY_MAX_AGE_MS,
        kept = EARLY_KEPT,
        dwellMs = FOCUS_DWELL_MS,
        pauseMs = GUESS_PAUSE_MS,
        // Um palpite só vale se trouxe o vídeo em DASH ou HLS; o resto, o pedido de verdade resolve.
        usable = { it is PlaybackResult.Ready && it.stream !is Stream.Progressive },
        request = { id, guess -> fetch(id, guess) },
        onPaused = { id -> Log.i(TAG, "video $id (ahead of the OK) didn't come: no requests ahead of the OK for ${GUESS_PAUSE_MS / 60_000} min") },
    )

    /**
     * Começa a pedir o vídeo ao motor já no toque do card: abrir a tela do player leva de 0,2 a
     * 0,5 s (mais na TV), e o pedido corre nesse tempo. O [load] seguinte do mesmo vídeo usa este.
     */
    fun prefetch(videoId: String) = early.asked(videoId)

    /**
     * TV: o foco chegou no card do vídeo. Se ficar parado nele, o vídeo é pedido antes do OK, e o
     * [load] depois do OK já encontra a resposta (pedido do Johan, 2026-11-13: o vídeo demorava a começar).
     */
    fun focused(videoId: String) = early.focused(videoId)

    /** TV: o foco saiu do card do vídeo. */
    fun unfocused(videoId: String) = early.unfocused(videoId)

    /**
     * Pede o vídeo ao motor ou, se ele já foi pedido há pouco ([prefetch], [focused]), espera aquele
     * pedido. O adiantado vale uma vez só: um novo load (ex.: depois de trocar de cliente) pede de novo.
     */
    suspend fun load(videoId: String): PlaybackResult {
        val waitedFrom = SystemClock.elapsedRealtime()
        early.take(videoId)?.let {
            Log.i(TAG, "video $videoId: requested early, waited ${SystemClock.elapsedRealtime() - waitedFrom} ms more")
            return it
        }
        return fetch(videoId, guess = false)
    }

    /**
     * Pede o vídeo ao motor, trocando de cliente do YouTube até um entregar DASH ou HLS. Um
     * cliente que só entrega o arquivo único (360p) fica guardado como último recurso: antes,
     * o app tocava esse arquivo assim que aparecia, mesmo com outro cliente entregando 1080p.
     *
     * @param guess o pedido é um palpite (o foco parado num card da TV): uma tentativa só, sem trocar
     * de cliente. Se não vier o vídeo, o pedido de verdade resolve depois.
     */
    private suspend fun fetch(videoId: String, guess: Boolean): PlaybackResult = io {
        var lastReason: String? = null
        var lowQuality: PlaybackResult.Ready? = null
        repeat(if (guess) 1 else MAX_CLIENT_SWITCHES) {
            val from = SystemClock.elapsedRealtime()
            val attempt = runCatching { Engine.mediaItems.getFormatInfo(videoId) }
            val engineMs = SystemClock.elapsedRealtime() - from
            val info = attempt.getOrNull()
            val client = info?.clientInfo?.clientName
            val how = if (guess) " (ahead of the OK)" else ""
            when {
                info == null -> {
                    Log.i(TAG, "video $videoId$how: no answer from the engine in $engineMs ms (${attempt.exceptionOrNull()?.javaClass?.simpleName})")
                    lastReason = attempt.exceptionOrNull()?.message ?: lastReason
                }
                info.isUnplayable -> return@io PlaybackResult.Unplayable(info.playabilityReason)
                else -> {
                    val stream = runCatching { pickStream(info) }.getOrNull()
                    val totalMs = SystemClock.elapsedRealtime() - from
                    Log.i(
                        TAG,
                        "video $videoId$how, client $client: ${stream?.let { it::class.simpleName } ?: "nothing playable (sabr=${info.containsSabrFormats()})"}" +
                            " (engine $engineMs ms, manifest ${totalMs - engineMs} ms)",
                    )
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
            if (!guess) Engine.service.switchNextClientNow()
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
