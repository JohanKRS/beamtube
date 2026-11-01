package ytslim

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ytslim.data.VideoLinks
import ytslim.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = isTv()
        if (!tv) enableEdgeToEdge()
        // youtube.com/playlist?list=… vindo de outro app abre direto a página da playlist.
        val playlistId = if (savedInstanceState == null) VideoLinks.parsePlaylist(intent?.dataString) else null
        setContent { AppRoot(isTv = tv, openPlaylistId = playlistId) }
    }

    companion object {
        fun playlistIntent(context: Context, playlistId: String): Intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=$playlistId"), context, MainActivity::class.java)
    }
}

/** Um APK só: a interface de TV ou de toque é escolhida aqui, na abertura. */
fun Context.isTv(): Boolean {
    val uiMode = getSystemService(UiModeManager::class.java)?.currentModeType
    return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}
