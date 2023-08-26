package com.woohyman.xml.base.emulator

import android.app.AlertDialog
import android.preference.PreferenceManager
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.emulator.EmulatorRunner
import com.woohyman.keyboard.utils.DialogUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import com.woohyman.xml.base.emulator.business.EmulatorManagerProxy
import com.woohyman.xml.base.emulator.business.EmulatorViewProxy
import com.woohyman.xml.base.emulator.business.GameControlProxy
import com.woohyman.xml.base.emulator.business.GameMenuDelegate
import com.woohyman.xml.ui.timetravel.TimeTravelDialog
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import java.lang.ref.WeakReference

class EmulatorMediator constructor(
    private val appCompatActivity: AppCompatActivity,
    val game: GameDescription,
    val emulatorInstance: Emulator,
    val fragmentShader: String,
) : IEmulatorMediator, EmulatorRunner.OnNotRespondingListener {

    val gameMenuProxy: GameMenuDelegate = GameMenuDelegate(this)
    val emulatorManagerProxy: EmulatorManagerProxy = EmulatorManagerProxy(this)
    val gameControlProxy: GameControlProxy = GameControlProxy(this)
    val emulatorView = EmulatorViewProxy(this)

    val activity: AppCompatActivity? get() = WeakReference(appCompatActivity).get()

    private val maxPRC = 10
    private var autoHide = false
    private var warningShowing = atomic(false)

    var isRestarting = false
    var canRestart = false
    var slotToRun: Int? = null
    var slotToSave: Int? = null
    var baseDir: String? = null

    init {
        appCompatActivity.lifecycle.addObserver(this)
        appCompatActivity.lifecycle.addObserver(gameMenuProxy)
        appCompatActivity.lifecycle.addObserver(emulatorManagerProxy)
        appCompatActivity.lifecycle.addObserver(gameControlProxy)
    }

    val dialog: TimeTravelDialog by lazy {
        TimeTravelDialog(Utils.getApp(), emulatorManagerProxy, game)
    }

    override fun onCreate(owner: LifecycleOwner) {
        NLog.d(EmulatorActivity.TAG, "onCreate - BaseActivity")
        super.onCreate(owner)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        autoHide = PreferenceUtil.isAutoHideControls(activity)

        try {
            slotToRun = 0
            val quality = PreferenceUtil.getEmulationQuality(activity)
            emulatorView.setQuality(quality)
            emulatorView.onResume()
            emulatorManagerProxy.enableCheats()
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    @MainThread
    override fun onNotResponding() {
        warningShowing.getAndUpdate {
            if (!it) {
                true
            } else {
                return
            }
        }
        val dialog = AlertDialog.Builder(Utils.getApp())
            .setMessage(R.string.too_slow)
            .create()
        dialog.setOnDismissListener { activity?.finish() }
        emulatorManagerProxy.pauseEmulation()
        DialogUtils.show(dialog, true)
    }

    fun setShouldPauseOnResume(b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
            .putBoolean("emulator_activity_pause", b).apply()
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

    fun hideTouchController() {
        NLog.i(EmulatorActivity.TAG, "hide controler")
        if (autoHide) {
            gameControlProxy.hideTouchController()
        }
    }

    fun quickSave() {
        emulatorManagerProxy.saveState(10)
    }

    fun quickLoad() {
        emulatorManagerProxy.loadState(10)
    }

    fun handleException(e: EmulatorException) {
        val dialog = AlertDialog.Builder(appCompatActivity)
            .setMessage(e.getMessage(appCompatActivity))
            .create()
        dialog.setOnDismissListener { appCompatActivity.finish() }
        DialogUtils.show(dialog, true)
    }
}