package com.woohyman.xml.emulator.controllers

import android.view.KeyEvent
import android.view.View
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_BACK
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_FAST_FORWARD
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_LOAD_SLOT_0
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_LOAD_SLOT_1
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_LOAD_SLOT_2
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_MENU
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_SAVE_SLOT_0
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_SAVE_SLOT_1
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_SAVE_SLOT_2
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.KEY_XPERIA_CIRCLE
import com.woohyman.keyboard.keyboard.KeyboardControllerKeys.Companion.PLAYER2_OFFSET
import com.woohyman.keyboard.keyboard.KeyboardProfile
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.emulator.IEmulatorMediator
import javax.inject.Inject

class KeyboardController @Inject constructor(
    private var emulatorMediator: IEmulatorMediator,
) : EmulatorController {

    private val activity by lazy {
        ActivityUtils.getTopActivity()!!
    }

    private val tmpKeys = IntArray(2)
    private val loadingOrSaving = BooleanArray(4)
    private var profile: KeyboardProfile? = null

    override fun onResume() {
        profile =
            KeyboardProfile.getSelectedProfile(EmuUtils.fetchProxy.game.checksum, Utils.getApp())
        emulator.resetKeys()
        for (i in loadingOrSaving.indices) {
            loadingOrSaving[i] = false
        }
    }

    override fun connectToEmulator(port: Int) {
        throw UnsupportedOperationException()
    }

    private fun multiToKeys(mapValue: Int, keys: IntArray) {
        val key1 = (mapValue - 10000) / 1000
        val key2 = (mapValue - 10000) - (key1 * 1000)
        keys[0] = key1
        keys[1] = key2
    }

    override val view: View = object : View(Utils.getApp()) {
        override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
            var mapValue = 0
            keyCode.let {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed) {
                        return@let KEY_XPERIA_CIRCLE
                    }
                }
                keyCode
            }.let {
                val value = profile?.keyMap?.get(it, -1)?.also { value ->
                    mapValue = value
                }
                return if (value != -1 && value != null) {
                    processKey(mapValue, true)
                    true
                } else {
                    super.onKeyDown(it, event)
                }
            }
        }

        override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
            var mapValue = 0
            keyCode.let {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed) {
                        return@let KEY_XPERIA_CIRCLE
                    }
                }
                keyCode
            }.let {
                val value = profile?.keyMap?.get(it, -1)?.also { value ->
                    mapValue = value
                }
                return if (value != null && value != -1) {
                    processKey(mapValue, false)
                    emulatorMediator.hideTouchController()
                    true
                } else {
                    super.onKeyUp(it, event)
                }
            }
        }
    }

    fun processKey(mapValue: Int, pressed: Boolean) {
        NLog.i(TAG, "process key $mapValue")
        var port = 0
        mapValue.let {
            if (it >= PLAYER2_OFFSET) {
                port = 1
                return@let it - PLAYER2_OFFSET
            }
            it
        }.let {
            when {
                it == KEY_BACK -> {
                    if (pressed) {
                        activity.finish()
                    }
                }

                it == KEY_SAVE_SLOT_0 -> {
                    save(1, pressed)
                }

                it == KEY_SAVE_SLOT_1 -> {
                    save(2, pressed)
                }

                it == KEY_SAVE_SLOT_2 -> {
                    save(3, pressed)
                }

                it == KEY_LOAD_SLOT_0 -> {
                    load(1, pressed)
                }

                it == KEY_LOAD_SLOT_1 -> {
                    load(2, pressed)
                }

                it == KEY_LOAD_SLOT_2 -> {
                    load(3, pressed)
                }

                it == KEY_FAST_FORWARD -> {
                    if (pressed) {
                        emulatorMediator.emulatorManagerProxy.onFastForwardDown()
                    } else {
                        emulatorMediator.emulatorManagerProxy.onFastForwardUp()
                    }
                }

                it == KEY_MENU -> {
                    if (pressed) {
                        emulatorMediator.gameMenuProxy.openGameMenu()
                    }
                }

                KeyboardControllerKeys.isMulti(it) -> {
                    multiToKeys(it, tmpKeys)
                    emulator.setKeyPressed(port, emulator.info.getMappingValue(tmpKeys[0]), pressed)
                    emulator.setKeyPressed(port, emulator.info.getMappingValue(tmpKeys[1]), pressed)
                }

                else -> {
                    val value = emulator.info.getMappingValue(it)
                    emulator.setKeyPressed(port, value, pressed)
                }
            }
        }
    }

    private fun save(slot: Int, isKeyPressed: Boolean) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true
            emulatorMediator.emulatorManagerProxy.saveState(slot)
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false
        }
    }

    private fun load(slot: Int, isKeyPressed: Boolean) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true
            emulatorMediator.emulatorManagerProxy.loadState(slot)
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false
        }
    }

    companion object {
        private const val TAG = "controller.KeyboardController"
    }
}