package ytslim

import android.app.Application
import ytslim.data.Engine

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Engine.init(this)
    }
}
