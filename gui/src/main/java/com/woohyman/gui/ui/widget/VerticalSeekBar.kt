/**
 * Based on http://hackskrieg.wordpress.com/2012/04/20/working-vertical-seekbar-for-android/
 */
package com.woohyman.gui.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatSeekBar

class VerticalSeekBar : AppCompatSeekBar {
    private var onChangeListener: OnSeekBarChangeListener? = null
    private var lastProgress = 0

    constructor(context: Context?) : super(context!!)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(h, w, oldh, oldw)
    }

    @Synchronized
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec)
        setMeasuredDimension(measuredHeight, measuredWidth)
    }

    override fun onDraw(c: Canvas) {
        c.rotate(-90f)
        c.translate(-height.toFloat(), 0f)
        super.onDraw(c)
    }

    override fun setOnSeekBarChangeListener(
        onChangeListener: OnSeekBarChangeListener
    ) {
        this.onChangeListener = onChangeListener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                onChangeListener!!.onStartTrackingTouch(this)
                isPressed = true
                isSelected = true
            }

            MotionEvent.ACTION_MOVE -> {
                super.onTouchEvent(event)
                var progress = (max
                        - (max * event.y / height).toInt())

                // Ensure progress stays within boundaries
                if (progress < 0) {
                    progress = 0
                }
                if (progress > max) {
                    progress = max
                }
                setProgress(progress) // Draw progress
                if (progress != lastProgress) {
                    // Only enact listener if the progress has actually changed
                    lastProgress = progress
                    onChangeListener!!.onProgressChanged(this, progress, true)
                }
                onSizeChanged(width, height, 0, 0)
                isPressed = true
                isSelected = true
            }

            MotionEvent.ACTION_UP -> {
                onChangeListener!!.onStopTrackingTouch(this)
                isPressed = false
                isSelected = false
            }

            MotionEvent.ACTION_CANCEL -> {
                super.onTouchEvent(event)
                isPressed = false
                isSelected = false
            }
        }
        return true
    }

    @Synchronized
    fun setProgressAndThumb(progress: Int) {
        setProgress(progress)
        onSizeChanged(width, height, 0, 0)
        if (progress != lastProgress) {
            // Only enact listener if the progress has actually changed
            lastProgress = progress
            onChangeListener!!.onProgressChanged(this, progress, true)
        }
    }

    @get:Synchronized
    @set:Synchronized
    var maximum: Int
        get() = max
        set(maximum) {
            max = maximum
        }
}