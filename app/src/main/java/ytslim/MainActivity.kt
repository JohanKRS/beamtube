package ytslim

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import ytslim.data.VideoLinks
import ytslim.ui.AppLanguage
import ytslim.ui.AppRoot
import ytslim.ui.Link

class MainActivity : ComponentActivity() {
    /** O idioma escolhido em Settings ([AppLanguage]). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
        AppLanguage.applyTo(this)
    }

    /** Página pedida de fora da navegação: link de playlist de outro app, ou o canal a partir do player. */
    private val links = MutableStateFlow<Link?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = isTv()
        if (!tv) drawEdgeToEdge()
        if (savedInstanceState == null) links.value = linkOf(intent)
        setContent { AppRoot(isTv = tv, links = links) }
    }

    /**
     * O app desenha a tela inteira: o vidro das barras passa por trás da barra de status, da barra de
     * gestos e das laterais. O recorte da câmera é ignorado (no tablet do Johan ele fica na lateral,
     * em retrato): o conteúdo vai por baixo dele, e só as barras do sistema afastam os botões.
     */
    private fun drawEdgeToEdge() {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
        val cutoutMode = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            else -> null
        }
        if (cutoutMode != null) window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = cutoutMode }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        linkOf(intent)?.let { links.value = it }
    }

    private fun linkOf(intent: Intent?): Link? {
        intent ?: return null
        intent.getStringExtra(EXTRA_CHANNEL_ID)?.let { return Link.Channel(it, intent.getStringExtra(EXTRA_CHANNEL_NAME).orEmpty()) }
        // youtube.com/playlist?list=… vindo de outro app abre direto a página da playlist.
        return VideoLinks.parsePlaylist(intent.dataString)?.let { Link.Playlist(it) }
    }

    companion object {
        private const val EXTRA_CHANNEL_ID = "channel_id"
        private const val EXTRA_CHANNEL_NAME = "channel_name"

        fun playlistIntent(context: Context, playlistId: String): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=$playlistId"), context, MainActivity::class.java)

        /** Abre a página do canal por cima do que já está aberto no app (vindo do player). */
        fun channelIntent(context: Context, channelId: String, name: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_CHANNEL_ID, channelId)
                .putExtra(EXTRA_CHANNEL_NAME, name)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

/** Um APK só: a interface de TV ou de toque é escolhida aqui, na abertura. */
fun Context.isTv(): Boolean {
    val uiMode = getSystemService(UiModeManager::class.java)?.currentModeType
    return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
