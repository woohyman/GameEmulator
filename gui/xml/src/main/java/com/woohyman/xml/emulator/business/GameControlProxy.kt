package com.woohyman.xml.emulator.business

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.emulator.EmulatorMediator
import com.woohyman.xml.controllers.DynamicDPad
import com.woohyman.xml.controllers.KeyboardController
import com.woohyman.xml.controllers.QuickSaveController
import com.woohyman.xml.controllers.TouchController
import com.woohyman.xml.controllers.ZapperGun

class GameControlProxy constructor(
    private val emulatorMediator: EmulatorMediator,
) : DefaultLifecycleObserver, EmulatorController {

    private var controllers: MutableList<EmulatorController> = mutableListOf()
    private var controllerViews: MutableList<View> = ArrayList()

    val group: ViewGroup by lazy {
        FrameLayout(Utils.getApp()).also {
            val display = emulatorMediator.activity?.windowManager?.defaultDisplay ?: return@also
            val w = EmuUtils.getDisplayWidth(display)
            val h = EmuUtils.getDisplayHeight(display)
            val params = ViewGroup.LayoutParams(w, h)
            it.layoutParams = params
        }
    }

    private val dynamic: DynamicDPad? by lazy {
        touchController.connectToEmulator(0)
        val display = emulatorMediator.activity?.windowManager?.defaultDisplay ?: return@lazy null
        DynamicDPad(Utils.getApp(), display, touchController)
    }

    private val touchController: TouchController by lazy {
        TouchController(emulatorMediator)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        controllers.add(touchController)

        dynamic?.let {
            controllers.add(it)

            it.connectToEmulator(0)
        }

        val qsc = QuickSaveController(emulatorMediator, touchController)
        controllers.add(qsc)

        val zapper = ZapperGun(Utils.getApp(), emulatorMediator)
        zapper.connectToEmulator(1)
        controllers.add(zapper)

        val kc = KeyboardController(
            Utils.getApp(),
            EmuUtils.fetchProxy.game.checksum,
            emulatorMediator
        )
        controllers.add(kc)


        for (controller in controllers) {
            val controllerView = controller.view
            controllerViews.add(controllerView)
            group.addView(controllerView)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        dynamic?.let {
            if (PreferenceUtil.isDynamicDPADEnable(Utils.getApp())) {
                if (!controllers.contains(it)) {
                    controllers.add(it)
                    controllerViews.add(it.view)
                }
                PreferenceUtil.setDynamicDPADUsed(Utils.getApp(), true)
            } else {
                controllers.remove(it)
                controllerViews.remove(it.view)
            }
        }

        if (PreferenceUtil.isFastForwardEnabled(Utils.getApp())) {
            PreferenceUtil.setFastForwardUsed(Utils.getApp(), true)
        }
        if (PreferenceUtil.isScreenSettingsSaved(Utils.getApp())) {
            PreferenceUtil.setScreenLayoutUsed(Utils.getApp(), true)
        }

        for (controller in controllers) {
            controller.onResume()
        }
        try {
            for (controller in controllers) {
                controller.onGameStarted()
            }
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
        }

    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        for (controller in controllers) {
            controller.onPause()
            controller.onGamePaused()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        for (controller in controllers) {
            controller.onDestroy()
        }
        controllers.clear()
        controllerViews.clear()
        group.removeAllViews()
    }

    override fun onResume() {
        controllers.forEach {
            it.onResume()
        }
    }

    override fun onPause() {
        controllers.forEach {
            it.onPause()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        controllers.forEach {
            it.onWindowFocusChanged(hasFocus)
        }
    }

    override fun onGameStarted() {
        controllers.forEach {
            it.onGameStarted()
        }
    }

    override fun onGamePaused() {
        controllers.forEach {
            it.onGamePaused()
        }
    }

    override fun connectToEmulator(port: Int) {
        controllers.forEach {
            it.connectToEmulator(port)
        }
    }

    override fun getView(): View {
        controllers.forEach {
            it.view
        }
        return group
    }

    override fun onDestroy() {
        controllers.forEach {
            it.onDestroy()
        }
    }

    fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        touchController.show()
        var result = true
        controllerViews.forEach {
            if (!it.dispatchTouchEvent(ev)) {
                result = false
            }
        }
        return result
    }

    fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        var result = true
        controllerViews.forEach {
            if (!it.dispatchKeyEvent(ev)) {
                result = false
            }
        }
        return result
    }

    fun hideTouchController() {
        touchController.hide()
    }
}