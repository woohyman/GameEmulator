package com.woohyman.xml.emulator.business

import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import com.woohyman.xml.emulator.EmulatorActivity
import com.woohyman.xml.emulator.IEmulatorMediator
import javax.inject.Inject

class EmulatorManagerProxy @Inject constructor(
    private val emulatorMediator: IEmulatorMediator,
) : DefaultLifecycleObserver, Manager(Utils.getApp()) {

    init {
        emulatorMediator.emulatorManagerProxy = this
    }

    private var isFF = false
    private var isToggleFF = false
    private var isFFPressed = false

    val needsBenchmark by lazy {
        val quality = PreferenceUtil.getEmulationQuality(Utils.getApp())
        val alreadyBenchmarked = PreferenceUtil.isBenchmarked(Utils.getApp())
        quality != 2 && !alreadyBenchmarked
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        setOnNotRespondingListener(emulatorMediator)
        if (needsBenchmark) {
            setBenchmark(
                Benchmark(
                    EmulatorActivity.EMULATION_BENCHMARK,
                    1000,
                    emulatorMediator.emulatorView.benchmarkCallback
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
            stopGame()
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
        } finally {
            emulatorMediator.emulatorView.onPause()
        }

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        setFastForwardFrameCount(PreferenceUtil.getFastForwardFrameCount(Utils.getApp()))

        isToggleFF = PreferenceUtil.isFastForwardToggleable(Utils.getApp())
        isFF = false
        isFFPressed = false

        try {
            startGame()

            if (emulatorMediator.slotToRun != -1) {
                loadState(emulatorMediator.slotToRun)
            } else {
                if (SlotUtils.autoSaveExists(
                        emulatorMediator.baseDir,
                        EmuUtils.fetchProxy.game.checksum
                    )
                ) {
                    loadState(0)
                }
            }
            if (emulatorMediator.slotToSave != null) {
                copyAutoSave(emulatorMediator.slotToSave)
            }
            val wasRotated =
                EmulatorActivity.oldConfig and ActivityInfo.CONFIG_ORIENTATION == ActivityInfo.CONFIG_ORIENTATION
            EmulatorActivity.oldConfig = 0
            if (emulatorMediator.shouldPause() && !wasRotated) {
                emulatorMediator.gameMenuProxy.gameMenu.open()
            }
            if (emulatorMediator.gameMenuProxy.gameMenu.isOpen) {
                pauseEmulation()
            }
            emulatorMediator.setShouldPauseOnResume(true)
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
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
            numCheats = enableCheats(Utils.getApp())
        } catch (e: EmulatorException) {
            Toast.makeText(
                Utils.getApp(), e.getMessage(Utils.getApp()),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (numCheats > 0) {
            Toast.makeText(
                Utils.getApp(), Utils.getApp().getString(R.string.toast_cheats_enabled, numCheats),
                Toast.LENGTH_LONG
            ).show()
        }
    }

}