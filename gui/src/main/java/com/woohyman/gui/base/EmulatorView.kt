package com.woohyman.gui.base

import android.view.View
import com.woohyman.keyboard.base.ViewPort

internal interface EmulatorView {
    fun onPause()
    fun onResume()
    fun setQuality(quality: Int)
    val viewPort: ViewPort?
    fun asView(): View
}