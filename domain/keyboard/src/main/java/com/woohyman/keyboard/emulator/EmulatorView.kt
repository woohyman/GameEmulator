package com.woohyman.keyboard.emulator

import android.view.View
import com.woohyman.keyboard.base.ViewPort

interface EmulatorView {
    fun onPause()
    fun onResume()
    fun setQuality(quality: Int)
    val viewPort: ViewPort?
    fun asView(): View
}