package ytslim.player

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
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch
import ytslim.R
import ytslim.data.AppScope
import ytslim.data.VideoLinks

/**
 * Tela do player. Usa Views em vez de Compose porque o PlayerView do Media3 já cuida do
 * D-pad da TV e do toque no celular.
 */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    private lateinit var controller: PlaybackController
    private lateinit var playerView: PlayerView
    private lateinit var titleView: TextView
    private lateinit var detailView: TextView
    private lateinit var messageView: TextView
    private lateinit var infoBar: View

    private var external = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = resolveTarget(intent)
        if (target == null) {
            Toast.makeText(this, R.string.link_not_a_video, Toast.LENGTH_LONG).show()
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
        setContentView(buildLayout(intent.getStringExtra(EXTRA_TITLE)))
        playerView.player = controller.player

        controller.open(
            videoId = target.videoId,
            startPositionMs = savedInstanceState?.getLong(KEY_POSITION) ?: target.startMs,
            // Link externo (WhatsApp, navegador, Compartilhar): não entra no histórico.
            saveHistory = !target.external,
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                controller.state.collect(::render)
            }
        }
    }

    private fun render(state: PlaybackController.State) {
        when (state) {
            PlaybackController.State.Loading -> messageView.visibility = View.GONE
            is PlaybackController.State.Playing -> {
                messageView.visibility = View.GONE
                titleView.text = state.info.title
                val noHistory = if (external) getString(R.string.player_no_history) else null
                detailView.text = listOfNotNull(state.info.author, state.selection, noHistory).joinToString(" · ")
            }
            is PlaybackController.State.Failed -> {
                messageView.text = state.message
                messageView.visibility = View.VISIBLE
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerView.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        controller.saveProgress()
        controller.player.pause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_POSITION, controller.player.currentPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::controller.isInitialized) controller.release()
    }

    private fun buildLayout(initialTitle: String?): View {
        playerView = PlayerView(this).apply {
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowShuffleButton(false)
            setShowSubtitleButton(true)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            keepScreenOn = true
            isFocusable = true
            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                infoBar.visibility = visibility
            })
        }

        titleView = TextView(this).apply {
            text = initialTitle
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            maxLines = 2
        }
        detailView = TextView(this).apply {
            setTextColor(0xCCFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
        infoBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0x66000000)
            addView(titleView)
            addView(detailView)
        }
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
            addView(infoBar, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.TOP))
            addView(messageView, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER))
        }
    }

    private data class Target(val videoId: String, val startMs: Long, val external: Boolean)

    /** Vídeo pedido: de dentro do app (extra) ou de um link externo (VIEW ou SEND). */
    private fun resolveTarget(intent: Intent): Target? {
        intent.getStringExtra(EXTRA_VIDEO_ID)?.let { return Target(it, 0, external = false) }
        val text = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        val link = VideoLinks.parse(text) ?: return null
        return Target(link.videoId, link.startSeconds * 1000, external = true)
    }

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
