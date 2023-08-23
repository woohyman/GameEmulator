package com.woohyman.xml

import android.app.Application
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.NLog

abstract class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        val debug = EmuUtils.isDebuggable(this)
        NLog.setDebugMode(debug)
    }

    abstract fun hasGameMenu(): Boolean
}