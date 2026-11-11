package ytslim.player

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.VideoSize
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import androidx.media3.ui.TimeBar
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Size as CoilSize
import coil3.toBitmap
import java.lang.ref.WeakReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ytslim.MainActivity
import ytslim.R
import ytslim.isTv
import ytslim.data.AppScope
import ytslim.data.Chapter
import ytslim.data.Chapters
import ytslim.data.ExternalLinks
import ytslim.data.PlayQueue
import ytslim.data.Rating
import ytslim.data.SettingsRepository
import ytslim.data.Storyboard
import ytslim.data.VideoLinks
import ytslim.ui.Look

/**
 * Tela do player, que é também a "página do vídeo": like/dislike e inscrição ficam nos
 * controles. Usa Views porque o PlayerView do Media3 já cuida do D-pad da TV e do toque.
 * O desenho é o da opção A (player_controls): pílula de ações em cima, ilha de vidro embaixo.
 *
 * O botão da lua abre o painel de dormir: escurecer o vídeo, quantos vídeos da playlist ainda
 * tocam e o que fazer quando eles acabarem (desligar a TV, pelo [ScreenOffService]).
 *
 * No celular e no tablet: toques no vídeo como no YouTube ([TouchRules]), e a tela gira conforme
 * o vídeo (deitada para vídeo horizontal, em pé para vertical), com um botão para girar à mão.
 * Com a tela desligada no meio do vídeo, ele continua tocando, só o som ([PlaybackService]); saindo
 * do app com o vídeo tocando, ele fica no mini player flutuante ([MiniPlayerRules]).
 */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private lateinit var controller: PlaybackController
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var detailView: TextView
    private lateinit var messageView: TextView
    private lateinit var actions: View
    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var subscribeButton: TextView
    private lateinit var saveButton: ImageButton
    private lateinit var channelButton: TextView
    private lateinit var captionsButton: ImageButton
    private lateinit var sleepButton: ImageButton
    private lateinit var rotateButton: ImageButton
    private lateinit var island: PlayerIsland
    private lateinit var captionsView: SubtitleView
    private lateinit var captionLift: CaptionLift
    private lateinit var seekBadge: TextView
    private lateinit var sleepPanel: View
    private lateinit var qualityButton: TextView
    private lateinit var liveButton: TextView
    private lateinit var positionView: View
    private lateinit var durationView: View
    private lateinit var qualityPanel: View
    private lateinit var qualityRow: LinearLayout
    private lateinit var brightnessRow: LinearLayout
    private lateinit var queueRow: LinearLayout
    private lateinit var endRow: LinearLayout
    private lateinit var dimView: View
    private lateinit var timeBar: ChapterTimeBar
    private lateinit var chapterView: TextView
    private lateinit var islandView: View
    private lateinit var preview: View
    private lateinit var previewImage: ImageView
    private lateinit var previewChapter: TextView
    private lateinit var previewTime: TextView

    private var saveHistory = true
    private var external = false

    /** Quando o card foi tocado (para o log do tempo até o primeiro quadro); 0 = agora. */
    private var requestedAtMs = 0L

    /** Brilho do vídeo no modo dormir, em %. Vale até fechar o app ([sessionBrightness]). */
    private var brightness = 100

    /** "When done: Turn off", guardado nas configurações. */
    private var turnOffWhenDone = false

    /** Desligar no fim é só da TV (no celular e no tablet a opção não existe). */
    private val canTurnOff by lazy { isTv() && ScreenOffService.supported }

    /** O fim atual já foi tratado (a tela só desliga uma vez por fim). */
    private var endHandled = false
    private var turnOffJob: Job? = null

    /** O painel aberto acima da barra de tempo (resolução ou modo dormir), se houver. */
    private var openPanel: View? = null

    /** A seta que acabou de esconder os controles ([hidesControls]): o resto dela não os acende de novo. */
    private var hidingKey: Int? = null

    /**
     * A tecla apertada agora começou com os controles abertos. Segurada, só ela pode escondê-los: a
     * seta que acendeu os controles, se continuar apertada, não os apaga logo em seguida.
     */
    private var keyStartedOpen = false

    /** As resoluções que o painel de qualidade mostra agora. */
    private var qualityHeights: List<Int> = emptyList()

    /** Os trechos que o avanço pula inteiros, marcados agora na barra de tempo. */
    private var shownSkipSpots: List<Long> = emptyList()
    private var showingLive = false

    /** Os capítulos do vídeo atual (frestas na barra, o nome do atual e o do preview). */
    private var chapters: List<Chapter> = emptyList()

    /** A barra está sendo arrastada (com o dedo ou, na TV, com esquerda e direita). */
    private var scrubbing = false

    /** As miniaturas do preview do vídeo atual e as últimas folhas baixadas (no máximo [SHEETS_KEPT]). */
    private var storyboard: Storyboard? = null
    private val sheets = object : LinkedHashMap<Int, Bitmap>(SHEETS_KEPT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Bitmap>?) = size > SHEETS_KEPT
    }
    private val sheetsLoading = HashSet<Int>()
    private var previewFrame: Storyboard.Frame? = null
    private val previewMatrix = Matrix()
    private val timeText = StringBuilder()
    private val timeFormat = java.util.Formatter(timeText, java.util.Locale.getDefault())

    /** Celular ou tablet (toques no vídeo, girar a tela, 1080p). */
    private val touch by lazy { !isTv() }

    /** O aparelho tem o mini player flutuante (picture-in-picture): celular e tablet, Android 8 em diante. */
    private val miniPlayerSupported by lazy {
        touch && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    /** O vídeo está no mini player agora. */
    private var floating = false
    private val videoBounds = Rect()

    /** O sentido escolhido no botão de girar; vale até fechar o player. null = segue o vídeo. */
    private var chosenOrientation: Int? = null

    /** Pulos seguidos com dois toques: o lado, até quando um toque só ainda soma, e o total. */
    private var streakZone: TouchRules.Zone? = null
    private var streakUntil = 0L
    private var streakSeconds = 0
    private var streakTap = false

    private val closePanelOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closePanel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // A cor de destaque escolhida em Settings, para os drawables do player (values/accents.xml).
        theme.applyStyle(Look.accent.playerStyle, true)
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra(EXTRA_BACK_TO_PLAYER, false)) {
            // O toque na notificação, com o player numa tarefa que não é a de cima: só traz ele para a frente.
            PlaybackService.bringPlayerToFront(this)
            finish()
            return
        }
        val target = resolveTarget(intent)
        if (target == null) {
            // Um link de playlist compartilhado vai para a página da playlist.
            playlistLink(intent)?.let { startActivity(MainActivity.playlistIntent(this, it)) }
                ?: Toast.makeText(this, R.string.link_not_a_video, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        // Um vídeo novo fecha o mini player do anterior: os dois não tocam juntos.
        floatingPlayer?.get()?.takeIf { it !== this }?.finish()
        external = target.external
        if (savedInstanceState == null) requestedAtMs = intent.getLongExtra(EXTRA_REQUESTED_AT, 0L)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Um link de outro app não continua a sequência de uma playlist aberta antes.
        if (target.external && savedInstanceState == null) PlayQueue.clear()

        controller = PlaybackController(this, AppScope, QualityRules.top(tv = !touch))
        // Recriada no meio de uma sequência, a tela volta ao vídeo que estava tocando.
        val videoId = savedInstanceState?.getString(KEY_VIDEO) ?: target.videoId
        val restored = savedInstanceState?.containsKey(KEY_VIDEO) == true
        val title = (if (restored) savedInstanceState?.getString(KEY_TITLE) else intent.getStringExtra(EXTRA_TITLE))
            ?.takeIf { it.isNotBlank() }
        setContentView(buildLayout(title))
        playerView.player = controller.player
        controller.player.addListener(PlayerScreenListener())
        bindButtons()
        if (touch) bindTouchGestures()
        if (miniPlayerSupported) bindMiniPlayer()
        bindScrubbing()
        buildSleepPanel()
        // O brilho escolhido num vídeo vale para os próximos, até fechar o app (pedido do Johan, 2026-11-03).
        if (sessionBrightness < 100) setBrightness(sessionBrightness)
        onBackPressedDispatcher.addCallback(this, closePanelOnBack)

        val startMs = savedInstanceState?.getLong(KEY_POSITION) ?: target.startMs
        // O tempo do card vale só para o vídeo pedido (recriada no meio da sequência, é outro).
        val resumeMs = if (restored) 0L else target.resumeMs
        lifecycleScope.launch {
            turnOffWhenDone = canTurnOff && SettingsRepository.current().turnOffWhenDone
            renderSleep()
            if (!target.external) return@launch start(videoId, startMs, saveHistory = true, title, resumeMs)
            // Link de outro app: o padrão é não ir para o histórico (configurável).
            when (SettingsRepository.current().externalLinks) {
                ExternalLinks.NO_HISTORY -> start(videoId, startMs, saveHistory = false, title, resumeMs)
                ExternalLinks.SAVE_HISTORY -> start(videoId, startMs, saveHistory = true, title, resumeMs)
                ExternalLinks.ASK -> ask { save -> start(videoId, startMs, save, title, resumeMs) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { controller.state.collect(::render) }
                launch { controller.controls.collect(::renderControls) }
                launch { controller.ended.collect(::onEnded) }
                launch { controller.quality.collect(::renderQuality) }
                launch {
                    while (true) {
                        renderLive()
                        delay(LIVE_TICK_MS)
                    }
                }
                launch { combine(PlayQueue.active, PlayQueue.left) { _, _ -> }.collect { renderSleep() } }
                launch {
                    controller.notices.collect { notice ->
                        if (notice != null) {
                            Toast.makeText(this@PlayerActivity, notice, Toast.LENGTH_LONG).show()
                            controller.noticeShown()
                        }
                    }
                }
            }
        }
    }

    private fun start(videoId: String, startMs: Long, saveHistory: Boolean, title: String?, resumeMs: Long) {
        this.saveHistory = saveHistory
        controller.open(videoId, startMs, saveHistory, title, resumeHintMs = resumeMs, requestedAtMs = requestedAtMs)
        requestedAtMs = 0L
    }

    private fun ask(onChoice: (saveHistory: Boolean) -> Unit) {
        val options = arrayOf(getString(R.string.ext_ask_no_history), getString(R.string.ext_ask_save_history))
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.ext_ask_title)
            .setItems(options) { _, which -> onChoice(which == 1) }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun render(state: PlaybackController.State) {
        when (state) {
            PlaybackController.State.Loading, is PlaybackController.State.Playing -> {
                // Também o próximo vídeo da sequência: título novo, sem os detalhes do anterior.
                messageView.visibility = View.GONE
                val board = (state as? PlaybackController.State.Playing)?.info?.storyboard
                if (board !== storyboard) {
                    storyboard = board
                    sheets.clear()
                    sheetsLoading.clear()
                }
                showTitle()
                showDetail()
            }
            is PlaybackController.State.Failed -> {
                messageView.text = state.message
                messageView.visibility = View.VISIBLE
            }
        }
    }

    /**
     * O título do vídeo: o do stream; se ele vier sem (o cliente de TV do YouTube não manda), o do
     * card que abriu o vídeo (ou da sequência); e, nos links de outros apps, o da página do vídeo.
     */
    private fun showTitle() {
        val info = (controller.state.value as? PlaybackController.State.Playing)?.info
        val title = info?.title ?: controller.titleHint ?: controller.controls.value.page?.title
        if (titleView.text?.toString().orEmpty() != title.orEmpty()) titleView.text = title
    }

    /**
     * A linha embaixo do título: o canal (um botão, quando a página do vídeo chega) e o aviso de
     * "fora do histórico". Sem a página, o nome do canal vem do stream, como texto.
     */
    private fun showDetail() {
        val state = controller.state.value
        val page = controller.controls.value.page
        val channel = page?.channelName?.takeIf { page.channelId != null && it.isNotBlank() }
        channelButton.visibility = if (channel != null) View.VISIBLE else View.GONE
        channelButton.text = channel
        val author = if (channel == null) (state as? PlaybackController.State.Playing)?.info?.author else null
        val noHistory = if (!saveHistory) getString(R.string.player_no_history) else null
        val rest = listOfNotNull(author, noHistory).joinToString(" · ")
        detailView.text = if (channel != null && rest.isNotEmpty()) "· $rest" else rest
    }

    private fun renderControls(controls: PlaybackController.Controls) {
        showTitle()
        showDetail()
        val page = controls.page
        actions.visibility = if (page != null) View.VISIBLE else View.GONE
        if (page != null) {
            likeButton.setActive(page.rating == Rating.LIKED)
            dislikeButton.setActive(page.rating == Rating.DISLIKED)
            subscribeButton.visibility = if (page.channelId != null) View.VISIBLE else View.GONE
            subscribeButton.setText(if (page.subscribed) R.string.player_subscribed else R.string.player_subscribe)
            // Inscrito: só o contorno (player_subscribe_bg).
            subscribeButton.isActivated = page.subscribed
        }
        saveButton.visibility = if (controls.saved != null) View.VISIBLE else View.GONE
        saveButton.setImageResource(if (controls.saved == true) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
        saveButton.setActive(controls.saved == true)
        saveButton.contentDescription = getString(if (controls.saved == true) R.string.player_saved else R.string.player_save)
        if (controls.skipSpots != shownSkipSpots) {
            shownSkipSpots = controls.skipSpots
            playerView.setExtraAdGroupMarkers(controls.skipSpots.toLongArray(), BooleanArray(controls.skipSpots.size))
        }
        if (controls.chapters != chapters) {
            chapters = controls.chapters
            timeBar.setChapters(chapters.map { it.startMs })
            showChapter(controller.player.currentPosition)
        }
        captionsButton.visibility = if (controls.captionsAvailable) View.VISIBLE else View.GONE
        captionsButton.setActive(controls.captionsOn)
        captionsButton.contentDescription =
            getString(if (controls.captionsOn) R.string.player_captions_off else R.string.player_captions)
    }

    private fun bindButtons() {
        likeButton.setOnClickListener { controller.rate(Rating.LIKED) }
        dislikeButton.setOnClickListener { controller.rate(Rating.DISLIKED) }
        subscribeButton.setOnClickListener { controller.toggleSubscribed() }
        saveButton.setOnClickListener { controller.toggleSaved() }
        channelButton.setOnClickListener {
            val page = controller.controls.value.page ?: return@setOnClickListener
            val channelId = page.channelId ?: return@setOnClickListener
            // Fecha antes de abrir o canal: sair assim não abre o mini player.
            finish()
            startActivity(MainActivity.channelIntent(this, channelId, page.channelName.orEmpty()))
        }
        captionsButton.setOnClickListener { controller.toggleCaptions() }
        sleepButton.setOnClickListener { togglePanel(sleepPanel) }
        rotateButton.setOnClickListener { rotate() }
        qualityButton.setOnClickListener { togglePanel(qualityPanel) }
        liveButton.setOnClickListener {
            controller.player.seekToDefaultPosition()
            renderLive()
        }
        // O painel some junto com os controles, e a legenda sobe e desce com eles.
        playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility != View.VISIBLE) closePanel(focusButton = false)
                captionLift.setOpen(visibility == View.VISIBLE)
            },
        )
        captionLift.setOpen(playerView.isControllerFullyVisible)
    }

    /**
     * Botão de resolução: mostra a que está tocando (âmbar quando escolhida à mão). O painel tem
     * "Auto" e as resoluções do vídeo; a escolha vale só para o vídeo atual e fica mesmo com a rede
     * oscilando (no Auto, o app desce e volta sozinho).
     */
    private fun renderQuality(quality: PlaybackController.Quality) {
        qualityButton.visibility = if (quality.heights.isEmpty()) View.GONE else View.VISIBLE
        qualityButton.text = quality.playing?.let { getString(R.string.player_quality_height, it) }
            ?: getString(R.string.player_quality_auto)
        qualityButton.setTextColor(color(if (quality.manual != null) R.color.player_accent else R.color.player_icon))
        if (quality.heights != qualityHeights) {
            qualityHeights = quality.heights
            qualityRow.removeAllViews()
            qualityRow.addView(chip(getString(R.string.player_quality_auto), AUTO_QUALITY) { chooseQuality(null) })
            quality.heights.forEach { height ->
                qualityRow.addView(chip(getString(R.string.player_quality_height, height), height) { chooseQuality(height) })
            }
            if (quality.heights.isEmpty() && openPanel == qualityPanel) closePanel(focusButton = false)
        }
        qualityRow.select(quality.manual ?: AUTO_QUALITY)
    }

    /**
     * Ao vivo (pedido do Johan, 2026-11-03): numa live o tempo e a duração ficam parados (a janela
     * que dá para voltar anda junto com o vídeo), então dão lugar ao botão LIVE. O ponto fica
     * vermelho no ponto ao vivo e cinza quando o vídeo está atrás dele; tocar volta para o ao vivo.
     * Roda a cada segundo enquanto a tela está aberta.
     */
    private fun renderLive() {
        val player = controller.player
        val live = player.isCurrentMediaItemLive
        if (live != showingLive) {
            showingLive = live
            positionView.visibility = if (live) View.GONE else View.VISIBLE
            durationView.visibility = if (live) View.GONE else View.VISIBLE
            liveButton.visibility = if (live) View.VISIBLE else View.GONE
        }
        if (!live) return
        val atLive = behindLiveMs(player) < LIVE_SLACK_MS
        liveButton.compoundDrawableTintList = ColorStateList.valueOf(color(if (atLive) R.color.player_live else R.color.player_text_secondary))
    }

    /** Quanto o vídeo está atrás do ponto em que o player toca uma live (a folga que ele mantém do fim). */
    private fun behindLiveMs(player: Player): Long {
        val offset = player.currentLiveOffset
        if (offset == C.TIME_UNSET || player.currentTimeline.isEmpty) return 0
        val window = player.currentTimeline.getWindow(player.currentMediaItemIndex, Timeline.Window())
        val target = window.liveConfiguration?.targetOffsetMs?.takeIf { it != C.TIME_UNSET } ?: 0
        return offset - target
    }

    private fun chooseQuality(height: Int?) {
        controller.setQuality(height)
        closePanel()
    }

    /** As opções do painel de dormir. A lista de "Then play" só aparece numa sequência de playlist. */
    private fun buildSleepPanel() {
        BRIGHTNESS_LEVELS.forEach { level ->
            brightnessRow.addView(chip(getString(R.string.player_brightness_level, level), level) { setBrightness(level) })
        }
        PlayQueue.CHOICES.forEach { count ->
            val label = when (count) {
                0 -> getString(R.string.player_then_play_none)
                PlayQueue.ALL -> getString(R.string.player_then_play_all)
                else -> count.toString()
            }
            queueRow.addView(chip(label, count) { PlayQueue.setLeft(count) })
        }
        endRow.addView(chip(getString(R.string.player_stay_on), false) { setTurnOff(false) })
        endRow.addView(chip(getString(R.string.player_turn_off), true) { setTurnOff(true) })
        renderSleep()
    }

    private fun chip(text: String, value: Any, onClick: () -> Unit): TextView =
        TextView(this, null, 0, R.style.PlayerChip).apply {
            this.text = text
            tag = value
            // Peso 1: num trilho alargado por [sameWidth], as opções crescem por igual.
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(34), 1f)
            setOnClickListener { onClick() }
        }

    private fun renderSleep() {
        if (!::sleepPanel.isInitialized) return
        brightnessRow.select(brightness)
        // Cada linha está num grupo com o nome dela em cima: é o grupo que aparece ou some.
        (queueRow.parent as View).visibility = if (PlayQueue.active.value) View.VISIBLE else View.GONE
        queueRow.select(PlayQueue.left.value)
        // Só na TV, e só com a ação de dormir do sistema (Android 9 em diante).
        (endRow.parent as View).visibility = if (canTurnOff) View.VISIBLE else View.GONE
        endRow.select(turnOffWhenDone)
        val limited = PlayQueue.active.value && PlayQueue.left.value != PlayQueue.ALL
        sleepButton.setActive(brightness < 100 || turnOffWhenDone || limited)
        sameWidth(brightnessRow, queueRow, endRow)
    }

    /** Os trilhos visíveis do painel ficam da largura do maior, como no desenho. */
    private fun sameWidth(vararg rows: LinearLayout) {
        val shown = rows.filter { (it.parent as View).visibility == View.VISIBLE }
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        shown.forEach {
            it.minimumWidth = 0
            it.measure(unspecified, unspecified)
        }
        val width = shown.maxOfOrNull { it.measuredWidth } ?: return
        shown.forEach { it.minimumWidth = width }
    }

    /** Marca a opção de [value] na linha (as cores vêm de player_segment_bg e player_segment_text). */
    private fun LinearLayout.select(value: Any) {
        for (i in 0 until childCount) {
            val option = getChildAt(i)
            option.isActivated = option.tag == value
        }
    }

    /** Abre [panel] (fechando o outro) ou fecha, se ele já estiver aberto. */
    private fun togglePanel(panel: View) {
        if (openPanel == panel) return closePanel()
        closePanel(focusButton = false)
        if (panel == sleepPanel) renderSleep()
        panel.visibility = View.VISIBLE
        openPanel = panel
        closePanelOnBack.isEnabled = true
        // Com o painel aberto, os controles não somem sozinhos (no celular os toques não contam).
        playerView.controllerShowTimeoutMs = 0
        playerView.showController()
        val focus = if (panel == sleepPanel) {
            brightnessRow.findViewWithTag<View>(brightness)
        } else {
            qualityRow.findViewWithTag<View>(controller.quality.value.manual ?: AUTO_QUALITY)
        }
        focus?.requestFocus()
    }

    private fun closePanel(focusButton: Boolean = true) {
        val panel = openPanel ?: return
        panel.visibility = View.GONE
        openPanel = null
        closePanelOnBack.isEnabled = false
        playerView.controllerShowTimeoutMs = CONTROLS_TIMEOUT_MS
        if (focusButton) {
            playerView.showController()
            (if (panel == sleepPanel) sleepButton else qualityButton).requestFocus()
        }
    }

    /** Escurece o vídeo (a legenda junto), sem mexer nos controles nem no brilho da TV. */
    private fun setBrightness(level: Int) {
        brightness = level
        sessionBrightness = level
        dimView.alpha = 1f - level / 100f
        dimView.visibility = if (level < 100) View.VISIBLE else View.GONE
        // Escurecido, os controles não acendem sozinhos na troca de vídeo nem no fim.
        playerView.controllerAutoShow = level == 100
        renderSleep()
    }

    private fun setTurnOff(on: Boolean) {
        turnOffWhenDone = on
        AppScope.launch { SettingsRepository.setTurnOffWhenDone(on) }
        renderSleep()
        if (on && !ScreenOffService.enabled) {
            // Uma vez só: o Android pede que o serviço seja ligado à mão.
            Toast.makeText(this, R.string.player_turn_off_setup, Toast.LENGTH_LONG).show()
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (_: ActivityNotFoundException) {
                // Sem a tela de Acessibilidade: o aviso já diz onde ligar.
            }
        }
    }

    /**
     * O vídeo acabou sem próximo na sequência: a tela pode apagar e, com "Turn off", o aparelho
     * dorme depois de alguns segundos (qualquer botão cancela). Tocar de novo volta a manter a
     * tela acesa.
     */
    private fun onEnded(ended: Boolean) {
        keepScreenOn(!ended)
        if (!ended) {
            endHandled = false
            cancelTurnOff(announce = false)
            return
        }
        if (endHandled) return
        endHandled = true
        if (!turnOffWhenDone || !canTurnOff) return
        if (!ScreenOffService.enabled) {
            Toast.makeText(this, R.string.player_turn_off_not_allowed, Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, R.string.player_turning_off, Toast.LENGTH_LONG).show()
        turnOffJob = lifecycleScope.launch {
            delay(TURN_OFF_DELAY_MS)
            turnOffJob = null
            ScreenOffService.turnOff()
        }
    }

    private fun cancelTurnOff(announce: Boolean) {
        val job = turnOffJob ?: return
        turnOffJob = null
        job.cancel()
        if (announce) Toast.makeText(this, R.string.player_turn_off_canceled, Toast.LENGTH_SHORT).show()
    }

    private fun keepScreenOn(on: Boolean) {
        playerView.keepScreenOn = on
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && turnOffJob != null) {
            cancelTurnOff(announce = true)
            return true
        }
        hidingKey?.let { key ->
            if (event.keyCode == key) {
                if (event.action == KeyEvent.ACTION_UP) hidingKey = null
                return true
            }
            hidingKey = null
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            keyStartedOpen = ::playerView.isInitialized && playerView.isControllerFullyVisible
        }
        if (event.action == KeyEvent.ACTION_DOWN && (event.repeatCount == 0 || keyStartedOpen) && hidesControls(event.keyCode)) {
            playerView.hideController()
            hidingKey = event.keyCode
            return true
        }
        // A tecla de avançar do controle remoto também pula o trecho (sem ele, o PlayerView avança 15 s).
        if (event.keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
            ::controller.isInitialized
        ) {
            val player = controller.player
            val from = player.currentPosition
            if (jumpAhead(from, from + player.seekForwardIncrement)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Descer da última linha dos controles, ou subir da primeira, esconde os controles na hora, sem
     * esperar o tempo (pedido do Johan na TV, 2026-11-04). Com um painel aberto, a seta fica com ele.
     */
    private fun hidesControls(keyCode: Int): Boolean {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            else -> return false
        }
        if (openPanel != null || !::playerView.isInitialized || !playerView.isControllerFullyVisible) return false
        val controls = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_controller) ?: return false
        val focused = controls.findFocus() ?: return false
        return focused.focusSearch(direction)?.isInside(controls) != true
    }

    private fun View.isInside(group: View) = generateSequence(this) { it.parent as? View }.any { it === group }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && turnOffJob != null) cancelTurnOff(announce = true)
        return super.dispatchTouchEvent(event)
    }

    /** A legenda (centralizada, [CaptionRules]) e o sentido da tela seguem o player. */
    private inner class PlayerScreenListener : Player.Listener {
        override fun onCues(cueGroup: CueGroup) {
            captionsView.setCues(CaptionRules.centered(cueGroup.cues))
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) = followVideo(videoSize)

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_PLAY_WHEN_READY_CHANGED, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_VIDEO_SIZE_CHANGED)) {
                updateMiniPlayer()
            }
        }
    }

    /** Celular e tablet: deitado para vídeo horizontal, em pé para vertical (a não ser que o botão tenha escolhido). */
    private fun followVideo(size: VideoSize) {
        if (!touch || chosenOrientation != null || size.width <= 0 || size.height <= 0) return
        val landscape = size.width * size.pixelWidthHeightRatio >= size.height
        requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }

    /** O botão de girar: troca entre deitado e em pé, e a escolha fica até fechar o player. */
    private fun rotate() {
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val next = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        chosenOrientation = next
        requestedOrientation = next
    }

    /**
     * O mini player flutuante (pedido do Johan, 2026-11-11): sair do app com o vídeo tocando deixa
     * o vídeo numa janelinha por cima dos outros apps. Nela fica só o vídeo; tocar/pausar são os do
     * Android, pela sessão de mídia do [PlaybackService]. Fechar a janelinha fecha o vídeo, e
     * tocar nela volta para o player.
     */
    private fun bindMiniPlayer() {
        addOnPictureInPictureModeChangedListener { showFloating(it.isInPictureInPictureMode) }
        // Onde o vídeo está na tela, para a janelinha sair dele.
        playerView.findViewById<View>(androidx.media3.ui.R.id.exo_content_frame)
            ?.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) updateMiniPlayer()
            }
    }

    private fun showFloating(on: Boolean) {
        floating = on
        if (on) {
            floatingPlayer = WeakReference(this)
            closePanel(focusButton = false)
            seekBadge.animate().cancel()
            seekBadge.visibility = View.GONE
            preview.visibility = View.GONE
        } else {
            if (floatingPlayer?.get() === this) floatingPlayer = null
            // A janelinha saiu com a tela já fora de vista: foi fechada (o X ou arrastar para baixo),
            // não aberta de volta. Fecha o vídeo, e a tarefa que o Android criou para a janelinha.
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                if (isTaskRoot) finishAndRemoveTask() else finish()
            }
        }
        // Sem os controles da tela na janelinha (escondê-los também baixa a legenda).
        playerView.useController = !on
    }

    private fun floats() =
        MiniPlayerRules.floats(touch, controller.player.playWhenReady, controller.player.playbackState, isFinishing)

    /** A forma da janelinha, de onde ela sai e, do Android 12 em diante, se ela abre sozinha ao sair do app. */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun miniPlayerParams(): PictureInPictureParams {
        val size = controller.player.videoSize
        val (width, height) = MiniPlayerRules.shape(size.width, size.height, size.pixelWidthHeightRatio)
        val params = PictureInPictureParams.Builder().setAspectRatio(Rational(width, height))
        val video = playerView.findViewById<View>(androidx.media3.ui.R.id.exo_content_frame)
        if (!floating && video != null && video.getGlobalVisibleRect(videoBounds) && !videoBounds.isEmpty) {
            params.setSourceRectHint(videoBounds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) params.setAutoEnterEnabled(floats())
        return params.build()
    }

    private fun updateMiniPlayer() {
        if (!miniPlayerSupported || !::controller.isInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching { setPictureInPictureParams(miniPlayerParams()) }
            .onFailure { Log.w(TAG, "couldn't set up the mini player", it) }
    }

    /** Até o Android 11 a janelinha abre à mão, ao sair do app; do 12 em diante, sozinha ([miniPlayerParams]). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.O until Build.VERSION_CODES.S) return
        if (!miniPlayerSupported || !::controller.isInitialized || floating || !floats()) return
        runCatching { enterPictureInPictureMode(miniPlayerParams()) }
            .onFailure { Log.w(TAG, "couldn't open the mini player", it) }
    }

    override fun finish() {
        super.finish()
        // Fechando o player (voltar, ou o canal do vídeo), sair não abre a janelinha.
        updateMiniPlayer()
    }

    /**
     * Toques no vídeo ([TouchRules]): um toque mostra ou esconde os controles (o PlayerView faz
     * isso no performClick); dois toques nos lados voltam ou avançam 10 s, e cada toque a mais do
     * mesmo lado logo em seguida soma outros 10 s; dois toques no meio pausam ou tocam. Avançar
     * dentro de um trecho que o público pula, ou logo antes dele, vai para o fim do trecho ([jumpAhead]).
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouchGestures() {
        val detector = GestureDetector(
            this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean = playerView.performClick()

                override fun onDoubleTap(e: MotionEvent): Boolean {
                    when (val zone = TouchRules.zone(e.x, playerView.width)) {
                        TouchRules.Zone.PLAY_PAUSE -> togglePlay()
                        else -> seek(zone)
                    }
                    return true
                }
            },
        )
        playerView.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                val zone = TouchRules.zone(event.x, playerView.width)
                streakTap = zone != TouchRules.Zone.PLAY_PAUSE && zone == streakZone && SystemClock.uptimeMillis() < streakUntil
            }
            val streak = streakZone
            if (streakTap && streak != null) {
                if (event.actionMasked == MotionEvent.ACTION_UP) seek(streak)
            } else {
                detector.onTouchEvent(event)
            }
            true
        }
    }

    private fun seek(zone: TouchRules.Zone) {
        val now = SystemClock.uptimeMillis()
        streakSeconds = if (zone == streakZone && now < streakUntil) streakSeconds + STEP_SECONDS else STEP_SECONDS
        streakZone = zone
        streakUntil = now + TouchRules.STREAK_MS
        val player = controller.player
        val from = player.currentPosition
        val to = TouchRules.seekTarget(from, player.duration, zone)
        if (zone == TouchRules.Zone.FORWARD && jumpAhead(from, to)) {
            // O próximo toque do mesmo lado volta a contar do zero: "+10 s" a partir do fim do trecho.
            streakSeconds = 0
            return
        }
        player.seekTo(to)
        val back = zone == TouchRules.Zone.BACK
        flash(
            text = getString(if (back) R.string.player_seek_back else R.string.player_seek_forward, streakSeconds),
            icon = 0,
            gravity = (if (back) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL,
        )
    }

    /**
     * A barra de tempo: o nome do capítulo acompanha a posição, e arrastar a barra mostra o preview
     * ([showPreview]). Na TV, direita na barra avança o vídeo (o DefaultTimeBar arrasta e solta
     * sozinho 1 s depois da última tecla); se o avanço começou dentro de um trecho que o público
     * pula, ou logo antes dele, vai para o fim do trecho. Este ouvinte entra depois do do
     * PlayerControlView, então o pulo vem depois do seek normal e fica valendo.
     */
    private fun bindScrubbing() {
        timeBar.onPosition = { if (!scrubbing) showChapter(it) }
        timeBar.addListener(
            object : TimeBar.OnScrubListener {
                private var from = C.TIME_UNSET

                override fun onScrubStart(timeBar: TimeBar, position: Long) {
                    from = controller.player.currentPosition
                    scrubbing = true
                    closePanel(focusButton = false)
                    showPreview(position)
                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) = showPreview(position)

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                    scrubbing = false
                    preview.visibility = View.GONE
                    showChapter(position)
                    if (!touch && !canceled && from != C.TIME_UNSET && position > from) jumpAhead(from, position)
                    from = C.TIME_UNSET
                }
            },
        )
    }

    /** O nome do capítulo em que [positionMs] está, logo acima da ilha (some sem capítulos). */
    private fun showChapter(positionMs: Long) {
        val title = Chapters.at(chapters, positionMs)?.title
        if (chapterView.text?.toString() != title) chapterView.text = title
        chapterView.visibility = if (title != null && !scrubbing) View.VISIBLE else View.GONE
    }

    /**
     * O cartão acima da ilha ao arrastar a barra (pedido do Johan, 2026-11-03): a miniatura daquele
     * ponto (se o YouTube mandou o storyboard), o capítulo e o tempo, centrado no círculo da barra.
     */
    private fun showPreview(positionMs: Long) {
        chapterView.visibility = View.GONE
        val chapter = Chapters.at(chapters, positionMs)?.title
        previewChapter.text = chapter
        previewChapter.visibility = if (chapter != null) View.VISIBLE else View.GONE
        previewTime.text = Util.getStringForTime(timeText, timeFormat, positionMs)
        val board = storyboard
        previewImage.visibility = if (board != null) View.VISIBLE else View.GONE
        if (board != null) showFrame(board, positionMs)
        preview.visibility = View.VISIBLE
        placePreview(positionMs)
    }

    /** Centra o cartão no círculo da barra, sem sair da tela, logo acima da ilha. */
    private fun placePreview(positionMs: Long) {
        val root = preview.parent as? View ?: return
        val spot = IntArray(2)
        root.getLocationInWindow(spot)
        val rootX = spot[0]
        val rootY = spot[1]
        timeBar.getLocationInWindow(spot)
        val center = spot[0] - rootX + timeBar.xOf(positionMs)
        islandView.getLocationInWindow(spot)
        val islandTop = spot[1] - rootY
        val width = preview.layoutParams.width.toFloat()
        val edge = resources.getDimension(R.dimen.player_edge)
        val left = (center - width / 2).coerceIn(edge, (root.width - edge - width).coerceAtLeast(edge))
        preview.translationX = left
        // O cartão fica embaixo (layout_gravity bottom): sobe até 10 dp acima da ilha.
        preview.translationY = (islandTop - dp(10) - root.height).toFloat()
    }

    /** Mostra o quadro de [positionMs] na miniatura do preview, baixando a folha dele se faltar. */
    private fun showFrame(board: Storyboard, positionMs: Long) {
        val frame = board.frameAt(positionMs)
        previewFrame = frame
        val sheet = sheets[frame.sheet]
        if (sheet != null) drawFrame(sheet, board, frame) else previewImage.setImageDrawable(null)
        loadSheet(board, frame.sheet)
        // A seguinte também, para o arrastar não esperar na virada de folha.
        if (frame.sheet + 1 < board.sheetCount(controller.player.duration)) loadSheet(board, frame.sheet + 1)
    }

    private fun loadSheet(board: Storyboard, index: Int) {
        if (sheets.containsKey(index) || !sheetsLoading.add(index)) return
        val request = ImageRequest.Builder(this)
            .data(board.url(index))
            .size(CoilSize.ORIGINAL)
            .target(
                onError = { sheetsLoading.remove(index) },
                onSuccess = { image ->
                    sheetsLoading.remove(index)
                    if (board !== storyboard) return@target
                    val bitmap = image.toBitmap()
                    sheets[index] = bitmap
                    val frame = previewFrame
                    if (frame?.sheet == index && preview.visibility == View.VISIBLE) drawFrame(bitmap, board, frame)
                },
            )
            .build()
        SingletonImageLoader.get(this).enqueue(request)
    }

    /** Recorta o quadro da folha: a folha inteira na ImageView, com uma matriz que mostra só ele. */
    private fun drawFrame(sheet: Bitmap, board: Storyboard, frame: Storyboard.Frame) {
        val frameWidth = sheet.width / board.cols.toFloat()
        val frameHeight = sheet.height / board.rows.toFloat()
        val viewWidth = previewImage.width.takeIf { it > 0 } ?: previewImage.layoutParams.width
        val viewHeight = previewImage.height.takeIf { it > 0 } ?: previewImage.layoutParams.height
        if (frameWidth <= 0 || frameHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
        // Preenche o 16:9 da miniatura (corta um pouco se o quadro tiver outra proporção).
        val scale = maxOf(viewWidth / frameWidth, viewHeight / frameHeight)
        previewMatrix.setScale(scale, scale)
        previewMatrix.postTranslate(
            -frame.col * frameWidth * scale + (viewWidth - frameWidth * scale) / 2,
            -frame.row * frameHeight * scale + (viewHeight - frameHeight * scale) / 2,
        )
        if ((previewImage.drawable as? BitmapDrawable)?.bitmap !== sheet) previewImage.setImageBitmap(sheet)
        previewImage.imageMatrix = previewMatrix
        previewImage.invalidate()
    }

    /**
     * Um avanço de [from] até [to] que começa dentro de um trecho que o público pula, ou logo antes
     * dele, vai direto para o fim do trecho, como no app do YouTube ([PlaybackController.jumpAhead]).
     * false = avanço normal, a cargo de quem chamou.
     */
    private fun jumpAhead(from: Long, to: Long): Boolean {
        val end = controller.jumpAhead(from, to) ?: return false
        controller.player.seekTo(end)
        flash(
            text = getString(R.string.player_jumped_ahead),
            icon = R.drawable.ic_skip_ahead,
            gravity = Gravity.END or Gravity.CENTER_VERTICAL,
        )
        return true
    }

    private fun togglePlay() {
        val player = controller.player
        // O ícone mostra o que acabou de acontecer.
        val icon = if (Util.shouldShowPlayButton(player)) R.drawable.ic_play else R.drawable.ic_pause
        Util.handlePlayPauseButtonAction(player)
        flash(text = "", icon = icon, gravity = Gravity.CENTER)
    }

    /** Um aviso rápido por cima do vídeo (o pulo de 10 s, o do trecho ou o tocar/pausar). */
    private fun flash(text: String, icon: Int, gravity: Int) {
        seekBadge.animate().cancel()
        seekBadge.text = text
        seekBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        val params = seekBadge.layoutParams as FrameLayout.LayoutParams
        params.gravity = gravity
        val side = (playerView.width * 0.12f).toInt()
        params.marginStart = side
        params.marginEnd = side
        seekBadge.layoutParams = params
        seekBadge.alpha = 1f
        seekBadge.visibility = View.VISIBLE
        seekBadge.animate().alpha(0f).setStartDelay(FLASH_MS).setDuration(FLASH_FADE_MS)
            .withEndAction { seekBadge.visibility = View.GONE }
    }

    private fun ImageButton.setActive(active: Boolean) {
        setColorFilter(color(if (active) R.color.player_accent else R.color.player_icon))
        isActivated = active
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onStart() {
        super.onStart()
        if (!::controller.isInitialized) return
        playerView.requestFocus()
        if (touch) {
            // O serviço que deixa o vídeo continuar com a tela desligada começa com a tela na frente.
            controller.sessionPlayer?.let { PlaybackService.attach(this, it) }
            controller.setBackground(false)
            updateMiniPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!::controller.isInitialized) return
        // Saiu do app durante a contagem: não desliga nada.
        cancelTurnOff(announce = false)
        controller.saveProgress()
        val screenOn = getSystemService(PowerManager::class.java)?.isInteractive != false
        // Tela desligada no meio do vídeo (celular e tablet): continua tocando (pedido do Johan, 2026-11-11).
        if (!ScreenOffRules.keepsPlaying(touch, screenOn, controller.player.playWhenReady)) controller.player.pause()
        // Fora da tela, sem imagem: só o som, se continuar (ou se voltar a tocar pela notificação).
        if (touch && !isFinishing) controller.setBackground(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::controller.isInitialized) return
        outState.putLong(KEY_POSITION, controller.player.currentPosition)
        controller.videoId?.let { outState.putString(KEY_VIDEO, it) }
        controller.titleHint?.let { outState.putString(KEY_TITLE, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingPlayer?.get() === this) floatingPlayer = null
        if (!::controller.isInitialized) return
        // A notificação e a sessão saem antes de o player ser solto.
        controller.sessionPlayer?.let { PlaybackService.detach(this, it) }
        controller.release()
    }

    private fun buildLayout(initialTitle: String?): View {
        playerView = layoutInflater.inflate(R.layout.player_view, null) as PlayerView
        titleView = playerView.findViewById<TextView>(R.id.player_title).apply { text = initialTitle }
        detailView = playerView.findViewById(R.id.player_detail)
        actions = playerView.findViewById(R.id.player_actions)
        likeButton = playerView.findViewById(R.id.player_like)
        dislikeButton = playerView.findViewById(R.id.player_dislike)
        subscribeButton = playerView.findViewById(R.id.player_subscribe)
        saveButton = playerView.findViewById(R.id.player_save)
        channelButton = playerView.findViewById(R.id.player_channel)
        captionsButton = playerView.findViewById(R.id.player_captions)
        sleepButton = playerView.findViewById(R.id.player_sleep)
        rotateButton = playerView.findViewById<ImageButton>(R.id.player_rotate).apply {
            visibility = if (touch) View.VISIBLE else View.GONE
        }
        island = PlayerIsland(playerView, tv = !touch).apply { fit(resources.configuration.screenWidthDp) }
        timeBar = playerView.findViewById<ChapterTimeBar>(R.id.exo_progress).apply {
            // Esquerda e direita na barra andam 10 s, como os dois toques no celular (achado do Johan
            // na TV, 2026-11-04: o padrão do Media3 anda 1/20 do vídeo, minutos num vídeo longo).
            setKeyTimeIncrement(TouchRules.SEEK_STEP_MS)
        }
        chapterView = playerView.findViewById(R.id.player_chapter)
        islandView = playerView.findViewById(R.id.player_island)
        preview = playerView.findViewById(R.id.player_preview)
        previewImage = playerView.findViewById<ImageView>(R.id.player_preview_image).apply { clipToOutline = true }
        previewChapter = playerView.findViewById(R.id.player_preview_chapter)
        previewTime = playerView.findViewById(R.id.player_preview_time)
        sleepPanel = playerView.findViewById(R.id.player_sleep_panel)
        qualityButton = playerView.findViewById(R.id.player_quality_button)
        liveButton = playerView.findViewById(R.id.player_live)
        positionView = playerView.findViewById(androidx.media3.ui.R.id.exo_position)
        durationView = playerView.findViewById(androidx.media3.ui.R.id.exo_duration)
        qualityPanel = playerView.findViewById(R.id.player_quality_panel)
        qualityRow = playerView.findViewById(R.id.player_quality_options)
        brightnessRow = playerView.findViewById(R.id.player_sleep_brightness)
        queueRow = playerView.findViewById(R.id.player_sleep_queue)
        endRow = playerView.findViewById(R.id.player_sleep_end)

        // Por cima do vídeo e da legenda, por baixo dos controles.
        dimView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        (playerView.overlayFrameLayout ?: playerView).addView(dimView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // A legenda no estilo do YouTube ([CaptionRules]). Fica no lugar da do PlayerView, que
        // seguiria a posição e o fundo que vêm do YouTube; as falas chegam pelo PlayerScreenListener.
        captionsView = SubtitleView(this).apply {
            setApplyEmbeddedStyles(false)
            setApplyEmbeddedFontSizes(false)
            setStyle(CaptionRules.style(ResourcesCompat.getFont(this@PlayerActivity, R.font.figtree_semibold)))
            setUserDefaultTextSize()
            // Uma margem pequena da borda de baixo, só para a legenda não encostar nela.
            setPadding(0, 0, 0, resources.getDimensionPixelSize(R.dimen.player_captions_bottom))
        }
        val original = playerView.subtitleView
        val captionsParent = original?.parent as? ViewGroup
        if (original != null && captionsParent != null) {
            original.visibility = View.GONE
            captionsParent.addView(captionsView, captionsParent.indexOfChild(original) + 1, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        } else {
            (playerView.overlayFrameLayout ?: playerView).addView(captionsView, 0, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        }
        captionLift = CaptionLift(captionsView, listOf(chapterView, islandView))

        // O aviso dos toques no vídeo (celular e tablet), por cima do escurecer.
        seekBadge = TextView(this).apply {
            setBackgroundResource(R.drawable.player_badge_bg)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = ResourcesCompat.getFont(this@PlayerActivity, R.font.figtree_semibold)
            compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
            gravity = Gravity.CENTER
            minWidth = dp(64)
            minHeight = dp(48)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            visibility = View.GONE
        }
        (playerView.overlayFrameLayout ?: playerView).addView(
            seekBadge,
            FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER),
        )

        messageView = TextView(this).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            val pad = dp(32)
            setPadding(pad, pad, pad, pad)
            visibility = View.GONE
        }

        return FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(playerView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(messageView, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER))
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::island.isInitialized) island.fit(newConfig.screenWidthDp)
    }

    /** @param resumeMs onde o card do YouTube diz que o vídeo parou (0 = não diz). */
    private data class Target(val videoId: String, val startMs: Long, val external: Boolean, val resumeMs: Long = 0)

    private fun linkText(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }

    /** Vídeo pedido: de dentro do app (extra) ou de um link externo (VIEW ou SEND). */
    private fun resolveTarget(intent: Intent): Target? {
        intent.getStringExtra(EXTRA_VIDEO_ID)?.let { return Target(it, 0, external = false, resumeMs = intent.getLongExtra(EXTRA_RESUME, 0L)) }
        val link = VideoLinks.parse(linkText(intent)) ?: return null
        return Target(link.videoId, link.startSeconds * 1000, external = true)
    }

    private fun playlistLink(intent: Intent): String? = VideoLinks.parsePlaylist(linkText(intent))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "Doorglow"
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_REQUESTED_AT = "requested_at"
        private const val EXTRA_RESUME = "resume_ms"
        private const val EXTRA_BACK_TO_PLAYER = "back_to_player"
        private const val KEY_POSITION = "position"
        private const val KEY_VIDEO = "video"
        private const val KEY_TITLE = "video_title"

        /** O mesmo show_timeout do player_view. */
        private const val CONTROLS_TIMEOUT_MS = 4000

        /** De quanto em quanto tempo o botão LIVE confere se o vídeo está no ao vivo. */
        private const val LIVE_TICK_MS = 1000L

        /** Até esta distância do ponto ao vivo o vídeo ainda conta como ao vivo (o player oscila um pouco). */
        private const val LIVE_SLACK_MS = 10_000L

        /** A opção "Auto" do painel de resolução (as outras são as alturas). */
        private const val AUTO_QUALITY = 0

        /** Opções de brilho do painel de dormir, em %. */
        private val BRIGHTNESS_LEVELS = listOf(100, 50, 25, 10, 0)

        /**
         * O último brilho escolhido, guardado só na memória: vale para os próximos vídeos até o app
         * fechar (ou a TV desligar), e o app abre de novo com o vídeo claro.
         */
        private var sessionBrightness = 100

        /** O player que está no mini player agora, se houver (um vídeo novo o fecha). */
        private var floatingPlayer: WeakReference<PlayerActivity>? = null

        /** Tempo para cancelar o "Turn off" depois que os vídeos acabam. */
        private const val TURN_OFF_DELAY_MS = 10_000L

        private const val STEP_SECONDS = (TouchRules.SEEK_STEP_MS / 1000).toInt()

        /** Folhas do storyboard guardadas (cada uma tem 25 quadros, uns 50 s de vídeo). */
        private const val SHEETS_KEPT = 4

        /** O aviso dos toques fica este tempo e some em [FLASH_FADE_MS]. */
        private const val FLASH_MS = 450L
        private const val FLASH_FADE_MS = 250L

        /**
         * O toque na notificação do vídeo tocando com a tela desligada ([PlaybackService]). Com o
         * player em cima da tarefa do app, o Android só a traz para a frente (SINGLE_TOP); senão, a
         * tela nova o procura e fecha ([PlaybackService.bringPlayerToFront]).
         */
        fun backIntent(context: Context): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_BACK_TO_PLAYER, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        /** @param resumeMs onde o card do YouTube diz que o vídeo parou ([Video.resumeMs]). */
        fun intent(context: Context, videoId: String, title: String?, resumeMs: Long = 0): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_RESUME, resumeMs)
                .putExtra(EXTRA_REQUESTED_AT, SystemClock.elapsedRealtime())
    }
}
