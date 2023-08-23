package com.woohyman.gui.ui.multitouchbutton

import android.view.MotionEvent

interface MultitouchBtnInterface {
    fun onTouchEnter(event: MotionEvent?)
    fun onTouchExit(event: MotionEvent?)
    fun setOnMultitouchEventlistener(listener: OnMultitouchEventListener?)
    fun getId(): Int
    fun isPressed(): Boolean
    fun requestRepaint()
    fun removeRequestRepaint()
    val isRepaintState: Boolean
    fun getVisibility(): Int
}