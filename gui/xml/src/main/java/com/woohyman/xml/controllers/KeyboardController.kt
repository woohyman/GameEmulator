package com.woohyman.xml.controllers

import android.content.Context
import android.view.KeyEvent
import android.view.View
import com.woohyman.xml.emulator.EmulatorActivity
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.database.GameDescription
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
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.emulator.EmulatorMediator

class KeyboardController(
    private var context: Context?,
    var gameHash: String,
    private var emulatorMediator: EmulatorMediator
) : EmulatorController {
    private val tmpKeys = IntArray(2)
    private val loadingOrSaving = BooleanArray(4)
    private var profile: KeyboardProfile? = null
    private val keyMapping: Map<Int,Int>

    init {
        keyMapping = emulator.info.keyMapping
    }

    override fun onResume() {
        profile = KeyboardProfile.getSelectedProfile(gameHash, context!!)
        emulator.resetKeys()
        for (i in loadingOrSaving.indices) {
            loadingOrSaving[i] = false
        }
    }

    override fun onPause() {}
    override fun onWindowFocusChanged(hasFocus: Boolean) {}
    override fun onGameStarted() {}
    override fun onGamePaused() {}
    override fun connectToEmulator(port: Int) {
        throw UnsupportedOperationException()
    }

    private fun multiToKeys(mapValue: Int, keys: IntArray) {
        var mapValue = mapValue
        mapValue -= 10000
        val key1 = mapValue / 1000
        mapValue -= key1 * 1000
        val key2 = mapValue
        keys[0] = key1
        keys[1] = key2
    }

    override fun getView(): View {
        return object : View(context) {
            override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
                var keyCode = keyCode
                var mapValue: Int = 0
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed) {
                        keyCode = KEY_XPERIA_CIRCLE
                    }
                }
                return if (profile != null && profile!!.keyMap[keyCode, -1].also {
                        mapValue = it
                    } != -1) {
                    processKey(mapValue, true)
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }

            override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
                var keyCode = keyCode
                var mapValue: Int = 0
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed) {
                        keyCode = KEY_XPERIA_CIRCLE
                    }
                }
                return if (profile != null && profile!!.keyMap[keyCode, -1].also {
                        mapValue = it
                    } != -1) {
                    processKey(mapValue, false)
                    emulatorMediator.hideTouchController()
                    true
                } else {
                    super.onKeyUp(keyCode, event)
                }
            }
        }
    }

    fun processKey(mapValue: Int, pressed: Boolean) {
        var mapValue = mapValue
        NLog.i(TAG, "process key $mapValue")
        var port = 0
        if (mapValue >= PLAYER2_OFFSET) {
            mapValue -= PLAYER2_OFFSET
            port = 1
        }
        if (mapValue == KEY_BACK) {
            if (pressed) {
                emulatorMediator.activity?.finish()
            }
        } else if (mapValue == KEY_SAVE_SLOT_0) {
            save(1, pressed)
        } else if (mapValue == KEY_SAVE_SLOT_1) {
            save(2, pressed)
        } else if (mapValue == KEY_SAVE_SLOT_2) {
            save(3, pressed)
        } else if (mapValue == KEY_LOAD_SLOT_0) {
            load(1, pressed)
        } else if (mapValue == KEY_LOAD_SLOT_1) {
            load(2, pressed)
        } else if (mapValue == KEY_LOAD_SLOT_2) {
            load(3, pressed)
        } else if (mapValue == KEY_FAST_FORWARD) {
            if (pressed) {
                emulatorMediator.emulatorManagerProxy.onFastForwardDown()
            } else {
                emulatorMediator.emulatorManagerProxy.onFastForwardUp()
            }
        } else if (mapValue == KEY_MENU) {
            if (pressed) {
                emulatorMediator.gameMenuProxy.openGameMenu()
            }
        } else if (KeyboardControllerKeys.isMulti(mapValue)) {
            multiToKeys(mapValue, tmpKeys)
            emulator.setKeyPressed(port, keyMapping[tmpKeys[0]]!!, pressed)
            emulator.setKeyPressed(port, keyMapping[tmpKeys[1]]!!, pressed)
        } else {
            NLog.i(TAG, "process key $mapValue $keyMapping")
            val value = keyMapping[mapValue]!!
            emulator.setKeyPressed(port, value, pressed)
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

    override fun onDestroy() {
        context = null
    }

    companion object{
        private const val TAG = "controller.KeyboardController"
    }
}