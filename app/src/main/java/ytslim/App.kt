package ytslim

import android.app.Application
import ytslim.data.AppIcons
import ytslim.data.Engine
import ytslim.data.PlaylistSubscriptions
import ytslim.data.SettingsRepository

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
        SettingsRepository.init(this)
        PlaylistSubscriptions.init(this)
        AppIcons.init(this)
    }
}
