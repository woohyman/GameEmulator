package com.woohyman.xml.emulator

import android.app.Activity
import android.app.AlertDialog
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.DialogUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import com.woohyman.xml.ui.timetravel.TimeTravelDialog
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import javax.inject.Inject

class EmulatorMediator @Inject constructor(

) : IEmulatorMediator {

    private val activity by lazy {
        ActivityUtils.getTopActivity()!!
    }

    private val emulatorViewModel by lazy {
        ViewModelProvider(activity as AppCompatActivity)[EmulatorViewModel::class.java]
    }

    override val gameMenuProxy by lazy {
        emulatorViewModel.gameMenuProxy
    }

    override val emulatorManagerProxy by lazy {
        emulatorViewModel.emulatorManagerProxy
    }

    override val gameControlProxy by lazy {
        emulatorViewModel.gameControlProxy
    }

    override val emulatorView by lazy {
        emulatorViewModel.emulatorView
    }

    private val maxPRC = 10
    private var autoHide = false
    private var warningShowing = atomic(false)

    override var isRestarting = false
    override var canRestart = false
    override var slotToRun: Int? = null
    override var slotToSave: Int? = null
    override var baseDir: String? = null

    override val dialog: TimeTravelDialog by lazy {
        TimeTravelDialog(Utils.getApp(), emulatorManagerProxy)
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
        dialog.setOnDismissListener { (activity as? Activity)?.finish() }
        emulatorManagerProxy.pauseEmulation()
        DialogUtils.show(dialog, true)
    }

    override fun setShouldPauseOnResume(b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
            .putBoolean("emulator_activity_pause", b).apply()
    }

    override fun shouldPause(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(Utils.getApp())
            .getBoolean("emulator_activity_pause", false)
    }

    override fun decreaseResumesToRestart(): Int {
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

    override fun resetProcessResetCounter() {
        val editor = PreferenceManager.getDefaultSharedPreferences(Utils.getApp()).edit()
        editor.putInt("PRC", maxPRC)
        editor.apply()
    }

    override fun hideTouchController() {
        NLog.i(EmulatorActivity.TAG, "hide controler")
        if (autoHide) {
            gameControlProxy.hideTouchController()
        }
    }

    override fun quickSave() {
        emulatorManagerProxy.saveState(10)
        Toast.makeText(
            Utils.getApp(),
            "state saved", Toast.LENGTH_SHORT
        ).show()
    }

    override fun quickLoad() {
        emulatorManagerProxy.loadState(10)
    }

    override fun handleException(e: EmulatorException) {
        val dialog = AlertDialog.Builder(activity)
            .setMessage(e.getMessage(activity))
            .create()
        dialog.setOnDismissListener { (activity as? Activity)?.finish() }
        DialogUtils.show(dialog, true)
    }
}