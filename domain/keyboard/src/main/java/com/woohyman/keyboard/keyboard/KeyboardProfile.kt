package com.woohyman.keyboard.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.SparseIntArray
import android.view.KeyEvent
import com.woohyman.keyboard.controllers.KeyAction
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.NLog
import java.io.Serializable

class KeyboardProfile : Serializable {
    var name: String? = null
    var keyMap = SparseIntArray()
    fun delete(context: Context): Boolean {
        NLog.i(TAG, "delete profile $name")
        var pref = context.getSharedPreferences("$name.keyprof", Context.MODE_PRIVATE)
        var editor = pref.edit()
        editor.clear()
        editor.apply()
        pref = context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE)
        editor = pref.edit()
        editor.remove(name)
        editor.apply()
        return true
    }

    fun save(context: Context): Boolean {
        var pref =
            context.getSharedPreferences(name + KEYBOARD_PROFILE_POSTFIX, Context.MODE_PRIVATE)
        NLog.i(TAG, "save profile $name $keyMap")
        var editor = pref.edit()
        editor.clear()
        for (i in BUTTON_NAMES!!.indices) {
            val value = BUTTON_KEY_EVENT_CODES!![i]
            val idx = keyMap.indexOfValue(value)
            val key = if (idx == -1) 0 else keyMap.keyAt(idx)
            if (key != 0) {
                NLog.i(TAG, "save " + BUTTON_NAMES!![i] + " " + key + "->" + value)
                editor.putInt(key.toString() + "", value)
            }
        }
        editor.apply()
        if (name != "default") {
            pref = context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE)
            editor = pref.edit()
            editor.putBoolean(name, true)
            editor.remove("default")
            editor.apply()
        }
        return true
    }

    companion object {
        val DEFAULT_PROFILES_NAMES = arrayOf("default", "ps3", "wiimote")
        private const val serialVersionUID = 5817859819275903370L
        private const val KEYBOARD_PROFILES_SETTINGS = "keyboard_profiles_pref"
        private const val KEYBOARD_PROFILE_POSTFIX = "_keyboard_profile"
        private const val TAG = "KeyboardProfile"
        val BUTTON_NAMES: Array<String> get() = EmuUtils.emulator.info.deviceKeyboardNames
        val BUTTON_DESCRIPTIONS: Array<String> get() = EmuUtils.emulator.info.deviceKeyboardDescriptions
        val BUTTON_KEY_EVENT_CODES: IntArray get() = EmuUtils.emulator.info.deviceKeyboardCodes

        @JvmStatic
        fun createDefaultProfile(): KeyboardProfile {
            val profile = KeyboardProfile()
            profile.name = "default"
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, KeyAction.KEY_LEFT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, KeyAction.KEY_RIGHT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, KeyAction.KEY_UP.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, KeyAction.KEY_DOWN.key)
            profile.keyMap.put(KeyEvent.KEYCODE_ENTER, KeyAction.KEY_START.key)
            profile.keyMap.put(KeyEvent.KEYCODE_SPACE, KeyAction.KEY_SELECT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_Q, KeyAction.KEY_A.key)
            profile.keyMap.put(KeyEvent.KEYCODE_W, KeyAction.KEY_B.key)
            profile.keyMap.put(KeyEvent.KEYCODE_A, KeyAction.KEY_A_TURBO.key)
            profile.keyMap.put(KeyEvent.KEYCODE_S, KeyAction.KEY_B_TURBO.key)
            return profile
        }

        @SuppressLint("InlinedApi")
        fun createPS3Profile(): KeyboardProfile {
            val profile = KeyboardProfile()
            profile.name = "ps3"
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, KeyAction.KEY_LEFT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, KeyAction.KEY_RIGHT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, KeyAction.KEY_UP.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, KeyAction.KEY_DOWN.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_START, KeyAction.KEY_START.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_SELECT, KeyAction.KEY_SELECT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_B, KeyAction.KEY_A.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_Y, KeyAction.KEY_B.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_A, KeyAction.KEY_A_TURBO.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_X, KeyAction.KEY_B_TURBO.key)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_R2, KeyboardControllerKeys.KEY_MENU)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_L2, KeyboardControllerKeys.KEY_BACK)
            profile.keyMap.put(KeyEvent.KEYCODE_BUTTON_L1, KeyboardControllerKeys.KEY_FAST_FORWARD)
            return profile
        }

        fun createWiimoteProfile(): KeyboardProfile {
            val profile = KeyboardProfile()
            profile.name = "wiimote"
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_LEFT, KeyAction.KEY_LEFT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, KeyAction.KEY_RIGHT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_UP, KeyAction.KEY_UP.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_DOWN, KeyAction.KEY_DOWN.key)
            profile.keyMap.put(KeyEvent.KEYCODE_P, KeyAction.KEY_START.key)
            profile.keyMap.put(KeyEvent.KEYCODE_M, KeyAction.KEY_SELECT.key)
            profile.keyMap.put(KeyEvent.KEYCODE_1, KeyAction.KEY_B.key)
            profile.keyMap.put(KeyEvent.KEYCODE_2, KeyAction.KEY_A.key)
            profile.keyMap.put(KeyEvent.KEYCODE_DPAD_CENTER, KeyboardControllerKeys.KEY_MENU)
            profile.keyMap.put(KeyEvent.KEYCODE_H, KeyboardControllerKeys.KEY_BACK)
            profile.keyMap.put(
                KeyEvent.KEYCODE_O,
                KeyAction.KEY_LEFT.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_J,
                KeyAction.KEY_RIGHT.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_I,
                KeyAction.KEY_UP.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_K,
                KeyAction.KEY_DOWN.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_PLUS,
                KeyAction.KEY_START.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_MINUS,
                KeyAction.KEY_SELECT.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_COMMA,
                KeyAction.KEY_B.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            profile.keyMap.put(
                KeyEvent.KEYCODE_PERIOD,
                KeyAction.KEY_A.key + KeyboardControllerKeys.PLAYER2_OFFSET
            )
            return profile
        }

        fun getSelectedProfile(gameHash: String?, context: Context): KeyboardProfile {
            val pref = PreferenceManager.getDefaultSharedPreferences(context)
            val name = pref.getString("pref_game_keyboard_profile", "default")
            return load(context, name)
        }

        fun load(context: Context, name: String?): KeyboardProfile {
            return if (name != null) {
                val pref = context.getSharedPreferences(
                    name + KEYBOARD_PROFILE_POSTFIX,
                    Context.MODE_PRIVATE
                )
                if (pref.all.size != 0) {
                    val profile = KeyboardProfile()
                    profile.name = name
                    for ((key, value1) in pref.all) {
                        val value = value1 as Int
                        val nkey = key.toInt()
                        profile.keyMap.put(nkey, value)
                    }
                    profile
                } else {
                    NLog.i(TAG, "empty " + name + KEYBOARD_PROFILE_POSTFIX)
                    when (name) {
                        "ps3" -> createPS3Profile()
                        "wiimote" -> createWiimoteProfile()
                        else -> createDefaultProfile()
                    }
                }
            } else {
                createDefaultProfile()
            }
        }

        @JvmStatic
        fun getProfilesNames(context: Context): ArrayList<String> {
            val pref =
                context.getSharedPreferences(KEYBOARD_PROFILES_SETTINGS, Context.MODE_PRIVATE)
            val prefNames: Set<String> = pref.all.keys
            val names = ArrayList<String>()
            for (defName in DEFAULT_PROFILES_NAMES) {
                if (!prefNames.contains(defName)) names.add(defName)
            }
            names.addAll(prefNames)
            return names
        }

        fun isDefaultProfile(name: String): Boolean {
            var defProf = false
            for (defName in DEFAULT_PROFILES_NAMES) {
                if (defName == name) {
                    defProf = true
                }
            }
            return defProf
        }

        fun restoreDefaultProfile(name: String, context: Context) {
            var prof: KeyboardProfile? = null
            when (name) {
                "ps3" -> prof = createPS3Profile()
                "default" -> prof = createDefaultProfile()
                "wiimote" -> prof = createWiimoteProfile()
            }
            prof?.save(context)
                ?: NLog.e(TAG, "Keyboard profile $name is unknown!!")
        }
    }
}