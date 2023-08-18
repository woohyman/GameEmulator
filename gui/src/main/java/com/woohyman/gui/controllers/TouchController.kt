package com.woohyman.gui.controllers

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import nostalgia.framework.emulator.Emulator
import nostalgia.framework.R
import nostalgia.framework.base.EmulatorActivity
import nostalgia.framework.controllers.EmulatorController
import nostalgia.framework.data.database.GameDescription
import nostalgia.framework.ui.multitouchbutton.MultitouchBtnInterface
import nostalgia.framework.ui.multitouchbutton.MultitouchButton
import nostalgia.framework.ui.multitouchbutton.MultitouchImageButton
import nostalgia.framework.ui.multitouchbutton.MultitouchLayer
import nostalgia.framework.ui.multitouchbutton.OnMultitouchEventListener
import nostalgia.framework.utils.EmuUtils
import nostalgia.framework.utils.PreferenceUtil
import java.lang.ref.WeakReference

class TouchController(private var emulatorActivity: EmulatorActivity?) : EmulatorController,
    OnMultitouchEventListener {
    private var emulator: Emulator? = null
    private var port = 0
    private var mapping: SparseIntArray? = null
    private val resIdMapping = SparseIntArray()
    private var multitouchLayer: MultitouchLayer? = null
    private var remoteIc: ImageView? = null
    private var zapperIc: ImageView? = null
    private var palIc: ImageView? = null
    private var ntscIc: ImageView? = null
    private var muteIc: ImageView? = null
    private var view: View? = null
    private var aTurbo: MultitouchImageButton? = null
    private var bTurbo: MultitouchImageButton? = null
    private var abButton: MultitouchImageButton? = null
    private var fastForward: MultitouchImageButton? = null
    private var hidden = false
    private val keyHandler = KeyHandler(this)
    override fun onResume() {
        if (multitouchLayer != null) {
            multitouchLayer!!.setVibrationDuration(
                PreferenceUtil.getVibrationDuration(
                    emulatorActivity!!
                )
            )
        }
        emulator!!.resetKeys()
        multitouchLayer!!.reloadTouchProfile()
        multitouchLayer!!.setOpacity(PreferenceUtil.getControlsOpacity(emulatorActivity))
        multitouchLayer!!.setEnableStaticDPAD(!PreferenceUtil.isDynamicDPADEnable(emulatorActivity))
    }

    override fun onPause() {}
    override fun onWindowFocusChanged(hasFocus: Boolean) {}
    override fun onDestroy() {
        multitouchLayer = null
        emulatorActivity = null
    }

    override fun connectToEmulator(port: Int, emulator: Emulator) {
        this.emulator = emulator
        this.port = port
        mapping = emulator.info?.keyMapping
    }

    fun isPointerHandled(pointerId: Int): Boolean {
        return multitouchLayer!!.isPointerHandled(pointerId)
    }

    private fun createView(): View {
        val inflater =
            emulatorActivity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.controler_layout, null)
        multitouchLayer = layout.findViewById(R.id.touch_layer)
        val up = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_up)
        up?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_up, mapping!![EmulatorController.KEY_UP])
        val down = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_down)
        down?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_down, mapping!![EmulatorController.KEY_DOWN])
        val left = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_left)
        left?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_left, mapping!![EmulatorController.KEY_LEFT])
        val right = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_right)
        right?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_right, mapping!![EmulatorController.KEY_RIGHT])
        val a = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_a)
        a?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_a, mapping!![EmulatorController.KEY_A])
        val b = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_b)
        b?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_b, mapping!![EmulatorController.KEY_B])
        aTurbo = multitouchLayer?.findViewById(R.id.button_a_turbo)
        aTurbo?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_a_turbo, mapping!![EmulatorController.KEY_A_TURBO])
        bTurbo = multitouchLayer?.findViewById(R.id.button_b_turbo)
        bTurbo?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_b_turbo, mapping!![EmulatorController.KEY_B_TURBO])
        abButton = multitouchLayer?.findViewById(R.id.button_ab)
        fastForward = multitouchLayer?.findViewById(R.id.button_fast_forward)
        fastForward?.setOnMultitouchEventlistener(object : OnMultitouchEventListener {
            override fun onMultitouchExit(btn: MultitouchBtnInterface) {
                emulatorActivity!!.onFastForwardUp()
            }

            override fun onMultitouchEnter(btn: MultitouchBtnInterface) {
                emulatorActivity!!.onFastForwardDown()
            }
        })
        val select = layout.findViewById<MultitouchButton>(R.id.button_select)
        select?.setOnMultitouchEventlistener(object : OnMultitouchEventListener {
            override fun onMultitouchExit(btn: MultitouchBtnInterface) {}
            override fun onMultitouchEnter(btn: MultitouchBtnInterface) {
                sendKey(EmulatorController.KEY_SELECT)
            }
        })
        val start = layout.findViewById<MultitouchButton>(R.id.button_start)
        start.setOnMultitouchEventlistener(object : OnMultitouchEventListener {
            override fun onMultitouchExit(btn: MultitouchBtnInterface) {}
            override fun onMultitouchEnter(btn: MultitouchBtnInterface) {
                sendKey(EmulatorController.KEY_START)
            }
        })
        val menu = layout.findViewById<MultitouchImageButton>(R.id.button_menu)
        menu.setOnMultitouchEventlistener(object : OnMultitouchEventListener {
            override fun onMultitouchExit(btn: MultitouchBtnInterface) {}
            override fun onMultitouchEnter(btn: MultitouchBtnInterface) {
                emulatorActivity!!.openGameMenu()
            }
        })
        val center = layout.findViewById<View>(R.id.button_center)
        val views = arrayOf(menu, select, start, up, down, right, left, a, b, center)
        for (view in views) {
            if (view != null) {
                view.isFocusable = false
            }
        }
        remoteIc = layout.findViewById(R.id.ic_game_remote)
        zapperIc = layout.findViewById(R.id.ic_game_zapper)
        palIc = layout.findViewById(R.id.ic_game_pal)
        ntscIc = layout.findViewById(R.id.ic_game_ntsc)
        muteIc = layout.findViewById(R.id.ic_game_muted)
        return layout
    }

    override fun getView(): View {
        if (view == null) {
            view = createView()
        }
        return view!!
    }

    fun setStaticDPadEnabled(enabled: Boolean) {
        if (multitouchLayer != null) {
            multitouchLayer!!.setEnableStaticDPAD(enabled)
        }
    }

    private fun sendKey(code: Int) {
        val cc = mapping!![code]
        emulator!!.setKeyPressed(port, cc, true)
        keyHandler.sendEmptyMessageDelayed(cc, 200)
    }

    fun handleKey(cc: Int) {
        emulator!!.setKeyPressed(port, cc, false)
    }

    override fun onMultitouchEnter(btn: MultitouchBtnInterface) {
        emulator!!.setKeyPressed(port, resIdMapping[btn.id], true)
    }

    override fun onMultitouchExit(btn: MultitouchBtnInterface) {
        emulator!!.setKeyPressed(port, resIdMapping[btn.id], false)
    }

    override fun onGameStarted(game: GameDescription) {
        val gfxProfile = emulator!!.activeGfxProfile
        zapperIc!!.visibility = if (PreferenceUtil.isZapperEnabled(
                emulatorActivity!!,
                game.checksum
            )
        ) View.VISIBLE else View.GONE
        palIc!!.visibility =
            if (gfxProfile!!.name == "PAL") View.VISIBLE else View.GONE
        ntscIc!!.visibility =
            if (gfxProfile.name == "NTSC") View.VISIBLE else View.GONE
        val remoteVisible = (PreferenceUtil.isWifiServerEnable(emulatorActivity!!)
                && EmuUtils.isWifiAvailable(emulatorActivity!!))
        remoteIc!!.visibility =
            if (remoteVisible) View.VISIBLE else View.INVISIBLE
        muteIc!!.visibility =
            if (PreferenceUtil.isSoundEnabled(emulatorActivity)) View.GONE else View.VISIBLE
        if (PreferenceUtil.isTurboEnabled(emulatorActivity)) {
            aTurbo!!.visibility = View.VISIBLE
            bTurbo!!.visibility = View.VISIBLE
            aTurbo!!.isEnabled = true
            bTurbo!!.isEnabled = true
        } else {
            aTurbo!!.visibility = View.INVISIBLE
            bTurbo!!.visibility = View.INVISIBLE
            aTurbo!!.isEnabled = false
            bTurbo!!.isEnabled = false
        }
        if (PreferenceUtil.isFastForwardEnabled(emulatorActivity)) {
            fastForward!!.visibility = View.VISIBLE
            fastForward!!.isEnabled = true
        } else {
            fastForward!!.visibility = View.INVISIBLE
            fastForward!!.isEnabled = false
        }
        abButton!!.visibility =
            if (PreferenceUtil.isABButtonEnabled(emulatorActivity)) View.VISIBLE else View.INVISIBLE
        abButton!!.isEnabled = PreferenceUtil.isABButtonEnabled(emulatorActivity)
        multitouchLayer!!.invalidate()
    }

    override fun onGamePaused(game: GameDescription) {}
    fun hide() {
        if (!hidden) {
            emulatorActivity!!.runOnUiThread { view!!.visibility = View.GONE }
            hidden = true
        }
    }

    fun show() {
        if (hidden) {
            emulatorActivity!!.runOnUiThread { view!!.visibility = View.VISIBLE }
            hidden = false
        }
    }

    private class KeyHandler internal constructor(controller: TouchController) : Handler() {
        var weakController: WeakReference<TouchController>

        init {
            weakController = WeakReference(controller)
        }

        override fun handleMessage(msg: Message) {
            val controller = weakController.get()
            controller?.handleKey(msg.what)
        }
    }

    companion object {
        private const val TAG = "controllers.TouchController"
    }
}