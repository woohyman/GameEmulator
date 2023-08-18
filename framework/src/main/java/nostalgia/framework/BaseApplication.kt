package nostalgia.framework

import android.app.Application
import com.blankj.utilcode.util.Utils
import nostalgia.framework.utils.EmuUtils
import nostalgia.framework.utils.NLog

abstract class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        val debug = EmuUtils.isDebuggable(this)
        NLog.setDebugMode(debug)
    }

    abstract fun hasGameMenu(): Boolean
}