package com.woohyman.xml.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceView
import android.view.View
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.base.ViewUtils.loadOrComputeViewPort
import com.woohyman.keyboard.emulator.EmulatorView
import com.woohyman.keyboard.utils.EmuUtils.emulator

internal class UnacceleratedView(
    context: Context,
    paddingLeft: Int,
    paddingTop: Int
) : SurfaceView(context), EmulatorView {
    private var startTime: Long = 0
    private var x = 0
    private var y = 0
    private val paddingTop: Int
    private val paddingLeft: Int
    override var viewPort: ViewPort? = null
        private set

    init {
        setWillNotDraw(false)
        this.paddingTop = paddingTop
        this.paddingLeft = paddingLeft
    }

    override fun onPause() {}
    override fun onResume() {}
    override fun setQuality(quality: Int) {}
    override fun asView(): View {
        return this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val vp = loadOrComputeViewPort(
            context, emulator,
            w, h, paddingLeft, paddingTop, false
        )
        x = vp!!.x
        y = vp.y
        emulator.setViewPortSize(vp.width, vp.height)
        startTime = System.currentTimeMillis()
        viewPort = vp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val endTime = System.currentTimeMillis()
        val delay = DELAY_PER_FRAME - (endTime - startTime)
        if (delay > 0) {
            try {
                Thread.sleep(delay)
            } catch (ignored: InterruptedException) {
            }
        }
        startTime = System.currentTimeMillis()
        emulator.renderGfx()
        emulator.draw(canvas, x, y)
        invalidate()
    }

    companion object {
        private const val DELAY_PER_FRAME = 40
    }
}