package ytslim.player

import ytslim.R
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.util.Log
import android.view.Display
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
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
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
import kotlinx.coroutines.delay
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
import ytslim.data.PlayQueue
import ytslim.data.PlaylistEditor
import ytslim.data.Rating
import ytslim.data.SavedVideos
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
 * "pular partes menos interessantes". Quando o vídeo acaba, toca o próximo da
 * sequência da playlist ([PlayQueue]), se houver.
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

    private val _ended = MutableStateFlow(false)
    /** O vídeo acabou e a sequência não tem próximo: a tela pode apagar (e a TV desligar, se escolhido). */
    val ended: StateFlow<Boolean> = _ended.asStateFlow()

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
        addAnalyticsListener(DroppedFramesListener())
    }

    /** Até onde [QualityRules] passa por cima dos limites declarados: 1080p, ou a tela se for menor. */
    private val maxForcedHeight: Int = run {
        val mode = (context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager)
            ?.getDisplay(Display.DEFAULT_DISPLAY)?.mode
        val screen = mode?.let { minOf(it.physicalWidth, it.physicalHeight) }?.takeIf { it > 0 }
        minOf(QualityRules.MAX_FORCED_HEIGHT, screen ?: QualityRules.MAX_FORCED_HEIGHT)
    }

    /** A qualidade deste vídeo já foi conferida ([raiseQuality]). */
    private var qualityChecked = false

    /** Está tocando uma faixa acima do limite que o decodificador declara. */
    private var qualityForced = false

    /** Quantas vezes o aparelho perdeu muitos quadros com a faixa forçada. */
    private var stutters = 0

    private var current: PlaybackInfo? = null
    private var saveHistory = true
    private var errorRetries = 0
    private var loadJob: Job? = null
    private var extrasJob: Job? = null
    private var nextJob: Job? = null
    private var heatmap: List<HeatSegment> = emptyList()

    /** O vídeo aberto agora. */
    var videoId: String? = null
        private set

    /** O título que veio com o vídeo (do card ou da sequência): o stream do cliente de TV não traz. */
    var titleHint: String? = null
        private set

    /** O vídeo atual foi aberto pela sequência (e não pelo usuário)? */
    private var fromQueue = false

    /**
     * @param saveHistory false para links externos: toca com a conta (Premium), mas não manda
     * o progresso para o histórico do YouTube.
     * @param title o título conhecido antes de carregar (o do card).
     */
    fun open(videoId: String, startPositionMs: Long = 0, saveHistory: Boolean = true, title: String? = null) {
        nextJob?.cancel()
        start(videoId, startPositionMs, saveHistory, title, fromQueue = false)
    }

    private fun start(videoId: String, startPositionMs: Long, saveHistory: Boolean, title: String?, fromQueue: Boolean) {
        this.videoId = videoId
        this.saveHistory = saveHistory
        this.titleHint = title?.takeIf { it.isNotBlank() }
        this.fromQueue = fromQueue
        _ended.value = false
        errorRetries = 0
        heatmap = emptyList()
        _controls.value = Controls()
        load(videoId, startPositionMs)
        loadExtras(videoId)
    }

    /**
     * Passa para o próximo vídeo da sequência; sem próximo, marca [ended].
     * @param counts false quando o atual não tocou: o próximo toma o lugar dele no limite.
     */
    private fun playNext(counts: Boolean, delayMs: Long = 0) {
        nextJob?.cancel()
        nextJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val next = PlayQueue.next(counts)
            if (next == null) {
                _ended.value = true
                return@launch
            }
            start(next.id, 0, saveHistory, next.title, fromQueue = true)
        }
    }

    /** Mostra o erro. Um vídeo da sequência que não toca (apagado, privado) é pulado. */
    private fun fail(message: String) {
        _state.value = State.Failed(message)
        if (fromQueue) playNext(counts = false, delayMs = FAILED_PAUSE_MS)
    }

    private fun load(videoId: String, startPositionMs: Long) {
        loadJob?.cancel()
        _state.value = State.Loading
        loadJob = scope.launch {
            val result = runCatching { PlaybackRepository.load(videoId) }
                .getOrElse { PlaybackResult.Unplayable(it.message) }
            when (result) {
                is PlaybackResult.Unplayable -> fail(result.reason ?: res.getString(R.string.player_unplayable))
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
                    // Um toque longo recente (talvez ainda indo para a conta) vale mais que a leitura.
                    _controls.update { it.copy(saved = saved?.let { server -> SavedVideos.changedHere(videoId) ?: server }) }
                }
                val page = VideoActions.state(videoId)
                _controls.update { it.copy(page = page, skipAvailable = it.skipAvailable || !page?.chapterStartsMs.isNullOrEmpty()) }
            }
        }
    }

    private suspend fun play(result: PlaybackResult.Ready, startPositionMs: Long) {
        current = result.info
        qualityChecked = false
        qualityForced = false
        stutters = 0
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

    /**
     * Botão "Save": põe ou tira o vídeo da playlist escolhida nas configurações (Assistir mais tarde).
     * O ícone e o aviso mudam na hora; a conta é atualizada em segundo plano ([SavedVideos.toggle]).
     */
    fun toggleSaved() {
        val id = videoId ?: return
        val saved = _controls.value.saved ?: return
        val now = SavedVideos.toggle(id, current = saved) { error ->
            if (videoId == id) _controls.update { c -> c.copy(saved = SavedVideos.isSaved(id)) }
            _notices.value = res.getString(R.string.action_failed, error.message ?: error.javaClass.simpleName)
        }
        _controls.update { it.copy(saved = now) }
        val name = SavedVideos.target.value.title ?: res.getString(R.string.watch_later)
        _notices.value = res.getString(if (now) R.string.player_saved_to else R.string.player_removed_from, name)
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
        nextJob?.cancel()
        player.release()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    saveProgress()
                    playNext(counts = true)
                }
                // Tocar de novo depois do fim (ou voltar na barra) mantém a tela acesa outra vez.
                Player.STATE_BUFFERING, Player.STATE_READY -> {
                    nextJob?.cancel()
                    _ended.value = false
                }
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            enforceOriginalAudio(tracks)
            raiseQuality(tracks)
            val info = current ?: return
            _state.value = State.Playing(info, describeSelection(tracks))
        }

        override fun onPlayerError(error: PlaybackException) {
            val id = videoId ?: return
            Log.w(TAG, "player error ${error.errorCodeName} (quality forced: $qualityForced)", error)
            if (qualityForced && error.errorCode in DECODER_ERRORS) {
                // O aparelho não deu conta da faixa acima do limite declarado: volta a confiar nele.
                trustDecoderLimits()
                player.prepare()
                return
            }
            // 403 e afins costumam ser do cliente do YouTube: troca de cliente e recarrega no mesmo ponto.
            if (errorRetries < MAX_ERROR_RETRIES) {
                errorRetries++
                PlaybackRepository.switchClient()
                load(id, player.currentPosition.coerceAtLeast(0))
            } else {
                fail(res.getString(R.string.player_error, error.errorCodeName))
            }
        }
    }

    /** Com a faixa forçada, o aparelho perdeu muitos quadros duas vezes: volta à escolha do Media3. */
    private inner class DroppedFramesListener : AnalyticsListener {
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            Log.i(TAG, "dropped $droppedFrames frames in $elapsedMs ms (quality forced: $qualityForced)")
            if (!qualityForced || droppedFrames < STUTTER_FRAMES || elapsedMs > STUTTER_WINDOW_MS) return
            if (++stutters >= MAX_STUTTERS) trustDecoderLimits()
        }
    }

    /**
     * Se o Media3 ficou com uma faixa menor do que o aparelho decodifica (decodificadores que
     * declaram limites baixos demais), troca pela maior que [QualityRules] aceita. Uma vez por vídeo.
     */
    private fun raiseQuality(tracks: Tracks) {
        if (qualityChecked) return
        val video = ArrayList<VideoTrack>()
        val described = ArrayList<String>()
        tracks.groups.forEachIndexed { g, group ->
            if (group.type != C.TRACK_TYPE_VIDEO) return@forEachIndexed
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val support = group.getTrackSupport(i)
                val track = VideoTrack(
                    group = g,
                    index = i,
                    height = minOf(format.width, format.height).takeIf { format.width > 0 } ?: format.height,
                    frameRate = format.frameRate,
                    bitrate = format.bitrate,
                    handled = support == C.FORMAT_HANDLED,
                    exceeds = support == C.FORMAT_EXCEEDS_CAPABILITIES,
                    hardware = hasHardwareDecoder(format.sampleMimeType),
                    selected = group.isTrackSelected(i),
                )
                video += track
                described += "${format.width}x${format.height}@${format.frameRate} ${format.codecs} " +
                    "${Util.getFormatSupportString(support)}${if (track.hardware) " hw" else ""}${if (track.selected) " SELECTED" else ""}"
            }
        }
        if (video.none { it.selected }) return
        qualityChecked = true
        Log.i(TAG, "video tracks (max forced ${maxForcedHeight}p): ${described.joinToString(" | ")}")
        if (decoderLimitsTrusted) return
        val target = QualityRules.upgrade(video, maxForcedHeight) ?: return
        Log.i(TAG, "raising quality to ${described[video.indexOf(target)]}")
        qualityForced = true
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(tracks.groups[target.group].mediaTrackGroup, target.index))
            .build()
    }

    /** Desiste de passar por cima dos limites do decodificador até o app fechar. */
    private fun trustDecoderLimits() {
        Log.w(TAG, "forced quality failed; trusting the decoder limits from now on")
        decoderLimitsTrusted = true
        qualityForced = false
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .build()
    }

    private fun hasHardwareDecoder(mimeType: String?): Boolean {
        mimeType ?: return false
        return hardwareDecoders.getOrPut(mimeType) {
            runCatching { MediaCodecUtil.getDecoderInfos(mimeType, false, false).firstOrNull()?.hardwareAccelerated == true }
                .getOrDefault(false)
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
        const val TAG = "Doorglow"
        const val MAX_ERROR_RETRIES = 3

        /** Erros do decodificador (ERROR_CODE_DECODER_INIT_FAILED até DECODING_FORMAT_UNSUPPORTED). */
        val DECODER_ERRORS = PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED

        /** O Media3 avisa a cada 50 quadros perdidos; 50 em menos de 10 s é engasgo visível. */
        const val STUTTER_FRAMES = 50
        const val STUTTER_WINDOW_MS = 10_000L
        const val MAX_STUTTERS = 2

        /** O aparelho já falhou com uma faixa forçada: vale o limite declarado até o app fechar. */
        var decoderLimitsTrusted = false

        /** Codec -> o decodificador principal é de hardware? */
        val hardwareDecoders = HashMap<String, Boolean>()

        /** Quanto o erro de um vídeo da sequência fica na tela antes de ir para o próximo. */
        const val FAILED_PAUSE_MS = 3_000L
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
