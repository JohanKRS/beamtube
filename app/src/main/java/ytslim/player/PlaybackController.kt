package ytslim.player

import ytslim.R
import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
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
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ytslim.data.AudioRules
import ytslim.data.AuthRepository
import ytslim.data.Caption
import ytslim.data.HeatSegment
import ytslim.data.HeatmapRepository
import ytslim.data.PlaybackInfo
import ytslim.data.PlaybackRepository
import ytslim.data.PlaybackResult
import ytslim.data.PlaylistEditor
import ytslim.data.Rating
import ytslim.data.SettingsRepository
import ytslim.data.SkipRules
import ytslim.data.Stream
import ytslim.data.SubtitleRules
import ytslim.data.VideoActions
import ytslim.data.VideoState
import java.util.Locale

/**
 * Dono do ExoPlayer. Busca os streams pelo motor e aplica as regras do player:
 * qualidade sempre na máxima, áudio sempre o original, legenda com um toque e
 * "pular partes menos interessantes".
 */
@OptIn(UnstableApi::class)
class PlaybackController(context: Context, private val scope: CoroutineScope) {
    private val res = context.applicationContext.resources

    sealed interface State {
        data object Loading : State
        data class Playing(val info: PlaybackInfo, val selection: String?) : State
        data class Failed(val message: String) : State
    }

    /** Botões da página do vídeo. [page] é null enquanto carrega ou sem login. */
    data class Controls(
        val captionsAvailable: Boolean = false,
        val captionsOn: Boolean = false,
        val skipAvailable: Boolean = false,
        val page: VideoState? = null,
        /** O vídeo está na playlist do botão "Save"? null = sem botão (sem login ou não deu para saber). */
        val saved: Boolean? = null,
    )

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _controls = MutableStateFlow(Controls())
    val controls: StateFlow<Controls> = _controls.asStateFlow()

    /** Mensagens curtas para o usuário (ex.: uma ação que falhou). */
    private val _notices = MutableStateFlow<String?>(null)
    val notices: StateFlow<String?> = _notices.asStateFlow()

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
    private var extrasJob: Job? = null
    private var heatmap: List<HeatSegment> = emptyList()

    /**
     * @param saveHistory false para links externos: toca com a conta (Premium), mas não manda
     * o progresso para o histórico do YouTube.
     */
    fun open(videoId: String, startPositionMs: Long = 0, saveHistory: Boolean = true) {
        this.videoId = videoId
        this.saveHistory = saveHistory
        errorRetries = 0
        heatmap = emptyList()
        _controls.value = Controls()
        load(videoId, startPositionMs)
        loadExtras(videoId)
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

    /** Gráfico de "mais repetidos" e a página do vídeo (like, inscrição, capítulos), em paralelo ao stream. */
    private fun loadExtras(videoId: String) {
        extrasJob?.cancel()
        extrasJob = scope.launch {
            launch {
                heatmap = HeatmapRepository.load(videoId)
                if (heatmap.isNotEmpty()) _controls.update { it.copy(skipAvailable = true) }
            }
            if (AuthRepository.isSigned()) {
                launch {
                    val target = SettingsRepository.current().saveTarget.playlistId
                    val saved = PlaylistEditor.isSaved(videoId, target)
                    _controls.update { it.copy(saved = saved) }
                }
                val page = VideoActions.state(videoId)
                _controls.update { it.copy(page = page, skipAvailable = it.skipAvailable || !page?.chapterStartsMs.isNullOrEmpty()) }
            }
        }
    }

    private suspend fun play(result: PlaybackResult.Ready, startPositionMs: Long) {
        current = result.info
        val caption = if (result.info.isLive) null else SubtitleRules.pick(
            captions = result.info.captions,
            deviceLanguage = Locale.getDefault().language,
            originalLanguage = SubtitleRules.originalLanguage(result.info.originalAudioLabel, result.info.captions),
        )
        val captionsOn = caption != null && SettingsRepository.current().captionsOn
        // Cada vídeo começa com as regras padrão: o que o usuário mudar vale só para o atual.
        player.trackSelectionParameters = TrackRules.defaults(
            player.trackSelectionParameters,
            originalAudioLabel = result.info.originalAudioLabel,
            captionsOn = captionsOn,
        )
        player.setMediaSource(buildSource(result, caption), startPositionMs)
        player.prepare()
        _controls.update { it.copy(captionsAvailable = caption != null, captionsOn = captionsOn) }
        _state.value = State.Playing(result.info, null)
    }

    private fun buildSource(result: PlaybackResult.Ready, caption: Caption?): MediaSource {
        val http: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(result.userAgent)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val item = MediaItem.Builder().setMediaId(result.info.videoId).build()
        val main = when (val stream = result.stream) {
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
        val subtitle = caption?.let { subtitleSource(it, http) } ?: return main
        return MergingMediaSource(main, subtitle)
    }

    /** Legenda externa, lida na extração (como o DefaultMediaSourceFactory faz desde o Media3 1.4). */
    private fun subtitleSource(caption: Caption, http: DataSource.Factory): MediaSource? {
        val format = Format.Builder()
            .setSampleMimeType(caption.mimeType)
            .setLanguage(caption.language)
            .setLabel(caption.name)
            // Padrão: ligar o tipo "texto" basta para o seletor escolher esta faixa.
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val parsers = DefaultSubtitleParserFactory()
        if (!parsers.supportsFormat(format)) return null
        val extractors = ExtractorsFactory { arrayOf(SubtitleExtractor(parsers.create(format), format)) }
        val lenient = LenientDataSource.Factory(http, LenientDataSource.emptyCaption(caption.mimeType))
        return ProgressiveMediaSource.Factory(lenient, extractors).createMediaSource(MediaItem.fromUri(caption.url))
    }

    fun toggleCaptions() {
        val controls = _controls.value
        if (!controls.captionsAvailable) return
        val on = !controls.captionsOn
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !on)
            .build()
        _controls.update { it.copy(captionsOn = on) }
        scope.launch { SettingsRepository.setCaptionsOn(on) }
    }

    /** Pula para o próximo trecho mais assistido (ou o próximo capítulo). false se não há para onde. */
    fun skipAhead(): Boolean {
        val position = player.currentPosition
        val target = SkipRules.nextHighlight(heatmap, position)
            ?: SkipRules.nextChapter(_controls.value.page?.chapterStartsMs.orEmpty(), position)
            ?: return false
        player.seekTo(target)
        return true
    }

    fun rate(target: Rating) {
        val id = videoId ?: return
        val page = _controls.value.page ?: return
        val to = if (page.rating == target) Rating.NONE else target
        _controls.update { it.copy(page = page.copy(rating = to)) }
        scope.launch {
            runCatching { VideoActions.rate(id, page.rating, to) }.onFailure {
                _controls.update { c -> c.copy(page = c.page?.copy(rating = page.rating)) }
                _notices.value = res.getString(R.string.action_failed, it.message ?: it.javaClass.simpleName)
            }
        }
    }

    fun toggleSubscribed() {
        val page = _controls.value.page ?: return
        val channelId = page.channelId ?: return
        val to = !page.subscribed
        _controls.update { it.copy(page = page.copy(subscribed = to)) }
        scope.launch {
            runCatching { VideoActions.setSubscribed(channelId, to) }.onFailure {
                _controls.update { c -> c.copy(page = c.page?.copy(subscribed = page.subscribed)) }
                _notices.value = res.getString(R.string.action_failed, it.message ?: it.javaClass.simpleName)
            }
        }
    }

    /** Botão "Save": põe ou tira o vídeo da playlist escolhida nas configurações (Assistir mais tarde). */
    fun toggleSaved() {
        val id = videoId ?: return
        val saved = _controls.value.saved ?: return
        _controls.update { it.copy(saved = !saved) }
        scope.launch {
            val target = SettingsRepository.current().saveTarget
            val name = target.title ?: res.getString(R.string.watch_later)
            runCatching { PlaylistEditor.setSaved(id, target.playlistId, !saved) }.fold(
                { _notices.value = res.getString(if (saved) R.string.player_removed_from else R.string.player_saved_to, name) },
                {
                    _controls.update { c -> c.copy(saved = saved) }
                    _notices.value = res.getString(R.string.action_failed, it.message ?: it.javaClass.simpleName)
                },
            )
        }
    }

    fun noticeShown() {
        _notices.value = null
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
        extrasJob?.cancel()
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
    fun defaults(base: TrackSelectionParameters, originalAudioLabel: String?, captionsOn: Boolean = false): TrackSelectionParameters =
        base.buildUpon()
            .clearOverrides()
            // Qualidade sempre na máxima que o aparelho decodifica, sem limitar ao tamanho da tela.
            .clearVideoSizeConstraints()
            .clearViewportSizeConstraints()
            .setForceHighestSupportedBitrate(true)
            // Áudio original: o Media3 preferiria o idioma do sistema (a dublagem em português).
            .setPreferredAudioLanguage(originalAudioLabel?.let { Util.normalizeLanguageCode(it) })
            // Legenda: só a faixa que o app escolheu, ligada ou desligada pelo botão.
            .setPreferredTextLanguage(null)
            .setSelectUndeterminedTextLanguage(false)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !captionsOn)
            .build()
}
