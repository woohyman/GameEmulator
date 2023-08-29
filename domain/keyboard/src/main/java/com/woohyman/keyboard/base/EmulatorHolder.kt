package com.woohyman.keyboard.base

import com.woohyman.keyboard.data.entity.EmulatorInfo

object EmulatorHolder {
    private lateinit var _emulator: JniEmulator

    @JvmStatic
    val info: EmulatorInfo
        get() = _emulator.info

    @JvmStatic
    fun setEmulatorClass(emulator: JniEmulator) {
        _emulator = emulator
    }
}