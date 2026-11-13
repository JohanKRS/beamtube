package ytslim

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import androidx.profileinstaller.ProfileVerifier
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ytslim.data.AppIcons
import ytslim.data.AppScope
import ytslim.data.Engine
import ytslim.data.EngineWarmup
import ytslim.data.ExploreRepository
import ytslim.data.FeedCache
import ytslim.data.FirstRun
import ytslim.data.ListCache
import ytslim.data.PlaylistSubscriptions
import ytslim.data.WatchPositions
import ytslim.data.SettingsRepository
import ytslim.player.ScreenOffService
import ytslim.ui.Accents
import ytslim.ui.AppLanguage
import ytslim.ui.ImageHttp

class App : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        AppLanguage.init(this)
        AppVisibility.init(this)
        Engine.init(this)
        SettingsRepository.init(this)
        FirstRun.init(this)
        PlaylistSubscriptions.init(this)
        ExploreRepository.init(this)
        FeedCache.init(this)
        ListCache.init(this)
        WatchPositions.init(this)
        AppIcons.init(this)
        Accents.init(this)
        AppScope.launch { SettingsRepository.dropOldExplore() }
        // De volta ao app (a TV ligou de novo, outro app fechou): o token do YouTube e o da conta podem
        // ter vencido, e o primeiro vídeo esperaria por eles. O que ainda vale não é refeito.
        AppScope.launch { AppVisibility.returns.collect { if (Engine.signIn.isSigned) EngineWarmup.start() } }
        if (!isTv()) removeScreenOffService()
        logCompilation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        AppLanguage.onAppConfigurationChanged(this, newConfig)
    }

    /** As miniaturas e as fotos dos canais vêm pelo carregador do app ([ImageHttp]). */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageHttp.newImageLoader(context)

    /**
     * Se o Android já compilou o app com o perfil (ver baseline-prof.txt). Depois de cada atualização
     * ele volta a rodar sem compilar até o aparelho ficar parado um tempo; isto diz em que pé está
     * (tag BeamTube), para não medir desempenho num app ainda sem compilar.
     */
    private fun logCompilation() {
        AppScope.launch {
            delay(10_000)
            // Sem segurar uma thread esperando: a resposta só vem depois que o ProfileInstaller roda.
            // Com o get() numa thread do Dispatchers.IO, cada App que nunca recebe a resposta (nos
            // testes do Robolectric, um por teste) prendia uma das 64 threads, até não sobrar nenhuma
            // para o DataStore e os testes pararem (o CI do #26 travou assim).
            val status = ProfileVerifier.getCompilationStatusAsync()
            status.addListener({
                runCatching {
                    val result = status.get()
                    Log.i(
                        "BeamTube",
                        "compilation: compiled with profile ${result.isCompiledWithProfile}, " +
                            "profile waiting for compilation ${result.hasProfileEnqueuedForCompilation()}, " +
                            "install result ${result.profileInstallResultCode}",
                    )
                }
            }, Runnable::run)
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
