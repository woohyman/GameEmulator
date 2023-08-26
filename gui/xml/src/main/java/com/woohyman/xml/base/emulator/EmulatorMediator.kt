package com.woohyman.xml.base.emulator

import android.preference.PreferenceManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate

class EmulatorMediator constructor(
    private val activity: EmulatorActivity
) : DefaultLifecycleObserver {
    private val maxPRC = 10
    private var autoHide = false
    var warningShowing = atomic(false)
    var exceptionOccurred = false
    var baseDir: String? = null

    val gameControlProxy by lazy {
        GameControlProxy(activity, activity.emulatorInstance, activity.game)
    }

    val emulatorManagerProxy by lazy {
        EmulatorManagerProxy(activity, activity.emulatorInstance, activity.game)
    }

    val gameMenuProxy by lazy {
        GameMenuProxy(activity, activity.emulatorInstance,activity.game)
    }

    val emulatorView by lazy {
        EmulatorViewProxy(activity)
    }

    init {
        activity.lifecycle.addObserver(gameMenuProxy)
        activity.lifecycle.addObserver(emulatorManagerProxy)
        activity.lifecycle.addObserver(gameControlProxy)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        try {
            baseDir = EmulatorUtils.getBaseDir(activity)
        } catch (e: EmulatorException) {
            activity.handleException(e)
            exceptionOccurred = true
            return
        }
        NLog.d(EmulatorActivity.TAG, "onCreate - BaseActivity")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        autoHide = PreferenceUtil.isAutoHideControls(activity)

        try {
            activity.slotToRun = 0
            val quality = PreferenceUtil.getEmulationQuality(activity)
            emulatorView.setQuality(quality)
            emulatorView.onResume()
            activity.enableCheats()
        } catch (e: EmulatorException) {
            activity.handleException(e)
        }
    }

    fun setShouldPauseOnResume(b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
            .putBoolean("emulator_activity_pause", b)
            .apply()
    }

    fun shouldPause(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(Utils.getApp())
            .getBoolean("emulator_activity_pause", false)
    }

    fun decreaseResumesToRestart(): Int {
        var prc =
            PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).getInt("PRC", maxPRC)
        if (prc > 0) {
            prc--
        }
        val editor = PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
        editor.putInt("PRC", prc)
        editor.apply()
        return prc
    }

    fun resetProcessResetCounter() {
        val editor = PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
        editor.putInt("PRC", maxPRC)
        editor.apply()
    }

    fun onNotResponding(){
        warningShowing.getAndUpdate {
            if (!it) {
                true
            } else {
                return
            }
        }
    }

    fun hideTouchController() {
        NLog.i(EmulatorActivity.TAG, "hide controler")
        if (autoHide) {
            gameControlProxy.hideTouchController()
        }
    }
}