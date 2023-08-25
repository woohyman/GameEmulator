package com.woohyman.xml.base.emulator

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.controllers.DynamicDPad
import com.woohyman.xml.controllers.KeyboardController
import com.woohyman.xml.controllers.QuickSaveController
import com.woohyman.xml.controllers.TouchController
import com.woohyman.xml.controllers.ZapperGun

class ControlProxy(
    private val activity: EmulatorActivity,
    private val emulatorInstance: Emulator,
    private val game: GameDescription,
) : DefaultLifecycleObserver, EmulatorController {
    private var controllers: MutableList<EmulatorController> = mutableListOf()
    private var controllerViews: MutableList<View> = ArrayList()
    val group: ViewGroup by lazy {
        FrameLayout(activity)
    }

    private val dynamic: DynamicDPad by lazy {
        touchController.connectToEmulator(0, emulatorInstance)
        DynamicDPad(activity, activity.windowManager.defaultDisplay, touchController)
    }

    private val touchController: TouchController by lazy {
        TouchController(activity)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        controllers.add(touchController)
        controllers.add(dynamic)

        dynamic.connectToEmulator(0, emulatorInstance)
        val qsc = QuickSaveController(activity, touchController)
        controllers.add(qsc)

        val zapper = ZapperGun(Utils.getApp(), activity)
        zapper.connectToEmulator(1, emulatorInstance)
        controllers.add(zapper)

        val kc = KeyboardController(emulatorInstance, Utils.getApp(), game.checksum, activity)
        controllers.add(kc)


        for (controller in controllers) {
            val controllerView = controller.view
            controllerViews.add(controllerView)
            group.addView(controllerView)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        if (PreferenceUtil.isDynamicDPADEnable(activity)) {
            if (!controllers.contains(dynamic)) {
                controllers.add(dynamic)
                controllerViews.add(dynamic.view)
            }
            PreferenceUtil.setDynamicDPADUsed(activity, true)
        } else {
            controllers.remove(dynamic)
            controllerViews.remove(dynamic.view)
        }
        if (PreferenceUtil.isFastForwardEnabled(activity)) {
            PreferenceUtil.setFastForwardUsed(activity, true)
        }
        if (PreferenceUtil.isScreenSettingsSaved(activity)) {
            PreferenceUtil.setScreenLayoutUsed(activity, true)
        }
        for (controller in controllers) {
            controller.onResume()
        }
        for (controller in controllers) {
            controller.onGameStarted(game)
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        for (controller in controllers) {
            controller.onPause()
            controller.onGamePaused(game)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        for (controller in controllers) {
            controller.onDestroy()
        }
        controllers.clear()
        controllerViews.clear()
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

    override fun onGameStarted(game: GameDescription?) {
        controllers.forEach {
            it.onGameStarted(game)
        }
    }

    override fun onGamePaused(game: GameDescription?) {
        controllers.forEach {
            it.onGamePaused(game)
        }
    }

    override fun connectToEmulator(port: Int, emulator: Emulator?) {
        controllers.forEach {
            it.connectToEmulator(port, emulator)
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