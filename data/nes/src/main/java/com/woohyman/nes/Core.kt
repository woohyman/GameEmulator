package com.woohyman.nes

import com.woohyman.keyboard.base.JniBridge
import javax.inject.Inject

class Core @Inject constructor() : JniBridge() {
    init {
        System.loadLibrary("nes")
    }
}