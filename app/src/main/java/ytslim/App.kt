package ytslim

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import androidx.profileinstaller.ProfileVerifier
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ytslim.data.AppIcons
import ytslim.data.AppScope
import ytslim.data.Engine
import ytslim.data.ExploreRepository
import ytslim.data.FeedCache
import ytslim.data.ListCache
import ytslim.data.PlaylistSubscriptions
import ytslim.data.SettingsRepository
import ytslim.player.ScreenOffService
import ytslim.ui.ImageHttp

class App : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        SettingsRepository.init(this)
        PlaylistSubscriptions.init(this)
        ExploreRepository.init(this)
        FeedCache.init(this)
        ListCache.init(this)
        AppIcons.init(this)
        AppScope.launch { SettingsRepository.dropOldExplore() }
        if (!isTv()) removeScreenOffService()
        logCompilation()
    }

    /** As miniaturas e as fotos dos canais vêm pelo carregador do app ([ImageHttp]). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageHttp.newImageLoader(context)

    /**
     * Se o Android já compilou o app com o perfil (ver baseline-prof.txt). Depois de cada atualização
     * ele volta a rodar sem compilar até o aparelho ficar parado um tempo; isto diz em que pé está
     * (tag Doorglow), para não medir desempenho num app ainda sem compilar.
     */
    private fun logCompilation() {
        AppScope.launch(Dispatchers.IO) {
            delay(10_000)
            runCatching {
                val status = ProfileVerifier.getCompilationStatusAsync().get()
                Log.i(
                    "Doorglow",
                    "compilation: compiled with profile ${status.isCompiledWithProfile}, " +
                        "profile waiting for compilation ${status.hasProfileEnqueuedForCompilation()}, " +
                        "install result ${status.profileInstallResultCode}",
                )
            }
        }
    }

    /**
     * Desligar no fim dos vídeos é só da TV: no celular e no tablet o serviço de Acessibilidade
     * sai da lista do sistema, em vez de ficar lá sem uso.
     */
    private fun removeScreenOffService() {
        val component = ComponentName(this, ScreenOffService::class.java)
        if (packageManager.getComponentEnabledSetting(component) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) return
        packageManager.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
    }
}
