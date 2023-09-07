package com.woohyman.keyboard.controllers

import android.view.View

/*管理游戏控制器*/
interface EmulatorController {
    val view: View
    fun onResume(){}
    fun onPause(){}
    fun onWindowFocusChanged(hasFocus: Boolean){}
    fun onGameStarted(){}
    fun onGamePaused(){}
    fun connectToEmulator(port: Int){}
    fun onDestroy(){}
}

enum class KeyAction(val key: Int) {
    KEY_A(0),
    KEY_B(1),
    KEY_A_TURBO(255),
    KEY_B_TURBO(256),
    KEY_X(2),
    KEY_Y(3),
    KEY_START(4),
    KEY_SELECT(5),
    KEY_UP(6),
    KEY_DOWN(7),
    KEY_LEFT(8),
    KEY_RIGHT(9),
    ACTION_DOWN(0),
    ACTION_UP(1),
}