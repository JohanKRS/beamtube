package ytslim.player

import ytslim.R
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
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
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ytslim.data.AudioRules
import ytslim.data.AuthRepository
import ytslim.data.Caption
import ytslim.data.Chapter
import ytslim.data.HeatSegment
import ytslim.data.HeatmapRepository
import ytslim.data.PlaybackInfo
import ytslim.data.Published
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
import ytslim.data.WatchPositions
import ytslim.data.VideoState
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Dono do ExoPlayer. Busca os streams pelo motor e aplica as regras do player:
 * qualidade sempre na máxima, áudio sempre o original, legenda com um toque e
 * "pular partes menos interessantes". Quando o vídeo acaba, toca o próximo da
 * sequência da playlist ([PlayQueue]), se houver.
 *
 * No celular e no tablet, o vídeo continua com a tela desligada ([PlaybackService]): o player
 * segura o processador e o Wi-Fi acordados enquanto toca, e [setBackground] desliga a imagem.
 */
@OptIn(UnstableApi::class)
class PlaybackController(
    context: Context,
    private val scope: CoroutineScope,
    /** O teto de qualidade do aparelho ([QualityRules.top]): 4K na TV, 1080p no celular e no tablet. */
    private val top: Int = QualityRules.MAX_HEIGHT,
) {
    private val app = context.applicationContext
    private val res = app.resources

    /** Na TV o vídeo pausa quando a tela some; no celular e no tablet ele pode continuar com ela apagada. */
    private val tv = top > QualityRules.TOUCH_HEIGHT

    sealed interface State {
        data object Loading : State
        data class Playing(val info: PlaybackInfo) : State
        data class Failed(val message: String) : State
    }

    /** Botões da página do vídeo. [page] é null enquanto carrega ou sem login. */
    data class Controls(
        val captionsAvailable: Boolean = false,
        val captionsOn: Boolean = false,
        /** Onde começa cada trecho que o avanço pula inteiro ([jumpAhead]), para marcar na barra de tempo. */
        val skipSpots: List<Long> = emptyList(),
        /** Os capítulos do vídeo (divisões na barra e o nome do atual); vazio sem capítulos. */
        val chapters: List<Chapter> = emptyList(),
        val page: VideoState? = null,
        /** O vídeo está na playlist do botão "Save"? null = sem botão (sem login ou não deu para saber). */
        val saved: Boolean? = null,
    )

    /** O botão de resolução: as do vídeo, a que está tocando e a escolhida à mão (null = Auto). */
    data class Quality(val heights: List<Int> = emptyList(), val playing: Int? = null, val manual: Int? = null)

    private val _quality = MutableStateFlow(Quality())
    val quality: StateFlow<Quality> = _quality.asStateFlow()

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
        // A regra de qualidade já na primeira escolha do Media3 ([QualitySelector]).
        .setTrackSelector(QualitySelector(context, ::describeTrack, ::chooseAuto))
        .setLoadControl(LoadRules.create(tv = tv))
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        // Com a tela desligada, o Android põe o processador e o Wi-Fi para dormir sem isto.
        .setWakeMode(if (tv) C.WAKE_MODE_NONE else C.WAKE_MODE_NETWORK)
        .build().apply {
        playWhenReady = true
        addListener(PlayerListener())
        addAnalyticsListener(DroppedFramesListener())
    }

    /**
     * O player como a notificação e a tela de bloqueio o veem, com o título e o canal que o app
     * conhece ([PlaybackService]). Só no celular e no tablet.
     */
    internal val sessionPlayer: SessionPlayer? = if (tv) null else SessionPlayer(player)

    /** A tela não mostra o vídeo ([setBackground]): só o som toca. */
    private var background = false

    /**
     * Entre um vídeo e o próximo da sequência (e enquanto o próximo é pedido ao YouTube), o player
     * não segura o processador: com a tela desligada, o aparelho dormiria antes de o próximo começar.
     */
    private val loadingLock: PowerManager.WakeLock? = if (tv) null else
        app.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BeamTube:next video")
            ?.apply { setReferenceCounted(false) }

    /** A qualidade deste vídeo já foi escolhida ([pickQuality]). */
    private var qualityChecked = false

    /** A altura que o app escolheu sozinho para este vídeo (null = escolha do Media3 ou do usuário). */
    private var playingHeight: Int? = null

    /** A resolução escolhida à mão para este vídeo (null = Auto). Vale só para o vídeo atual. */
    @Volatile private var manualHeight: Int? = null

    /** O teto deste vídeo: cai quando ele para várias vezes para carregar (a rede não dá conta). */
    @Volatile private var videoCeiling = top

    /** Depois de descer por causa da rede, tenta voltar ao topo ([scheduleClimb]). */
    private var climbJob: Job? = null

    /** Quantas vezes o aparelho perdeu muitos quadros, e quantas o vídeo parou para carregar. */
    private var stutters = 0
    private var rebuffers = 0

    /** O próximo "carregando" foi o usuário (ou o app) que causou: pular na barra, trocar de faixa. */
    private var expectBuffering = false
    private var lastPlaybackState = Player.STATE_IDLE

    private var current: PlaybackInfo? = null
    private var saveHistory = true
    private var errorRetries = 0
    private var loadJob: Job? = null
    private var extrasJob: Job? = null
    private var nextJob: Job? = null
    private var aheadJob: Job? = null

    /** O primeiro quadro do vídeo atual já apareceu (ou ele falhou): os extras esperam por isto. */
    private val started = MutableStateFlow(false)

    /** Para o log do tempo até o primeiro quadro: quando o vídeo foi pedido, e em quantos ms ficou pronto. */
    private var requestedAt = 0L
    private var readyInMs = -1L
    private var firstFrameLogged = false

    /** A qualidade do início foi trocada pelo app depois da escolha do Media3 (deveria ser raro agora). */
    private var switchedAtStart = false
    private var heatmap: List<HeatSegment> = emptyList()

    /** O vídeo aberto agora. */
    var videoId: String? = null
        private set

    /** O título que veio com o vídeo (do card ou da sequência): o stream do cliente de TV não traz. */
    var titleHint: String? = null
        private set

    /**
     * Há quanto tempo o vídeo foi publicado, como o card escreve ("3 hours ago"); null quando o vídeo
     * veio sem (link de outro app): o player usa a data da página do vídeo ([VideoState.published]).
     */
    var publishedHint: String? = null
        private set

    /** O vídeo atual foi aberto pela sequência (e não pelo usuário)? */
    private var fromQueue = false

    /**
     * @param saveHistory false para links externos: toca com a conta (Premium), mas não manda
     * o progresso para o histórico do YouTube.
     * @param title o título conhecido antes de carregar (o do card).
     * @param published há quanto tempo o vídeo foi publicado, como o card escreve.
     */
    fun open(
        videoId: String,
        startPositionMs: Long = 0,
        saveHistory: Boolean = true,
        title: String? = null,
        published: String? = null,
        /** Onde o card do YouTube diz que o vídeo parou; vale quando este aparelho não guardou outro ponto. */
        resumeHintMs: Long = 0L,
        /** Quando o card foi tocado ([SystemClock.elapsedRealtime]); 0 = agora. Só para o log de tempos. */
        requestedAtMs: Long = 0L,
    ) {
        nextJob?.cancel()
        start(videoId, startPositionMs, saveHistory, title, published, fromQueue = false, resumeHintMs, requestedAtMs)
    }

    private fun start(
        videoId: String,
        startPositionMs: Long,
        saveHistory: Boolean,
        title: String?,
        published: String?,
        fromQueue: Boolean,
        resumeHintMs: Long,
        requestedAtMs: Long = 0L,
    ) {
        requestedAt = requestedAtMs.takeIf { it > 0 } ?: SystemClock.elapsedRealtime()
        readyInMs = -1L
        firstFrameLogged = false
        switchedAtStart = false
        started.value = false
        aheadJob?.cancel()
        this.videoId = videoId
        this.saveHistory = saveHistory
        this.titleHint = title?.takeIf { it.isNotBlank() }
        this.publishedHint = published?.takeIf { it.isNotBlank() }
        this.fromQueue = fromQueue
        _ended.value = false
        errorRetries = 0
        heatmap = emptyList()
        manualHeight = null
        _quality.value = Quality()
        _controls.value = Controls()
        describeNowPlaying()
        load(videoId, startPositionMs, resumeHintMs)
        loadExtras(videoId)
    }

    /**
     * Passa para o próximo vídeo da sequência; sem próximo, marca [ended].
     * @param counts false quando o atual não tocou: o próximo toma o lugar dele no limite.
     */
    private fun playNext(counts: Boolean, delayMs: Long = 0) {
        nextJob?.cancel()
        holdAwake()
        nextJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val next = PlayQueue.next(counts)
            if (next == null) {
                letSleep()
                _ended.value = true
                return@launch
            }
            start(next.id, 0, saveHistory, next.title, Published.of(next), fromQueue = true, resumeHintMs = next.resumeMs)
        }
    }

    /** Mostra o erro. Um vídeo da sequência que não toca (apagado, privado) é pulado. */
    private fun fail(message: String) {
        started.value = true
        _state.value = State.Failed(message)
        if (fromQueue) playNext(counts = false, delayMs = FAILED_PAUSE_MS) else letSleep()
    }

    /**
     * @param startPositionMs 0 = de onde o vídeo parou ([WatchPositions], ou [resumeHintMs]); o
     * começo de um link com tempo, ou o ponto de uma nova tentativa, vem aqui.
     */
    private fun load(videoId: String, startPositionMs: Long, resumeHintMs: Long = 0L) {
        loadJob?.cancel()
        _state.value = State.Loading
        holdAwake()
        loadJob = scope.launch {
            val result = runCatching { PlaybackRepository.load(videoId) }
                .getOrElse { PlaybackResult.Unplayable(it.message) }
            if (readyInMs < 0) readyInMs = SystemClock.elapsedRealtime() - requestedAt
            when (result) {
                is PlaybackResult.Unplayable -> fail(result.reason ?: res.getString(R.string.player_unplayable))
                is PlaybackResult.Ready -> {
                    // Uma live começa no ao vivo, como antes.
                    val from = if (startPositionMs > 0 || result.info.isLive) startPositionMs else WatchPositions.startAt(videoId, resumeHintMs)
                    play(result, from)
                }
            }
        }
    }

    /** Gráfico de "mais repetidos", capítulos e a página do vídeo (like, inscrição), em paralelo ao stream. */
    private fun loadExtras(videoId: String) {
        extrasJob?.cancel()
        extrasJob = scope.launch {
            // Os extras esperam o primeiro quadro (no máximo uns segundos): ler essas páginas grandes junto
            // com o vídeo disputava a rede e o processador com ele, e o começo atrasava (mais na TV).
            withTimeoutOrNull(EXTRAS_WAIT_MS) { started.first { it } }
            launch {
                val markers = HeatmapRepository.load(videoId)
                heatmap = markers.heat
                _controls.update { it.copy(skipSpots = SkipRules.valleys(heatmap), chapters = markers.chapters) }
            }
            if (AuthRepository.isSigned()) {
                launch {
                    val target = SettingsRepository.current().saveTarget.playlistId
                    val saved = PlaylistEditor.isSaved(videoId, target)
                    // Um toque longo recente (talvez ainda indo para a conta) vale mais que a leitura.
                    _controls.update { it.copy(saved = saved?.let { server -> SavedVideos.changedHere(videoId) ?: server }) }
                }
                val page = VideoActions.state(videoId)
                _controls.update { it.copy(page = page) }
                describeNowPlaying()
            }
        }
    }

    private suspend fun play(result: PlaybackResult.Ready, startPositionMs: Long) {
        current = result.info
        describeNowPlaying()
        qualityChecked = false
        playingHeight = null
        videoCeiling = top
        climbJob?.cancel()
        stutters = 0
        rebuffers = 0
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
        // Daqui em diante, o próprio player segura o processador enquanto toca.
        letSleep()
        _controls.update { it.copy(captionsAvailable = caption != null, captionsOn = captionsOn) }
        _state.value = State.Playing(result.info)
        if (!result.info.isLive) prefetchNextNearEnd()
    }

    /**
     * Na sequência de uma playlist, o próximo vídeo é pedido ao motor perto do fim do atual: quando
     * este acaba, o próximo já está resolvido e só falta carregar.
     */
    private fun prefetchNextNearEnd() {
        aheadJob?.cancel()
        if (!PlayQueue.active.value) return
        aheadJob = scope.launch {
            while (isActive) {
                delay(AHEAD_CHECK_MS)
                val duration = player.duration
                if (duration == C.TIME_UNSET || duration <= 0) continue
                if (duration - player.currentPosition > AHEAD_MS) continue
                PlayQueue.peek()?.let {
                    Log.i(TAG, "resolving the next video in the sequence (${it.id})")
                    PlaybackRepository.prefetch(it.id)
                }
                return@launch
            }
        }
    }

    private fun buildSource(result: PlaybackResult.Ready, caption: Caption?): MediaSource {
        val http: DataSource.Factory = MediaHttp.factory(app, result.userAgent)
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
        val extractors = ExtractorsFactory { arrayOf(SubtitleExtractor(BlockCaptionParser(parsers.create(format)), format)) }
        val lenient = LenientDataSource.Factory(http, LenientDataSource.emptyCaption(caption.mimeType))
        return ProgressiveMediaSource.Factory(lenient, extractors).createMediaSource(MediaItem.fromUri(caption.url))
    }

    /**
     * A tela não mostra o vídeo ([on]: a tela apagou, ou o app foi para trás de outro): o player
     * continua só com o som, sem baixar nem decodificar a imagem (bateria e dados). Com a tela de
     * volta, a imagem volta no mesmo ponto, pela regra de qualidade de sempre.
     */
    fun setBackground(on: Boolean) {
        if (background == on) return
        background = on
        Log.i(TAG, if (on) "screen off: playing the sound only" else "screen back: video again")
        if (!on) {
            // A imagem volta carregando um instante: não é a rede falhando nem o aparelho engasgando.
            expectBuffering = true
            stutters = 0
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, on)
            .build()
    }

    /** O título e o canal na notificação e na tela de bloqueio: o que a tela do player mostraria. */
    private fun describeNowPlaying() {
        val session = sessionPlayer ?: return
        val id = videoId ?: return
        val info = current?.takeIf { it.videoId == id }
        val page = _controls.value.page
        session.describe(
            videoId = id,
            title = info?.title ?: titleHint ?: page?.title,
            channel = page?.channelName?.takeIf { it.isNotBlank() } ?: info?.author,
        )
    }

    private fun holdAwake() {
        loadingLock?.acquire(LOADING_LOCK_MS)
    }

    private fun letSleep() {
        loadingLock?.takeIf { it.isHeld }?.release()
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

    /**
     * Para onde vai um avanço de [from] até [to] quando ele começa dentro de um trecho que o
     * público pula, ou logo antes dele: o fim desse trecho ([SkipRules.valleyEnd]), se ficar além
     * de [to]. null = avanço normal.
     */
    fun jumpAhead(from: Long, to: Long): Long? {
        val end = SkipRules.valleyEnd(heatmap, from) ?: return null
        return end.takeIf { it > to }
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
        val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
        if (current?.isLive != true) scope.launch { WatchPositions.save(id, position, duration) }
        scope.launch { PlaybackRepository.saveProgress(id, position) }
    }

    fun release() {
        loadJob?.cancel()
        extrasJob?.cancel()
        nextJob?.cancel()
        climbJob?.cancel()
        aheadJob?.cancel()
        letSleep()
        player.release()
    }

    private inner class PlayerListener : Player.Listener {
        override fun onRenderedFirstFrame() {
            started.value = true
            if (firstFrameLogged) return
            firstFrameLogged = true
            val total = SystemClock.elapsedRealtime() - requestedAt
            val first = player.videoFormat?.let(::heightOf)
            Log.i(TAG, "first frame of $videoId in $total ms (stream ready in $readyInMs ms, ${first}p going to ${playingHeight ?: _quality.value.playing}p, quality switched at start: $switchedAtStart)")
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            watchRebuffering(playbackState)
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

        /** Pausado pela tela de bloqueio, pelo fone ou por uma ligação, com a tela desligada: guarda onde parou. */
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady && background) saveProgress()
        }

        override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK || reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT) {
                expectBuffering = true
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            enforceOriginalAudio(tracks)
            pickQuality(tracks)
            selectedHeight(tracks)?.let { height -> _quality.update { it.copy(playing = height) } }
            val info = current ?: return
            _state.value = State.Playing(info)
        }

        override fun onPlayerError(error: PlaybackException) {
            val id = videoId ?: return
            Log.w(TAG, "player error ${error.errorCodeName} (playing ${playingHeight}p, manual ${manualHeight}p)", error)
            val manual = manualHeight
            if (manual != null && error.errorCode in DECODER_ERRORS) {
                // A resolução escolhida à mão não tocou neste aparelho: avisa e volta ao Auto.
                _notices.value = res.getString(R.string.player_quality_failed, manual)
                setQuality(null)
                player.prepare()
                return
            }
            if (playingHeight != null && error.errorCode in DECODER_ERRORS) {
                // O decodificador não deu conta desta resolução: desce um degrau e continua do mesmo ponto.
                stepDown(sessionWide = true, reason = error.errorCodeName)
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

    /** O aparelho perdeu muitos quadros duas vezes: o decodificador não dá conta, desce um degrau. */
    private inner class DroppedFramesListener : AnalyticsListener {
        override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, droppedFrames: Int, elapsedMs: Long) {
            Log.i(TAG, "dropped $droppedFrames frames in $elapsedMs ms (playing ${playingHeight}p)")
            if (playingHeight == null || droppedFrames < STUTTER_FRAMES || elapsedMs > STUTTER_WINDOW_MS) return
            if (++stutters >= MAX_STUTTERS) stepDown(sessionWide = true, reason = "dropped frames")
        }

        /** Para conferir no log a troca da faixa de partida para o alvo, e se o decodificador seguiu o mesmo. */
        override fun onVideoInputFormatChanged(eventTime: AnalyticsListener.EventTime, format: Format, decoderReuseEvaluation: DecoderReuseEvaluation?) {
            val reuse = when (decoderReuseEvaluation?.result) {
                null -> "new decoder"
                DecoderReuseEvaluation.REUSE_RESULT_NO -> "decoder recreated"
                else -> "same decoder"
            }
            Log.i(TAG, "video now ${format.width}x${format.height} at ${eventTime.currentPlaybackPositionMs} ms ($reuse)")
        }
    }

    /** O vídeo parou para carregar várias vezes sem o usuário pular: a rede não dá conta, desce um degrau. */
    private fun watchRebuffering(state: Int) {
        val previous = lastPlaybackState
        lastPlaybackState = state
        when (state) {
            Player.STATE_READY -> expectBuffering = false
            Player.STATE_BUFFERING -> {
                // Sem a imagem (tela desligada), uma parada não diz nada da resolução do vídeo.
                if (previous != Player.STATE_READY || expectBuffering || !player.playWhenReady || background) return
                Log.i(TAG, "rebuffering (playing ${playingHeight}p)")
                if (playingHeight == null) return
                // Já abaixo do topo por causa da rede: a volta espera a rede ficar calma de novo.
                if (videoCeiling < top) scheduleClimb()
                if (++rebuffers >= MAX_REBUFFERS) stepDown(sessionWide = false, reason = "rebuffering")
            }
        }
    }

    /** Botão de resolução: [height] à mão, só neste vídeo, ou null para o Auto. */
    fun setQuality(height: Int?) {
        manualHeight = height
        stutters = 0
        rebuffers = 0
        qualityChecked = false
        pickQuality(player.currentTracks)
    }

    /**
     * A faixa do vídeo. No Auto, a maior que [QualityRules] aceita até o teto ([top] primeiro), mesmo
     * que o decodificador declare menos (muitas TVs declaram menos do que decodificam). Com uma
     * resolução escolhida à mão, a dela. Uma vez por vídeo, e de novo quando o teto ou a escolha mudam.
     */
    private fun pickQuality(tracks: Tracks) {
        if (qualityChecked) return
        val (video, described) = videoTracks(tracks)
        if (video.none { it.selected }) return
        qualityChecked = true
        val ceiling = minOf(sessionCeiling, videoCeiling)
        Log.i(TAG, "video tracks (ceiling ${ceiling}p, manual ${manualHeight}p): ${described.joinToString(" | ")}")
        val manual = manualHeight?.let { QualityRules.manual(video, it) }
        // Uma escolha à mão que este vídeo não tem (outro cliente do YouTube, outras resoluções) vira Auto.
        if (manual == null) manualHeight = null
        val target = manual ?: QualityRules.best(video, ceiling)
        // Só o Auto desce sozinho; a escolha à mão fica até o usuário mudar.
        playingHeight = if (manual == null) target?.height else null
        _quality.value = Quality(
            heights = QualityRules.heights(video),
            playing = (target ?: video.first { it.selected }).height,
            manual = manualHeight,
        )
        val parameters = player.trackSelectionParameters.buildUpon()
        if (target == null) {
            // Nenhuma faixa com decodificador de hardware até o teto: fica a escolha do Media3.
            parameters.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            Log.i(TAG, "playing ${described[video.indexOf(target)]}")
            if (target.selected) return
            if (!firstFrameLogged) switchedAtStart = true
            parameters.setOverrideForType(TrackSelectionOverride(tracks.groups[target.group].mediaTrackGroup, target.index))
        }
        expectBuffering = true
        player.trackSelectionParameters = parameters.build()
    }

    /** As faixas de vídeo como [QualityRules] as vê, e uma descrição de cada uma para o log. */
    private fun videoTracks(tracks: Tracks): Pair<List<VideoTrack>, List<String>> {
        val video = ArrayList<VideoTrack>()
        val described = ArrayList<String>()
        tracks.groups.forEachIndexed { g, group ->
            if (group.type != C.TRACK_TYPE_VIDEO) return@forEachIndexed
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val support = group.getTrackSupport(i)
                val track = describeTrack(format, support).copy(group = g, index = i, selected = group.isTrackSelected(i))
                video += track
                described += "${format.width}x${format.height}@${format.frameRate} ${format.codecs} " +
                    "${Util.getFormatSupportString(support)}${if (track.hardware) " hw" else ""}${if (track.selected) " SELECTED" else ""}"
            }
        }
        return video to described
    }

    /** Uma faixa de vídeo como [QualityRules] a vê ([support] é o C.FORMAT_* do decodificador). */
    private fun describeTrack(format: Format, support: Int) = VideoTrack(
        group = 0,
        index = 0,
        height = minOf(format.width, format.height).takeIf { format.width > 0 } ?: format.height,
        frameRate = format.frameRate,
        bitrate = format.bitrate,
        handled = support == C.FORMAT_HANDLED,
        exceeds = support == C.FORMAT_EXCEEDS_CAPABILITIES,
        hardware = hasHardwareDecoder(format.sampleMimeType),
        hdr = isHdr(format),
        selected = false,
        mime = format.sampleMimeType,
    )

    /**
     * A escolha do Auto (ou da resolução à mão) para o [QualitySelector], na thread do player: a mesma
     * regra do [pickQuality], já na primeira escolha do vídeo.
     */
    private fun chooseAuto(tracks: List<VideoTrack>): QualityPick? {
        manualHeight?.let { height -> QualityRules.manual(tracks, height)?.let { return QualityPick(it) } }
        val target = QualityRules.best(tracks, minOf(sessionCeiling, videoCeiling)) ?: return null
        // No Auto acima de 1080p, o primeiro pedaço vem numa faixa menor: começa mais rápido.
        return QualityPick(target, QualityRules.start(tracks, target))
    }

    /** A resolução (o lado menor do quadro) da faixa de vídeo tocando. */
    private fun selectedHeight(tracks: Tracks): Int? {
        val group = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected } ?: return null
        // Com a faixa de partida ([StartLowSelection]), duas ficam escolhidas: vale a maior, o alvo.
        return (0 until group.length).filter { group.isTrackSelected(it) }
            .mapNotNull { heightOf(group.getTrackFormat(it)) }
            .maxOrNull()
    }

    private fun heightOf(format: Format): Int? =
        if (format.width > 0) minOf(format.width, format.height) else format.height.takeIf { it > 0 }

    /**
     * A resolução atual não deu conta: o teto cai para o degrau de baixo e a faixa é escolhida de
     * novo. [sessionWide]: o decodificador falhou, e o teto vale até o app fechar; senão (rede),
     * só para este vídeo.
     */
    private fun stepDown(sessionWide: Boolean, reason: String) {
        val height = playingHeight ?: return
        val lower = QualityRules.lowerCeiling(height)
        if (sessionWide) {
            sessionCeiling = minOf(sessionCeiling, lower)
        } else {
            videoCeiling = minOf(videoCeiling, lower)
            scheduleClimb()
        }
        Log.w(TAG, "${height}p failed ($reason): ceiling now ${minOf(sessionCeiling, videoCeiling)}p")
        stutters = 0
        rebuffers = 0
        qualityChecked = false
        // Sem a faixa que falhou; se as faixas ainda não estiverem à mão, a escolha vem no próximo onTracksChanged.
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .build()
        pickQuality(player.currentTracks)
    }

    /**
     * A rede costuma melhorar: depois de [CLIMB_BACK_MS] sem o vídeo parar para carregar, o teto do
     * vídeo volta ao topo (o app oficial do YouTube não volta sozinho, e o Johan não gosta disso).
     */
    private fun scheduleClimb() {
        climbJob?.cancel()
        climbJob = scope.launch {
            delay(CLIMB_BACK_MS)
            Log.i(TAG, "network calm for ${CLIMB_BACK_MS / 1000} s: trying the top quality again")
            videoCeiling = top
            rebuffers = 0
            qualityChecked = false
            pickQuality(player.currentTracks)
        }
    }

    /** HDR (10 bits) só toca se o decodificador aceitar de verdade: forçar daria erro ou cores erradas. */
    private fun isHdr(format: Format): Boolean {
        val transfer = format.colorInfo?.colorTransfer
        if (transfer == C.COLOR_TRANSFER_ST2084 || transfer == C.COLOR_TRANSFER_HLG) return true
        val codecs = format.codecs ?: return false
        return codecs.startsWith("vp09.02") || HDR_AV1.containsMatchIn(codecs)
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

    private companion object {
        const val TAG = "BeamTube"
        const val MAX_ERROR_RETRIES = 3

        /** Erros do decodificador (ERROR_CODE_DECODER_INIT_FAILED até DECODING_FORMAT_UNSUPPORTED). */
        val DECODER_ERRORS = PlaybackException.ERROR_CODE_DECODER_INIT_FAILED..PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED

        /** O Media3 avisa a cada 50 quadros perdidos; 50 em menos de 10 s é engasgo visível. */
        const val STUTTER_FRAMES = 50
        const val STUTTER_WINDOW_MS = 10_000L
        const val MAX_STUTTERS = 2

        /** AV1 de 10 bits ("av01.0.12M.10"): os vídeos HDR do YouTube. */
        val HDR_AV1 = Regex("^av01\\.\\d\\.\\d{2}[MH]\\.10")

        /** Paradas para carregar, no mesmo vídeo, antes de descer um degrau. */
        const val MAX_REBUFFERS = 3

        /** Quanto tempo sem parar para carregar antes de tentar o topo de novo. */
        const val CLIMB_BACK_MS = 120_000L

        /** O teto até o app fechar: começa em 4K e cai quando o decodificador do aparelho não dá conta. */
        @Volatile var sessionCeiling = QualityRules.MAX_HEIGHT

        /** Codec -> o decodificador principal é de hardware? (lido também na thread do player) */
        val hardwareDecoders = ConcurrentHashMap<String, Boolean>()

        /** Quanto os extras (gráfico, like, Save) esperam o primeiro quadro antes de ir mesmo assim. */
        const val EXTRAS_WAIT_MS = 4_000L

        /** Na sequência, o próximo vídeo é resolvido quando faltam [AHEAD_MS] (conferido a cada [AHEAD_CHECK_MS]). */
        const val AHEAD_MS = 40_000L
        const val AHEAD_CHECK_MS = 5_000L

        /** Quanto o erro de um vídeo da sequência fica na tela antes de ir para o próximo. */
        const val FAILED_PAUSE_MS = 3_000L

        /** O máximo que o processador fica acordado esperando o próximo vídeo carregar. */
        const val LOADING_LOCK_MS = 60_000L
    }
}

/**
 * Quanto o player guarda de vídeo. No celular e no tablet, os últimos 15 s já vistos ficam guardados:
 * voltar 10 s com dois toques é instantâneo, sem baixar de novo (em 1080p são uns 8 MB). Na TV, o 4K
 * ocupa até ~130 MB nos 50 s à frente que o Media3 guarda; o teto de 80 MB (uns 25 s de 4K, minutos
 * em 1080p) deixa memória para o resto do app numa TV com pouca RAM.
 */
@OptIn(UnstableApi::class)
internal object LoadRules {
    const val TOUCH_BACK_BUFFER_MS = 15_000
    const val TV_TARGET_BYTES = 80 * 1024 * 1024

    fun create(tv: Boolean): DefaultLoadControl = DefaultLoadControl.Builder().apply {
        if (tv) setTargetBufferBytes(TV_TARGET_BYTES) else setBackBuffer(TOUCH_BACK_BUFFER_MS, /* retainBackBufferFromKeyframe = */ true)
    }.build()
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
