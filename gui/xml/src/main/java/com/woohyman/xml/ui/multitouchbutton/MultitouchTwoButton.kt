package com.woohyman.xml.ui.multitouchbutton

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.woohyman.xml.R

class MultitouchTwoButton : MultitouchImageButton {
    var firstBtnRID = -1
        protected set
    var secondBtnRID = -1
        protected set
    private val holder = ViewHolder()

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
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
        super.onTouchEnter(event)
        if (holder.firstButton == null) {
            initHolder()
        }
        holder.firstButton!!.onTouchEnter(event)
        holder.secondButton!!.onTouchEnter(event)
    }

    override fun onTouchExit(event: MotionEvent?) {
        super.onTouchExit(event)
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