package ytslim.player

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import kotlinx.coroutines.launch
import ytslim.MainActivity
import ytslim.R
import ytslim.data.AppScope
import ytslim.data.ExternalLinks
import ytslim.data.Rating
import ytslim.data.SettingsRepository
import ytslim.data.VideoLinks

/**
 * Tela do player, que é também a "página do vídeo": like/dislike e inscrição ficam nos
 * controles. Usa Views porque o PlayerView do Media3 já cuida do D-pad da TV e do toque.
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

    private var saveHistory = true
    private var external = false

    /** O título do card que abriu o vídeo (links de outros apps não têm). */
    private var cardTitle: String? = null

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

        controller = PlaybackController(this, AppScope)
        cardTitle = intent.getStringExtra(EXTRA_TITLE)?.takeIf { it.isNotBlank() }
        setContentView(buildLayout(cardTitle))
        playerView.player = controller.player
        bindButtons()

        val startMs = savedInstanceState?.getLong(KEY_POSITION) ?: target.startMs
        lifecycleScope.launch {
            if (!target.external) return@launch start(target.videoId, startMs, saveHistory = true)
            // Link de outro app: o padrão é não ir para o histórico (configurável).
            when (SettingsRepository.current().externalLinks) {
                ExternalLinks.NO_HISTORY -> start(target.videoId, startMs, saveHistory = false)
                ExternalLinks.SAVE_HISTORY -> start(target.videoId, startMs, saveHistory = true)
                ExternalLinks.ASK -> ask { save -> start(target.videoId, startMs, save) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { controller.state.collect(::render) }
                launch { controller.controls.collect(::renderControls) }
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

    private fun start(videoId: String, startMs: Long, saveHistory: Boolean) {
        this.saveHistory = saveHistory
        controller.open(videoId, startMs, saveHistory)
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
            PlaybackController.State.Loading -> messageView.visibility = View.GONE
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
     * card que abriu o vídeo; e, nos links de outros apps, o da página do vídeo.
     */
    private fun showTitle() {
        val info = (controller.state.value as? PlaybackController.State.Playing)?.info
        val title = info?.title ?: cardTitle ?: controller.controls.value.page?.title ?: return
        if (titleView.text?.toString() != title) titleView.text = title
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
        controller.saveProgress()
        controller.player.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::controller.isInitialized) outState.putLong(KEY_POSITION, controller.player.currentPosition)
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

        fun intent(context: Context, videoId: String, title: String?): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_ID, videoId)
                .putExtra(EXTRA_TITLE, title)
    }
}
