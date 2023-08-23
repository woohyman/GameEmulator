package com.woohyman.gui.ui.multitouchbutton

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.woohyman.gui.R

class MultitouchTwoButtonArea : MultitouchImageButton {
    var firstBtnRID = -1
        protected set
    var secondBtnRID = -1
        protected set
    private val holder = ViewHolder()

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (!isInEditMode) {
            visibility = INVISIBLE
        }
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.MultitouchTwoButtonArea, 0, 0
        )
        try {
            firstBtnRID = a.getResourceId(
                R.styleable.MultitouchTwoButtonArea_first_button, -1
            )
            secondBtnRID = a.getResourceId(
                R.styleable.MultitouchTwoButtonArea_second_button, -1
            )
        } finally {
            a.recycle()
        }
    }

    override fun onTouchEnter(event: MotionEvent?) {
        if (holder.firstButton == null) {
            initHolder()
        }
        holder.firstButton!!.onTouchEnter(event)
        holder.secondButton!!.onTouchEnter(event)
    }

    override fun onTouchExit(event: MotionEvent?) {
        if (holder.firstButton == null) {
            initHolder()
        }
        holder.firstButton!!.onTouchExit(event)
        holder.secondButton!!.onTouchExit(event)
    }

    private fun initHolder() {
        holder.firstButton = rootView.findViewById(firstBtnRID)
        holder.secondButton = rootView.findViewById(secondBtnRID)
    }

    override fun requestRepaint() {
        super.requestRepaint()
        holder.firstButton!!.requestRepaint()
        holder.secondButton!!.requestRepaint()
    }

    private class ViewHolder {
        var firstButton: MultitouchBtnInterface? = null
        var secondButton: MultitouchBtnInterface? = null
    }
}