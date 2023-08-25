package com.woohyman.xml.ui.widget

import android.app.Activity
import android.app.Application
import android.graphics.Canvas
import android.view.SurfaceView
import android.view.View
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.base.ViewUtils.loadOrComputeViewPort
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorView

internal class UnacceleratedView(
    context: Activity,
    private val emulator: Emulator?,
    paddingLeft: Int,
    paddingTop: Int
) : SurfaceView(context), EmulatorView {
    private val context: Application
    private var startTime: Long = 0
    private var x = 0
    private var y = 0
    private val paddingTop: Int
    private val paddingLeft: Int
    override var viewPort: ViewPort? = null
        private set

    init {
        this.context = context.application
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
        emulator!!.setViewPortSize(vp.width, vp.height)
        startTime = System.currentTimeMillis()
        viewPort = vp
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (emulator == null) {
            return
        }
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