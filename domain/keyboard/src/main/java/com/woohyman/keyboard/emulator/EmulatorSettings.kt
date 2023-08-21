package com.woohyman.keyboard.emulator

class EmulatorSettings {
    var zapperEnabled = false
    var historyEnabled = false
    var loadSavFiles = false
    var saveSavFiles = false
    var quality = 0
    fun toInt(): Int {
        var x = if (zapperEnabled) 1 else 0
        x += if (historyEnabled) 10 else 0
        x += if (loadSavFiles) 100 else 0
        x += if (saveSavFiles) 1000 else 0
        x += quality * 10000
        return x
    }
}