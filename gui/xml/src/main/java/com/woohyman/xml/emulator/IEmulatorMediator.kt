package com.woohyman.xml.emulator

import androidx.lifecycle.DefaultLifecycleObserver
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.emulator.EmulatorRunner
import com.woohyman.xml.emulator.business.EmulatorManagerProxy
import com.woohyman.xml.emulator.business.EmulatorViewProxy
import com.woohyman.xml.emulator.business.GameControlProxy
import com.woohyman.xml.emulator.business.GameMenuDelegate
import com.woohyman.xml.ui.timetravel.TimeTravelDialog

interface IEmulatorMediator : DefaultLifecycleObserver, EmulatorRunner.OnNotRespondingListener {
    var gameMenuProxy: GameMenuDelegate
    var emulatorManagerProxy: EmulatorManagerProxy
    var gameControlProxy: GameControlProxy
    var emulatorView: EmulatorViewProxy
    fun handleException(e: EmulatorException)
    var isRestarting: Boolean
    var canRestart: Boolean
    var slotToRun: Int?
    var slotToSave: Int?
    var baseDir: String?

    fun setShouldPauseOnResume(b: Boolean)
    val dialog: TimeTravelDialog

    fun quickSave()
    fun quickLoad()
    fun shouldPause(): Boolean
    fun hideTouchController()

    fun decreaseResumesToRestart(): Int
    fun resetProcessResetCounter()

}