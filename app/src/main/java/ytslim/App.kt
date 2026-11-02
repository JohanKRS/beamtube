package ytslim

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import kotlinx.coroutines.launch
import ytslim.data.AppIcons
import ytslim.data.AppScope
import ytslim.data.Engine
import ytslim.data.ExploreRepository
import ytslim.data.PlaylistSubscriptions
import ytslim.data.SettingsRepository
import ytslim.player.ScreenOffService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        SettingsRepository.init(this)
        PlaylistSubscriptions.init(this)
        ExploreRepository.init(this)
        AppIcons.init(this)
        AppScope.launch { SettingsRepository.dropOldExplore() }
        if (!isTv()) removeScreenOffService()
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
