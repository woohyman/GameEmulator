package com.woohyman.xml.base.emulator

import android.preference.PreferenceManager
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate

class EmulatorMediator constructor(
    private val activity: EmulatorActivity,
) : IEmulatorMediator {

    val gameMenuProxy: GameMenuProxy = GameMenuProxy(activity)
    val emulatorManagerProxy: EmulatorManagerProxy = EmulatorManagerProxy(activity)
    val gameControlProxy: GameControlProxy = GameControlProxy(activity)
    val emulatorView = EmulatorViewProxy(activity)

    val maxPRC = 10
    var autoHide = false
    var warningShowing = atomic(false)
    var baseDir: String? = null

    init {
        activity.lifecycle.addObserver(this)
        activity.lifecycle.addObserver(gameMenuProxy)
        activity.lifecycle.addObserver(emulatorManagerProxy)
        activity.lifecycle.addObserver(gameControlProxy)
    }

    override fun onCreate(owner: LifecycleOwner) {
        NLog.d(EmulatorActivity.TAG, "onCreate - BaseActivity")
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        autoHide = PreferenceUtil.isAutoHideControls(activity)

        try {
            activity.slotToRun = 0
            val quality = PreferenceUtil.getEmulationQuality(activity)
            emulatorView.setQuality(quality)
            emulatorView.onResume()
            emulatorManagerProxy.enableCheats()
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

    fun onNotResponding(): Boolean {
        warningShowing.getAndUpdate {
            if (!it) {
                true
            } else {
                return false
            }
        }
        return true
    }

    fun hideTouchController() {
        NLog.i(EmulatorActivity.TAG, "hide controler")
        if (autoHide) {
            gameControlProxy.hideTouchController()
        }
    }
}