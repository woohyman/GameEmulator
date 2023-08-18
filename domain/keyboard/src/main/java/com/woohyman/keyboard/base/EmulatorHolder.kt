package com.woohyman.keyboard.base

import nostalgia.framework.data.entity.EmulatorInfo

object EmulatorHolder {
    private var emulatorClass: Class<out JniEmulator>? = null
    @JvmStatic
    var info: EmulatorInfo? = null
        get() {
            if (field == null) {
                field = try {
                    val getInstance = emulatorClass!!.getMethod("getInstance")
                    val emulator = getInstance.invoke(null) as JniEmulator
                    emulator.info
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
            return field
        }
        private set

    @JvmStatic
    fun setEmulatorClass(emulatorClass: Class<out JniEmulator>?) {
        EmulatorHolder.emulatorClass = emulatorClass
    }
}