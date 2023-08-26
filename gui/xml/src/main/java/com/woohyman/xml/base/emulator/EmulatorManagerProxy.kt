package com.woohyman.xml.base.emulator

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R

class EmulatorManagerProxy(
    private val activity: EmulatorActivity,
    private val emulatorInstance: Emulator,
    private val game: GameDescription,
) : DefaultLifecycleObserver, Manager(emulatorInstance, activity) {

    private var isFF = false
    private var isToggleFF = false
    private var isFFPressed = false

    override fun saveState(slot: Int) {
        Toast.makeText(
            activity,
            "state saved", Toast.LENGTH_SHORT
        ).show()
    }


    val needsBenchmark by lazy {
        val quality = PreferenceUtil.getEmulationQuality(activity)
        val alreadyBenchmarked = PreferenceUtil.isBenchmarked(activity)
        quality != 2 && !alreadyBenchmarked
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        setOnNotRespondingListener(activity)
        if (needsBenchmark) {
            setBenchmark(
                Benchmark(
                    EmulatorActivity.EMULATION_BENCHMARK,
                    1000,
                    activity.emulatorMediator.emulatorView.benchmarkCallback
                )
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        try {
            destroy()
        } catch (ignored: EmulatorException) {

        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        try {
            pauseEmulation()
        } catch (ignored: EmulatorException) {
        }
        try {
            stopGame()
        } catch (e: EmulatorException) {
            activity.handleException(e)
        } finally {
            activity.emulatorMediator.emulatorView.onPause()
        }

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        setFastForwardFrameCount(PreferenceUtil.getFastForwardFrameCount(activity))

        isToggleFF = PreferenceUtil.isFastForwardToggleable(activity)
        isFF = false
        isFFPressed = false

        try {
            startGame(game)

            if (activity.slotToRun != -1) {
                loadState(activity.slotToRun)
            } else {
                if (SlotUtils.autoSaveExists(activity.emulatorMediator.baseDir, game.checksum)) {
                    loadState(0)
                }
            }
            if (activity.slotToSave != null) {
                copyAutoSave(activity.slotToSave)
            }
            val wasRotated =
                EmulatorActivity.oldConfig and ActivityInfo.CONFIG_ORIENTATION == ActivityInfo.CONFIG_ORIENTATION
            EmulatorActivity.oldConfig = 0
            if (activity.emulatorMediator.shouldPause() && !wasRotated) {
                activity.emulatorMediator.gameMenuProxy.gameMenu.open()
            }
            if (activity.emulatorMediator.gameMenuProxy.gameMenu.isOpen) {
                pauseEmulation()
            }
            activity.emulatorMediator.setShouldPauseOnResume(true)
        } catch (e: EmulatorException) {
            activity.handleException(e)
        }
    }

    fun onFastForwardDown() {
        if (isToggleFF) {
            if (!isFFPressed) {
                isFFPressed = true
                isFF = !isFF
                setFastForwardEnabled(isFF)
            }
        } else {
            setFastForwardEnabled(true)
        }
    }

    fun onFastForwardUp() {
        if (!isToggleFF) {
            setFastForwardEnabled(false)
        }
        isFFPressed = false
    }

    fun enableCheats() {
        var numCheats = 0
        try {
            numCheats = enableCheats(activity, game)
        } catch (e: EmulatorException) {
            Toast.makeText(
                activity, e.getMessage(activity),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (numCheats > 0) {
            Toast.makeText(
                activity, activity.getString(R.string.toast_cheats_enabled, numCheats),
                Toast.LENGTH_LONG
            ).show()
        }
    }

}