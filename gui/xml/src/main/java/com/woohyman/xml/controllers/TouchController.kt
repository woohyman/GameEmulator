package com.woohyman.xml.controllers

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.controllers.KeyAction
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import com.woohyman.xml.emulator.EmulatorMediator
import com.woohyman.xml.ui.multitouchbutton.MultitouchBtnInterface
import com.woohyman.xml.ui.multitouchbutton.MultitouchButton
import com.woohyman.xml.ui.multitouchbutton.MultitouchImageButton
import com.woohyman.xml.ui.multitouchbutton.MultitouchLayer
import com.woohyman.xml.ui.multitouchbutton.OnMultitouchEventListener
import java.lang.ref.WeakReference

class TouchController(
    private var emulatorMediator: EmulatorMediator
) : EmulatorController,
    OnMultitouchEventListener {
    private var port = 0
    private val resIdMapping = SparseIntArray()
    private var multitouchLayer: MultitouchLayer? = null
    private var remoteIc: ImageView? = null
    private var zapperIc: ImageView? = null
    private var palIc: ImageView? = null
    private var ntscIc: ImageView? = null
    private var muteIc: ImageView? = null
    private var aTurbo: MultitouchImageButton? = null
    private var bTurbo: MultitouchImageButton? = null
    private var abButton: MultitouchImageButton? = null
    private var fastForward: MultitouchImageButton? = null
    private var hidden = false
    private val keyHandler = KeyHandler(this)
    override fun onResume() {
        multitouchLayer?.setVibrationDuration(PreferenceUtil.getVibrationDuration(Utils.getApp()))
        emulator.resetKeys()
        multitouchLayer?.reloadTouchProfile()
        multitouchLayer?.setOpacity(PreferenceUtil.getControlsOpacity(Utils.getApp()))
        multitouchLayer?.setEnableStaticDPAD(!PreferenceUtil.isDynamicDPADEnable(Utils.getApp()))
    }

    override fun onPause() {}

    override fun onWindowFocusChanged(hasFocus: Boolean) {}

    override fun onDestroy() {
        multitouchLayer = null
    }

    override fun connectToEmulator(port: Int) {
        this.port = port
    }

    fun isPointerHandled(pointerId: Int): Boolean {
        return multitouchLayer!!.isPointerHandled(pointerId)
    }

    private fun createView(): View {
        val inflater =
            Utils.getApp().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val layout = inflater.inflate(R.layout.controler_layout, null)
        multitouchLayer = layout.findViewById(R.id.touch_layer)

        val up = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_up)
        up?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_up, emulator.info.getMappingValue(KeyAction.KEY_UP))

        val down = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_down)
        down?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_down, emulator.info.getMappingValue(KeyAction.KEY_DOWN))

        val left = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_left)
        left?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_left, emulator.info.getMappingValue(KeyAction.KEY_LEFT))

        val right = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_right)
        right?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_right, emulator.info.getMappingValue(KeyAction.KEY_RIGHT))

        val a = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_a)
        a?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_a, emulator.info.getMappingValue(KeyAction.KEY_A))

        val b = multitouchLayer?.findViewById<MultitouchImageButton>(R.id.button_b)
        b?.setOnMultitouchEventlistener(this)
        resIdMapping.put(R.id.button_b, emulator.info.getMappingValue(KeyAction.KEY_B))

        aTurbo = multitouchLayer?.findViewById(R.id.button_a_turbo)
        aTurbo?.setOnMultitouchEventlistener(this)
        resIdMapping.put(
            R.id.button_a_turbo,
            emulator.info.getMappingValue(KeyAction.KEY_A_TURBO)
        )

        bTurbo = multitouchLayer?.findViewById(R.id.button_b_turbo)
        bTurbo?.setOnMultitouchEventlistener(this)
        resIdMapping.put(
            R.id.button_b_turbo,
            emulator.info.getMappingValue(KeyAction.KEY_B_TURBO)
        )

        abButton = multitouchLayer?.findViewById(R.id.button_ab)
        fastForward = multitouchLayer?.findViewById(R.id.button_fast_forward)
        fastForward?.setOnMultitouchEventlistener(object : OnMultitouchEventListener {

            override fun onMultitouchEnter(btn: MultitouchBtnInterface?) {
                emulatorMediator.emulatorManagerProxy.onFastForwardDown()
            }

            override fun onMultitouchExit(btn: MultitouchBtnInterface?) {
                emulatorMediator.emulatorManagerProxy.onFastForwardUp()
            }
        })
        val select = layout.findViewById<MultitouchButton>(R.id.button_select)
        select?.setOnMultitouchEventlistener(object : OnMultitouchEventListener {

            override fun onMultitouchEnter(btn: MultitouchBtnInterface?) {
                sendKey(KeyAction.KEY_SELECT.key)
            }

            override fun onMultitouchExit(btn: MultitouchBtnInterface?) {

            }
        })
        val start = layout.findViewById<MultitouchButton>(R.id.button_start)
        start.setOnMultitouchEventlistener(object : OnMultitouchEventListener {

            override fun onMultitouchEnter(btn: MultitouchBtnInterface?) {
                sendKey(KeyAction.KEY_START.key)
            }

            override fun onMultitouchExit(btn: MultitouchBtnInterface?) {

            }
        })
        val menu = layout.findViewById<MultitouchImageButton>(R.id.button_menu)
        menu.setOnMultitouchEventlistener(object : OnMultitouchEventListener {
            override fun onMultitouchEnter(btn: MultitouchBtnInterface?) {
                emulatorMediator.gameMenuProxy.openGameMenu()
            }

            override fun onMultitouchExit(btn: MultitouchBtnInterface?) {

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

    override val view: View = createView()

    fun setStaticDPadEnabled(enabled: Boolean) {
        if (multitouchLayer != null) {
            multitouchLayer!!.setEnableStaticDPAD(enabled)
        }
    }

    private fun sendKey(code: Int) {
        val cc = emulator.info.getMappingValue(code)
        emulator.setKeyPressed(port, cc, true)
        keyHandler.sendEmptyMessageDelayed(cc, 200)
    }

    fun handleKey(cc: Int) {
        emulator.setKeyPressed(port, cc, false)
    }

    override fun onGameStarted() {
        val gfxProfile = emulator.activeGfxProfile
        zapperIc!!.visibility = if (PreferenceUtil.isZapperEnabled(
                Utils.getApp(),
                EmuUtils.fetchProxy.game.checksum
            )
        ) View.VISIBLE else View.GONE
        palIc!!.visibility =
            if (gfxProfile.name == "PAL") View.VISIBLE else View.GONE
        ntscIc!!.visibility =
            if (gfxProfile.name == "NTSC") View.VISIBLE else View.GONE
        val remoteVisible = (PreferenceUtil.isWifiServerEnable(Utils.getApp())
                && EmuUtils.isWifiAvailable(Utils.getApp()))
        remoteIc!!.visibility =
            if (remoteVisible) View.VISIBLE else View.INVISIBLE
        muteIc!!.visibility =
            if (PreferenceUtil.isSoundEnabled(Utils.getApp())) View.GONE else View.VISIBLE
        if (PreferenceUtil.isTurboEnabled(Utils.getApp())) {
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
        if (PreferenceUtil.isFastForwardEnabled(Utils.getApp())) {
            fastForward!!.visibility = View.VISIBLE
            fastForward!!.isEnabled = true
        } else {
            fastForward!!.visibility = View.INVISIBLE
            fastForward!!.isEnabled = false
        }
        abButton!!.visibility =
            if (PreferenceUtil.isABButtonEnabled(Utils.getApp())) View.VISIBLE else View.INVISIBLE
        abButton!!.isEnabled = PreferenceUtil.isABButtonEnabled(Utils.getApp())
        multitouchLayer!!.invalidate()
    }

    override fun onGamePaused() {}

    fun hide() {
        if (!hidden) {
            view.visibility = View.GONE
            hidden = true
        }
    }

    fun show() {
        if (hidden) {
            view.visibility = View.VISIBLE
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

    override fun onMultitouchEnter(btn: MultitouchBtnInterface?) {
        emulator.setKeyPressed(port, resIdMapping[btn!!.getId()], true)
    }

    override fun onMultitouchExit(btn: MultitouchBtnInterface?) {
        emulator.setKeyPressed(port, resIdMapping[btn!!.getId()], false)
    }

}