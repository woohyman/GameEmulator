package com.woohyman.gui.controllers

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.woohyman.gui.base.EmulatorActivity
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.PreferenceUtil.isQuickSaveEnabled

class QuickSaveController(
    var activity: EmulatorActivity?,
    var touchController: TouchController?
) : EmulatorController {
    private val gestureDetector: GestureDetectorCompat
    private var screenCenterX = 0
    private var isEnabled = false

    init {
        gestureDetector = GestureDetectorCompat(activity!!, GestureListener())
    }

    override fun onResume() {
        isEnabled = isQuickSaveEnabled(activity)
    }

    override fun onPause() {}
    override fun onWindowFocusChanged(hasFocus: Boolean) {}
    override fun onGameStarted(game: GameDescription) {}
    override fun onGamePaused(game: GameDescription) {}
    override fun connectToEmulator(port: Int, emulator: Emulator) {}
    override fun getView(): View {
        return object : View(activity) {
            override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
                super.onSizeChanged(w, h, oldw, oldh)
                screenCenterX = w / 2
            }

            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (!isEnabled) {
                    return true
                }
                val pointerId = event.getPointerId(event.actionIndex)
                return (touchController!!.isPointerHandled(pointerId)
                        || gestureDetector.onTouchEvent(event))
            }
        }
    }

    override fun onDestroy() {
        activity = null
        touchController = null
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val x = e.x
            if (x < screenCenterX) {
                activity!!.quickLoad()
            } else if (x > screenCenterX) {
                activity!!.quickSave()
            }
            return true
        }
    }
}