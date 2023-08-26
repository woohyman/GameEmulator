package com.woohyman.xml.controllers

import android.content.Context
import android.view.MotionEvent
import android.view.View
import com.woohyman.xml.base.emulator.EmulatorActivity
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.PreferenceUtil.isZapperEnabled
import com.woohyman.xml.base.emulator.EmulatorMediator

class ZapperGun(
    private var context: Context?,
    private var emulatorMediator: EmulatorMediator
) :
    EmulatorController {
    private val startX = 0f
    private val startY = 0f
    private var startedInside = false
    private var emulator: Emulator? = null
    private var minX = 0f
    private var maxX = 0f
    private var minY = 0f
    private var maxY = 0f
    private var vpw = 0f
    private var vph = 0f
    private var inited = false
    private var isEnabled = false
    override fun onResume() {}
    override fun onWindowFocusChanged(hasFocus: Boolean) {}
    override fun onPause() {}
    override fun connectToEmulator(port: Int, emulator: Emulator) {
        this.emulator = emulator
    }

    override fun getView(): View {
        return object : View(context) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                if (!isEnabled) {
                    return true
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    val x = event.x
                    val y = event.y
                    if (!startedInside && x >= minX && y >= minY && x <= maxX && y <= maxY) {
                        emulator!!.fireZapper(-1f, -1f)
                    }
                }
                if (event.action == MotionEvent.ACTION_DOWN) {
                    if (!inited) {
                        val viewPort = emulatorMediator.emulatorView.viewPort ?: return true
                        minX = viewPort.x.toFloat()
                        minY = viewPort.y.toFloat()
                        maxX = minX + viewPort.width - 1
                        maxY = minY + viewPort.height - 1
                        vpw = viewPort.width.toFloat()
                        vph = viewPort.height.toFloat()
                        inited = true
                    }
                    val x = event.x
                    val y = event.y
                    startedInside = false
                    if (x >= minX && y >= minY && x <= maxX && y <= maxY) {
                        startedInside = true
                        val tx = (x - minX) / vpw
                        val ty = (y - minY) / vph
                        emulator!!.fireZapper(tx, ty)
                    }
                }
                return true
            }
        }
    }

    override fun onDestroy() {
        context = null
        emulator = null
    }

    override fun onGameStarted(game: GameDescription) {
        isEnabled = isZapperEnabled(context!!, game.checksum)
    }

    override fun onGamePaused(game: GameDescription) {}
}