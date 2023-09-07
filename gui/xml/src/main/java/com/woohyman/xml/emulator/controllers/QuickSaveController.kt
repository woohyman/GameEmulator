package com.woohyman.xml.emulator.controllers

import android.annotation.SuppressLint
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.utils.PreferenceUtil.isQuickSaveEnabled
import com.woohyman.xml.emulator.IEmulatorMediator

class QuickSaveController(
    var emulatorMediator: IEmulatorMediator,
    var touchController: TouchController?
) : EmulatorController {

    private val gestureDetector = GestureDetectorCompat(Utils.getApp(), GestureListener())
    private var screenCenterX = 0
    private var isEnabled = false

    override fun onResume() {
        isEnabled = isQuickSaveEnabled(Utils.getApp())
    }

    override fun connectToEmulator(port: Int) {}
    override val view: View = object : View(Utils.getApp()) {
        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            screenCenterX = w / 2
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (!isEnabled) {
                return true
            }
            val pointerId = event.getPointerId(event.actionIndex)
            return (touchController!!.isPointerHandled(pointerId)
                    || gestureDetector.onTouchEvent(event))
        }
    }

    override fun onDestroy() {
        touchController = null
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = e.x
            if (x < screenCenterX) {
                emulatorMediator.quickLoad()
            } else if (x > screenCenterX) {
                emulatorMediator.quickSave()
            }
            return true
        }
    }
}