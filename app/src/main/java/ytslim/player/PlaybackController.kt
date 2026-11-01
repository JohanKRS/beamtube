package ytslim.player

import ytslim.R
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ytslim.data.AudioRules
import ytslim.data.PlaybackInfo
import ytslim.data.PlaybackRepository
import ytslim.data.PlaybackResult
import ytslim.data.Stream

/**
 * Dono do ExoPlayer. Busca os streams pelo motor e aplica as regras do player:
 * qualidade sempre na máxima e áudio sempre o original.
 */
@OptIn(UnstableApi::class)
class PlaybackController(context: Context, private val scope: CoroutineScope) {
    private val res = context.applicationContext.resources

    sealed interface State {
        data object Loading : State
        data class Playing(val info: PlaybackInfo, val selection: String?) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
        playWhenReady = true
        addListener(PlayerListener())
    }

    private var current: PlaybackInfo? = null
    private var videoId: String? = null
    private var saveHistory = true
    private var errorRetries = 0
    private var loadJob: Job? = null

    /**
     * @param saveHistory false para links externos: toca com a conta (Premium), mas não manda
     * o progresso para o histórico do YouTube.
     */
    fun open(videoId: String, startPositionMs: Long = 0, saveHistory: Boolean = true) {
        this.videoId = videoId
        this.saveHistory = saveHistory
        errorRetries = 0
        load(videoId, startPositionMs)
    }

    private fun load(videoId: String, startPositionMs: Long) {
        loadJob?.cancel()
        _state.value = State.Loading
        loadJob = scope.launch {
            val result = runCatching { PlaybackRepository.load(videoId) }
                .getOrElse { PlaybackResult.Unplayable(it.message) }
            when (result) {
                is PlaybackResult.Unplayable ->
                    _state.value = State.Failed(result.reason ?: res.getString(R.string.player_unplayable))
                is PlaybackResult.Ready -> play(result, startPositionMs)
            }
        }
    }

    private fun play(result: PlaybackResult.Ready, startPositionMs: Long) {
        current = result.info
        // Cada vídeo começa com as regras padrão: o que o usuário mudar vale só para o atual.
        player.trackSelectionParameters = TrackRules.defaults(
            player.trackSelectionParameters,
            originalAudioLabel = result.info.originalAudioLabel,
        )
        player.setMediaSource(buildSource(result), startPositionMs)
        player.prepare()
        _state.value = State.Playing(result.info, null)
    }

    private fun buildSource(result: PlaybackResult.Ready): MediaSource {
        val http: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(result.userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val item = MediaItem.Builder().setMediaId(result.info.videoId).build()
        return when (val stream = result.stream) {
            is Stream.DashManifest -> {
                val manifest = DashManifestParser().parse(Uri.EMPTY, stream.xml.inputStream())
                DashMediaSource.Factory(http).createMediaSource(manifest, item)
            }
            is Stream.DashUrl -> DashMediaSource.Factory(http)
                .createMediaSource(item.buildUpon().setUri(stream.url).build())
            is Stream.HlsUrl -> HlsMediaSource.Factory(http)
                .createMediaSource(item.buildUpon().setUri(stream.url).build())
            is Stream.Progressive -> ProgressiveMediaSource.Factory(http)
                .createMediaSource(item.buildUpon().setUri(stream.url).build())
        }
    }

    fun saveProgress() {
        if (!saveHistory) return
        val id = videoId ?: return
        val position = player.currentPosition
        if (position <= 0) return
        scope.launch { PlaybackRepository.saveProgress(id, position) }
    }

    fun release() {
        loadJob?.cancel()
        player.release()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onTracksChanged(tracks: Tracks) {
            enforceOriginalAudio(tracks)
            val info = current ?: return
            _state.value = State.Playing(info, describeSelection(tracks))
        }

        override fun onPlayerError(error: PlaybackException) {
            val id = videoId ?: return
            // 403 e afins costumam ser do cliente do YouTube: troca de cliente e recarrega no mesmo ponto.
            if (errorRetries < MAX_ERROR_RETRIES) {
                errorRetries++
                PlaybackRepository.switchClient()
                load(id, player.currentPosition.coerceAtLeast(0))
            } else {
                _state.value = State.Failed(res.getString(R.string.player_error, error.errorCodeName))
            }
        }
    }

    /** Se o player escolheu uma dublagem, força a faixa marcada como original. */
    private fun enforceOriginalAudio(tracks: Tracks) {
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
        if (audioGroups.size < 2) return
        val selected = audioGroups.firstOrNull { it.isSelected }
        if (selected != null && AudioRules.isOriginal(selected.getTrackFormat(0).language)) return

        val index = AudioRules.pickOriginalGroup(audioGroups.map { it.getTrackFormat(0).language }) ?: return
        val group = audioGroups[index]
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, (0 until group.length).toList()))
            .build()
    }

    private fun describeSelection(tracks: Tracks): String? {
        val video = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
            ?.let { g -> (0 until g.length).filter { g.isTrackSelected(it) }.map { g.getTrackFormat(it) }.maxByOrNull { it.height } }
        val audio = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.getTrackFormat(0)
        val parts = buildList {
            video?.let { add("${it.height}p ${it.codecs ?: it.sampleMimeType ?: ""}".trim()) }
            audio?.let { add(res.getString(R.string.player_audio, it.language ?: res.getString(R.string.player_audio_single))) }
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private companion object {
        const val MAX_ERROR_RETRIES = 3
    }
}

/** Parâmetros de seleção de faixas aplicados no início de cada vídeo. */
@OptIn(UnstableApi::class)
internal object TrackRules {
    fun defaults(base: TrackSelectionParameters, originalAudioLabel: String?): TrackSelectionParameters =
        base.buildUpon()
            .clearOverrides()
            // Qualidade sempre na máxima que o aparelho decodifica, sem limitar ao tamanho da tela.
            .clearVideoSizeConstraints()
            .clearViewportSizeConstraints()
            .setForceHighestSupportedBitrate(true)
            // Áudio original: o Media3 preferiria o idioma do sistema (a dublagem em português).
            .setPreferredAudioLanguage(originalAudioLabel?.let { Util.normalizeLanguageCode(it) })
            // Legenda desligada por padrão.
            .setPreferredTextLanguage(null)
            .setSelectUndeterminedTextLanguage(false)
            .build()
}
