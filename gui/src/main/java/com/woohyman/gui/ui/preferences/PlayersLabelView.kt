package com.woohyman.gui.ui.preferences

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class PlayersLabelView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {
    var paint = Paint()
    var textSize = 0f
    var offsets = intArrayOf(0, 300, 800)
        set(value) {
            field = value
            invalidate()
        }
    var offset = 0
        set(value) {
            field = value
            invalidate()
        }

    init {
        init()
    }

    private fun init() {
        paint.color = -0x1
        val r = resources
        val textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            25f, r.displayMetrics
        )
        paint.textSize = textSize
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(0f, 40f)
        canvas.rotate(-90f, 0f, 0f)
        for (i in offsets.indices) {
            val label = "PLAYER " + (i + 1)
            val width = paint.measureText(label)
            var off = (offset - width - offsets[i] + 40).toInt()
            var active = false
            active = if (i < offsets.size - 1) {
                offsets[i] <= offset && offset < offsets[i + 1]
            } else {
                offsets[i] <= offset && offset < offsets[i] + 20000
            }
            if (active && offset > 40 - width) off = (40 - width).toInt()
            paint.color = -0x1000000
            paint.style = Paint.Style.FILL
            canvas.drawRect((off - 2).toFloat(), 0f, off + width, measuredWidth.toFloat(), paint)
            paint.color = -0x1
            canvas.drawText(label, off.toFloat(), 40f, paint)
        }
        canvas.restore()
    }

    companion object {
        private const val TAG = "PlayersLabelView"
    }
}