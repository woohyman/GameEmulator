package com.woohyman.xml.base.emulator

import android.content.pm.ActivityInfo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.PreferenceUtil

class GameManagerProxy(
    private val activity: EmulatorActivity,
    private val emulatorInstance: Emulator,
    private val game: GameDescription,
) : DefaultLifecycleObserver, Manager(emulatorInstance, activity) {

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
                    activity.benchmarkCallback
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
            activity.emulatorView?.onPause()
        }

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        setFastForwardFrameCount(PreferenceUtil.getFastForwardFrameCount(activity))

        try {
            startGame(game)

            if (activity.slotToRun != -1) {
                loadState(activity.slotToRun)
            } else {
                if (SlotUtils.autoSaveExists(activity.baseDir, game.checksum)) {
                    loadState(0)
                }
            }
            if (activity.slotToSave != null) {
                copyAutoSave(activity.slotToSave)
            }
            val wasRotated =
                EmulatorActivity.oldConfig and ActivityInfo.CONFIG_ORIENTATION == ActivityInfo.CONFIG_ORIENTATION
            EmulatorActivity.oldConfig = 0
            if (activity.shouldPause() && !wasRotated) {
                activity.gameMenu.open()
            }
            if (activity.gameMenu.isOpen) {
                pauseEmulation()
            }
        } catch (e: EmulatorException) {
            activity.handleException(e)
        }
    }

}