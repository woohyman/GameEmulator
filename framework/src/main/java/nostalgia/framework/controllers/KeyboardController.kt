package nostalgia.framework.controllers

import android.content.Context
import android.util.SparseIntArray
import android.view.KeyEvent
import android.view.View
import nostalgia.framework.emulator.Emulator
import nostalgia.framework.keyboard.KeyboardProfile
import nostalgia.framework.base.EmulatorActivity
import nostalgia.framework.data.database.GameDescription
import nostalgia.framework.utils.NLog

class KeyboardController(
    private val emulator: Emulator,
    private var context: Context?,
    var gameHash: String,
    private var emulatorActivity: EmulatorActivity?
) : EmulatorController {
    private val tmpKeys = IntArray(2)
    private val loadingOrSaving = BooleanArray(4)
    private var profile: KeyboardProfile? = null
    private val keyMapping: SparseIntArray?

    init {
        keyMapping = emulator.info?.keyMapping
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
    override fun onGameStarted(game: GameDescription) {}
    override fun onGamePaused(game: GameDescription) {}
    override fun connectToEmulator(port: Int, emulator: Emulator) {
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
                    emulatorActivity!!.hideTouchController()
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
                emulatorActivity!!.finish()
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
                emulatorActivity!!.onFastForwardDown()
            } else {
                emulatorActivity!!.onFastForwardUp()
            }
        } else if (mapValue == KEY_MENU) {
            if (pressed) {
                emulatorActivity!!.openGameMenu()
            }
        } else if (isMulti(mapValue)) {
            multiToKeys(mapValue, tmpKeys)
            emulator.setKeyPressed(port, keyMapping!![tmpKeys[0]], pressed)
            emulator.setKeyPressed(port, keyMapping[tmpKeys[1]], pressed)
        } else {
            NLog.i(TAG, "process key $mapValue $keyMapping")
            val value = keyMapping!![mapValue]
            emulator.setKeyPressed(port, value, pressed)
        }
    }

    private fun save(slot: Int, isKeyPressed: Boolean) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true
            emulatorActivity!!.manager.saveState(slot)
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false
        }
    }

    private fun load(slot: Int, isKeyPressed: Boolean) {
        if (isKeyPressed && !loadingOrSaving[slot]) {
            loadingOrSaving[slot] = true
            emulatorActivity!!.manager.loadState(slot)
        }
        if (!isKeyPressed) {
            loadingOrSaving[slot] = false
        }
    }

    override fun onDestroy() {
        context = null
        emulatorActivity = null
    }

    companion object {
        const val PLAYER2_OFFSET = 100000
        const val KEY_XPERIA_CIRCLE = 2068987562
        const val KEY_MENU = 902
        const val KEY_BACK = 900
        const val KEY_RESET = 901
        const val KEY_FAST_FORWARD = 903
        const val KEY_SAVE_SLOT_0 = 904
        const val KEY_LOAD_SLOT_0 = 905
        const val KEY_SAVE_SLOT_1 = 906
        const val KEY_LOAD_SLOT_1 = 907
        const val KEY_SAVE_SLOT_2 = 908
        const val KEY_LOAD_SLOT_2 = 909
        private const val TAG = "controller.KeyboardController"

        @JvmField
        var KEYS_RIGHT_AND_UP =
            keysToMultiCode(EmulatorController.KEY_RIGHT, EmulatorController.KEY_UP)

        @JvmField
        var KEYS_RIGHT_AND_DOWN =
            keysToMultiCode(EmulatorController.KEY_RIGHT, EmulatorController.KEY_DOWN)

        @JvmField
        var KEYS_LEFT_AND_DOWN =
            keysToMultiCode(EmulatorController.KEY_LEFT, EmulatorController.KEY_DOWN)

        @JvmField
        var KEYS_LEFT_AND_UP =
            keysToMultiCode(EmulatorController.KEY_LEFT, EmulatorController.KEY_UP)

        private fun keysToMultiCode(key1: Int, key2: Int): Int {
            return key1 * 1000 + key2 + 10000
        }

        private fun isMulti(mapValue: Int): Boolean {
            return mapValue >= 10000
        }
    }
}