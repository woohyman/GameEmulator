package com.woohyman.xml.emulator.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Vibrator
import android.util.DisplayMetrics
import android.view.Display
import android.view.MotionEvent
import android.view.View
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.controllers.KeyAction
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import kotlin.math.abs
import kotlin.math.tan

class DynamicDPad(
    display: Display,
    private var touchController: TouchController?
) : EmulatorController {

    private var leftMapped = 0
    private var rightMapped = 0
    private var upMapped = 0
    private var downMapped = 0
    private var port = 0
    private var dpadCenterX = -1f
    private var dpadCenterY = -1f
    private var currentX = 0f
    private var currentY = 0f
    private var minDistX = -1
    private var minDistY = -1
    private val paint = Paint()

    init {
        val dm = DisplayMetrics()
        display.getMetrics(dm)
        val xdpi = dm.xdpi
        val ydpi = dm.ydpi
        val xpcm = xdpi / 2.54f
        val ypcm = ydpi / 2.54f
        val minDistCm = 0.2f
        minDistX = (minDistCm * xpcm).toInt()
        minDistY = (minDistCm * ypcm).toInt()
    }

    override fun onResume() {
        paint.alpha = PreferenceUtil.getControlsOpacity(Utils.getApp())
    }

    override fun connectToEmulator(port: Int) {
        this.port = port
        leftMapped = emulator.info.getMappingValue(KeyAction.KEY_LEFT)
        rightMapped = emulator.info.getMappingValue(KeyAction.KEY_RIGHT)
        downMapped = emulator.info.getMappingValue(KeyAction.KEY_DOWN)
        upMapped = emulator.info.getMappingValue(KeyAction.KEY_UP)
    }

    override val view: View = DPadView(Utils.getApp())

    override fun onDestroy() {
        touchController = null
    }

    private inner class DPadView(context: Context) : View(context) {
        private val ANGLE = 18
        val direction by lazy {
            arrayOf(
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_right),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_right_up),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_up),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_left_up),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_left),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_left_down),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_down),
                BitmapFactory.decodeResource(resources, R.drawable.dynamic_dpad_right_down),
            )
        }

        var btnW = -1
        var btnH = -1
        private var directionIdx = -1
        private val u1 = 0.0174532925f * ANGLE
        private val u2 = 0.0174532925f * (90 - ANGLE)
        private val TAN_DIAGONAL_MIN = tan(u1.toDouble()).toFloat()
        private val TAN_DIAGONAL_MAX = tan(u2.toDouble()).toFloat()
        private var activePointerId = -1
        private var optimizationCounter = 0
        private var vibrationDuration = 100
        private val vibrator: Vibrator

        init {
            paint.color = Color.WHITE
            paint.strokeWidth = 3f
            vibrator = getContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrationDuration = PreferenceUtil.getVibrationDuration(context)
            btnW = direction[0]?.width!!
            btnH = direction[0]?.height!!
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (dpadCenterX != -1f && currentX != -1f) {
                val bitmapOffsetX = (dpadCenterX - btnW / 2).toInt()
                val bitmapOffsetY = (dpadCenterY - btnH / 2).toInt()
                if (directionIdx != -1) {
                    canvas.drawBitmap(
                        direction[directionIdx]!!,
                        bitmapOffsetX.toFloat(),
                        bitmapOffsetY.toFloat(),
                        paint
                    )
                }
            }
        }

        private fun release() {
            emulator.setKeyPressed(port, rightMapped, false)
            emulator.setKeyPressed(port, leftMapped, false)
            emulator.setKeyPressed(port, upMapped, false)
            emulator.setKeyPressed(port, downMapped, false)
            dpadCenterX = -1f
            dpadCenterY = -1f
            currentX = -1f
            currentY = -1f
            activePointerId = -1
            directionIdx = -1
            invalidate()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val actionMasked = event.actionMasked
            if (actionMasked == MotionEvent.ACTION_MOVE) {
                optimizationCounter++
                optimizationCounter = if (optimizationCounter < 5) {
                    return true
                } else {
                    0
                }
            }

            if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                if (activePointerId == -1) {
                    activePointerId = event.getPointerId(event.actionIndex)
                    if (!touchController!!.isPointerHandled(activePointerId)) {
                        val pointerIndex = event.findPointerIndex(activePointerId)
                        dpadCenterX = event.getX(pointerIndex)
                        dpadCenterY = event.getY(pointerIndex)
                        if (vibrationDuration > 0) {
                            vibrator.vibrate(vibrationDuration.toLong())
                        }
                        return true
                    } else {
                        activePointerId = -1
                    }
                }
            }

            if (actionMasked == MotionEvent.ACTION_MOVE) {
                if (dpadCenterX != -1f) {
                    for (i in 0 until event.pointerCount) {
                        if (event.getPointerId(i) == activePointerId) {
                            if (!touchController!!.isPointerHandled(activePointerId)) {
                                val x = event.getX(i)
                                val y = event.getY(i)
                                currentX = x
                                currentY = y
                                val dx = x - dpadCenterX
                                val dy = dpadCenterY - y
                                val tan = abs(dy) / abs(dx)
                                var left: Boolean
                                var right: Boolean
                                var up: Boolean
                                var down: Boolean
                                down = false
                                up = down
                                left = up
                                right = left
                                if (dx > minDistX) {
                                    right = true
                                } else if (dx < -minDistX) {
                                    left = true
                                }
                                if (tan > TAN_DIAGONAL_MIN) {
                                    if (dy > minDistY) {
                                        up = true
                                    } else if (dy < -minDistY) {
                                        down = true
                                    }
                                }
                                if (tan > TAN_DIAGONAL_MAX) {
                                    right = false
                                    left = false
                                }
                                emulator.setKeyPressed(port, rightMapped, right)
                                emulator.setKeyPressed(port, leftMapped, left)
                                emulator.setKeyPressed(port, upMapped, up)
                                emulator.setKeyPressed(port, downMapped, down)
                                if (right) {
                                    directionIdx = if (down) {
                                        7
                                    } else if (up) {
                                        1
                                    } else {
                                        0
                                    }
                                } else if (left) {
                                    directionIdx = if (down) {
                                        5
                                    } else if (up) {
                                        3
                                    } else {
                                        4
                                    }
                                } else {
                                    if (up) {
                                        directionIdx = 2
                                    } else if (down) {
                                        directionIdx = 6
                                    }
                                }
                                invalidate()
                                return true
                            } else {
                                release()
                            }
                        }
                    }
                }
            }
            when (actionMasked) {
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_UP ->
                    if (
                        activePointerId != -1
                        && event.getPointerId(event.actionIndex) == activePointerId
                    ) {
                        release()
                    }
            }
            return true
        }
    }
}