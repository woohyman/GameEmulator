package com.woohyman.gui.ui.multitouchbutton

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton

class MultitouchButton : AppCompatButton, MultitouchBtnInterface {
    override var isRepaintState = true

    var listener: OnMultitouchEventListener? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    override fun onTouchEnter(event: MotionEvent?) {
        isPressed = true
        if (listener != null) listener!!.onMultitouchEnter(this)
    }

    override fun onTouchExit(event: MotionEvent?) {
        isPressed = false
        if (listener != null) listener!!.onMultitouchExit(this)
    }

    override fun setOnMultitouchEventlistener(listener: OnMultitouchEventListener?) {
        this.listener = listener
    }

    override fun requestRepaint() {
        isRepaintState = true
    }

    override fun removeRequestRepaint() {
        isRepaintState = false
    }

    override fun invalidate() {
        super.invalidate()
    }
}