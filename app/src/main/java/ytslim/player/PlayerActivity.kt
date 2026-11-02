package ytslim.player

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import ytslim.MainActivity
import ytslim.R
import ytslim.data.AppScope
import ytslim.data.ExternalLinks
import ytslim.data.PlayQueue
import ytslim.data.Rating
import ytslim.data.SettingsRepository
import ytslim.data.VideoLinks

/**
 * Tela do player, que é também a "página do vídeo": like/dislike e inscrição ficam nos
 * controles. Usa Views porque o PlayerView do Media3 já cuida do D-pad da TV e do toque.
 *
 * O botão da lua abre o painel de dormir: escurecer o vídeo, quantos vídeos da playlist ainda
 * tocam e o que fazer quando eles acabarem (desligar a TV, pelo [ScreenOffService]).
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
    private lateinit var skipButton: TextView
    private lateinit var captionsButton: ImageButton
    private lateinit var sleepButton: ImageButton
    private lateinit var sleepPanel: View
    private lateinit var brightnessRow: LinearLayout
    private lateinit var queueRow: LinearLayout
    private lateinit var endRow: LinearLayout
    private lateinit var dimView: View

    private var saveHistory = true
    private var external = false

    /** Brilho do vídeo no modo dormir, em %. Volta a 100 a cada player aberto. */
    private var brightness = 100

    /** "When done: Turn off", guardado nas configurações. */
    private var turnOffWhenDone = false

    /** O fim atual já foi tratado (a tela só desliga uma vez por fim). */
    private var endHandled = false
    private var turnOffJob: Job? = null

    private val closePanelOnBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeSleepPanel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = resolveTarget(intent)
        if (target == null) {
            // Um link de playlist compartilhado vai para a página da playlist.
            playlistLink(intent)?.let { startActivity(MainActivity.playlistIntent(this, it)) }
                ?: Toast.makeText(this, R.string.link_not_a_video, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        external = target.external

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Um link de outro app não continua a sequência de uma playlist aberta antes.
        if (target.external && savedInstanceState == null) PlayQueue.clear()

        controller = PlaybackController(this, AppScope)
        // Recriada no meio de uma sequência, a tela volta ao vídeo que estava tocando.
        val videoId = savedInstanceState?.getString(KEY_VIDEO) ?: target.videoId
        val restored = savedInstanceState?.containsKey(KEY_VIDEO) == true
        val title = (if (restored) savedInstanceState?.getString(KEY_TITLE) else intent.getStringExtra(EXTRA_TITLE))
            ?.takeIf { it.isNotBlank() }
        setContentView(buildLayout(title))
        playerView.player = controller.player
        bindButtons()
        buildSleepPanel()
        onBackPressedDispatcher.addCallback(this, closePanelOnBack)

        val startMs = savedInstanceState?.getLong(KEY_POSITION) ?: target.startMs
        lifecycleScope.launch {
            turnOffWhenDone = SettingsRepository.current().turnOffWhenDone
            renderSleep()
            if (!target.external) return@launch start(videoId, startMs, saveHistory = true, title)
            // Link de outro app: o padrão é não ir para o histórico (configurável).
            when (SettingsRepository.current().externalLinks) {
                ExternalLinks.NO_HISTORY -> start(videoId, startMs, saveHistory = false, title)
                ExternalLinks.SAVE_HISTORY -> start(videoId, startMs, saveHistory = true, title)
                ExternalLinks.ASK -> ask { save -> start(videoId, startMs, save, title) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { controller.state.collect(::render) }
                launch { controller.controls.collect(::renderControls) }
                launch { controller.ended.collect(::onEnded) }
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

    private fun start(videoId: String, startMs: Long, saveHistory: Boolean, title: String?) {
        this.saveHistory = saveHistory
        controller.open(videoId, startMs, saveHistory, title)
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
            PlaybackController.State.Loading -> {
                // Também o próximo vídeo da sequência: título novo, sem os detalhes do anterior.
                messageView.visibility = View.GONE
                showTitle()
                detailView.text = null
            }
            is PlaybackController.State.Playing -> {
                messageView.visibility = View.GONE
                showTitle()
                val noHistory = if (!saveHistory) getString(R.string.player_no_history) else null
                detailView.text = listOfNotNull(state.info.author, state.selection, noHistory).joinToString(" · ")
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

    private fun renderControls(controls: PlaybackController.Controls) {
        showTitle()
        val page = controls.page
        actions.visibility = if (page != null) View.VISIBLE else View.GONE
        if (page != null) {
            likeButton.setActive(page.rating == Rating.LIKED)
            dislikeButton.setActive(page.rating == Rating.DISLIKED)
            subscribeButton.visibility = if (page.channelId != null) View.VISIBLE else View.GONE
            subscribeButton.setText(if (page.subscribed) R.string.player_subscribed else R.string.player_subscribe)
            subscribeButton.setTextColor(color(if (page.subscribed) R.color.player_text_secondary else R.color.player_icon))
            channelButton.visibility = if (page.channelId != null && !page.channelName.isNullOrBlank()) View.VISIBLE else View.GONE
            channelButton.text = page.channelName
        }
        saveButton.visibility = if (controls.saved != null) View.VISIBLE else View.GONE
        saveButton.setImageResource(if (controls.saved == true) R.drawable.ic_bookmark else R.drawable.ic_bookmark_border)
        saveButton.setActive(controls.saved == true)
        saveButton.contentDescription = getString(if (controls.saved == true) R.string.player_saved else R.string.player_save)
        skipButton.visibility = if (controls.skipAvailable) View.VISIBLE else View.GONE
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
            startActivity(MainActivity.channelIntent(this, channelId, page.channelName.orEmpty()))
            finish()
        }
        captionsButton.setOnClickListener { controller.toggleCaptions() }
        skipButton.setOnClickListener {
            if (!controller.skipAhead()) Toast.makeText(this, R.string.player_no_highlight, Toast.LENGTH_SHORT).show()
        }
        sleepButton.setOnClickListener { if (sleepPanel.visibility == View.VISIBLE) closeSleepPanel() else openSleepPanel() }
        // O painel some junto com os controles.
        playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility != View.VISIBLE) closeSleepPanel(focusButton = false)
            },
        )
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
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, dp(40)).apply { marginEnd = dp(8) }
            setOnClickListener { onClick() }
        }

    private fun renderSleep() {
        if (!::sleepPanel.isInitialized) return
        brightnessRow.select(brightness)
        queueRow.visibility = if (PlayQueue.active.value) View.VISIBLE else View.GONE
        queueRow.select(PlayQueue.left.value)
        // Sem a ação de dormir do sistema (antes do Android 9), não há o que escolher.
        endRow.visibility = if (ScreenOffService.supported) View.VISIBLE else View.GONE
        endRow.select(turnOffWhenDone)
        val limited = PlayQueue.active.value && PlayQueue.left.value != PlayQueue.ALL
        sleepButton.setActive(brightness < 100 || turnOffWhenDone || limited)
    }

    /** Marca a opção de [value] na linha (a primeira view, sem tag, é o nome da linha). */
    private fun LinearLayout.select(value: Any) {
        for (i in 0 until childCount) {
            val option = getChildAt(i) as? TextView ?: continue
            val tag = option.tag ?: continue
            val on = tag == value
            option.isActivated = on
            option.setTextColor(color(if (on) R.color.player_accent else R.color.player_icon))
        }
    }

    private fun openSleepPanel() {
        renderSleep()
        sleepPanel.visibility = View.VISIBLE
        closePanelOnBack.isEnabled = true
        // Com o painel aberto, os controles não somem sozinhos (no celular os toques não contam).
        playerView.controllerShowTimeoutMs = 0
        playerView.showController()
        brightnessRow.findViewWithTag<View>(brightness)?.requestFocus()
    }

    private fun closeSleepPanel(focusButton: Boolean = true) {
        if (sleepPanel.visibility != View.VISIBLE) return
        sleepPanel.visibility = View.GONE
        closePanelOnBack.isEnabled = false
        playerView.controllerShowTimeoutMs = CONTROLS_TIMEOUT_MS
        if (focusButton) {
            playerView.showController()
            sleepButton.requestFocus()
        }
    }

    /** Escurece o vídeo (a legenda junto), sem mexer nos controles nem no brilho da TV. */
    private fun setBrightness(level: Int) {
        brightness = level
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
        if (on && ScreenOffService.supported && !ScreenOffService.enabled) {
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
        if (!turnOffWhenDone || !ScreenOffService.supported) return
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
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && turnOffJob != null) cancelTurnOff(announce = true)
        return super.dispatchTouchEvent(event)
    }

    private fun ImageButton.setActive(active: Boolean) {
        setColorFilter(color(if (active) R.color.player_accent else R.color.player_icon))
        isActivated = active
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    override fun onStart() {
        super.onStart()
        playerView.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        if (!::controller.isInitialized) return
        // Saiu do app durante a contagem: não desliga nada.
        cancelTurnOff(announce = false)
        controller.saveProgress()
        controller.player.pause()
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
        if (::controller.isInitialized) controller.release()
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
        skipButton = playerView.findViewById(R.id.player_skip)
        captionsButton = playerView.findViewById(R.id.player_captions)
        sleepButton = playerView.findViewById(R.id.player_sleep)
        sleepPanel = playerView.findViewById(R.id.player_sleep_panel)
        brightnessRow = playerView.findViewById(R.id.player_sleep_brightness)
        queueRow = playerView.findViewById(R.id.player_sleep_queue)
        endRow = playerView.findViewById(R.id.player_sleep_end)

        // Por cima do vídeo e da legenda, por baixo dos controles.
        dimView = View(this).apply {
            setBackgroundColor(Color.BLACK)
            visibility = View.GONE
        }
        (playerView.overlayFrameLayout ?: playerView).addView(dimView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

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

    private data class Target(val videoId: String, val startMs: Long, val external: Boolean)

    private fun linkText(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> null
    }

    /** Vídeo pedido: de dentro do app (extra) ou de um link externo (VIEW ou SEND). */
    private fun resolveTarget(intent: Intent): Target? {
        intent.getStringExtra(EXTRA_VIDEO_ID)?.let { return Target(it, 0, external = false) }
        val link = VideoLinks.parse(linkText(intent)) ?: return null
        return Target(link.videoId, link.startSeconds * 1000, external = true)
    }

    private fun playlistLink(intent: Intent): String? = VideoLinks.parsePlaylist(linkText(intent))

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val EXTRA_VIDEO_ID = "video_id"
        private const val EXTRA_TITLE = "title"
        private const val KEY_POSITION = "position"
        private const val KEY_VIDEO = "video"
        private const val KEY_TITLE = "video_title"

        /** O mesmo show_timeout do player_view. */
        private const val CONTROLS_TIMEOUT_MS = 4000

        /** Opções de brilho do painel de dormir, em %. */
        private val BRIGHTNESS_LEVELS = listOf(100, 50, 25, 10, 0)

        /** Tempo para cancelar o "Turn off" depois que os vídeos acabam. */
        private const val TURN_OFF_DELAY_MS = 10_000L

        fun intent(context: Context, videoId: String, title: String?): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
    }
}
