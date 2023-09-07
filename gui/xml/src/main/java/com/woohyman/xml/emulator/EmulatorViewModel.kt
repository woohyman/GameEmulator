package com.woohyman.xml.emulator

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.woohyman.xml.emulator.business.EmulatorManagerProxy
import com.woohyman.xml.emulator.business.EmulatorViewProxy
import com.woohyman.xml.emulator.business.GameControlProxy
import com.woohyman.xml.emulator.business.GameMenuDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EmulatorViewModel @Inject constructor(
    val emulatorMediator: IEmulatorMediator,
    val gameMenuProxy: GameMenuDelegate,
    val emulatorManagerProxy: EmulatorManagerProxy,
    val gameControlProxy: GameControlProxy,
    val emulatorView: EmulatorViewProxy,
) : ViewModel() {

    fun addObserver(appCompatActivity: AppCompatActivity) {
        appCompatActivity.lifecycle.addObserver(emulatorMediator)
        appCompatActivity.lifecycle.addObserver(gameMenuProxy)
        appCompatActivity.lifecycle.addObserver(emulatorManagerProxy)
        appCompatActivity.lifecycle.addObserver(gameControlProxy)
    }

}