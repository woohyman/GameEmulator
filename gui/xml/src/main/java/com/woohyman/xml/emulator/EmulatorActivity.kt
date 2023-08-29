package com.woohyman.xml.emulator

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.NLog.d
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.PreferenceUtil.ROTATION
import com.woohyman.keyboard.utils.PreferenceUtil.getDisplayRotation
import com.woohyman.xml.ui.control.RestarterActivity

abstract class EmulatorActivity : AppCompatActivity() {

    abstract val fragmentShader: String
    private var exceptionOccurred = false

    val emulatorMediator: EmulatorMediator by lazy {
        EmulatorMediator(this, game, fragmentShader)
    }

    val game: GameDescription by lazy {
        intent.getSerializableExtra(EXTRA_GAME) as GameDescription
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            emulatorMediator.baseDir = EmulatorUtils.getBaseDir(this)
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
            exceptionOccurred = true
            return
        }

        intent.extras?.let {
            if (it.getBoolean(EXTRA_FROM_GALLERY)) {
                emulatorMediator.setShouldPauseOnResume(false)
                intent.removeExtra(EXTRA_FROM_GALLERY)
            }
        }

        emulatorMediator.canRestart = true
        d(TAG, "onCreate - BaseActivity")
        emulatorMediator.slotToRun = -1

        val wParams = window.attributes
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        window.attributes = wParams

        emulatorMediator.gameControlProxy.group.addView(emulatorMediator.emulatorView.asView())
        setContentView(emulatorMediator.gameControlProxy.group)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (exceptionOccurred) {
            return
        }
        oldConfig = changingConfigurations
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        emulatorMediator.setShouldPauseOnResume(false)
        if (resultCode == RESULT_OK) {
            emulatorMediator.canRestart = false
            val slotIdx = data?.getIntExtra(EXTRA_SLOT, -1)
            when (requestCode) {
                REQUEST_SAVE -> {
                    emulatorMediator.slotToSave = slotIdx
                    emulatorMediator.slotToRun = 0
                }

                REQUEST_LOAD -> {
                    emulatorMediator.slotToRun = slotIdx
                    emulatorMediator.slotToSave = null
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val res = super.dispatchTouchEvent(ev)
        emulatorMediator.gameControlProxy.dispatchTouchEvent(ev)
        return res
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        val res = super.dispatchKeyEvent(ev)
        emulatorMediator.gameControlProxy.dispatchKeyEvent(ev)
        return res
    }

    override fun onPause() {
        super.onPause()
        if (emulatorMediator.isRestarting) {
            finish()
            return
        }
        if (exceptionOccurred) {
            return
        }
        pm = null
        if (emulatorMediator.dialog.isShowing) {
            emulatorMediator.dialog.dismiss()
        }
    }

    private fun restartProcess(activityToStartClass: Class<*>) {
        emulatorMediator.isRestarting = true
        val intent = Intent(this, RestarterActivity::class.java)
        intent.putExtras(getIntent())
        intent.putExtra(RestarterActivity.EXTRA_PID, Process.myPid())
        intent.putExtra(RestarterActivity.EXTRA_CLASS, activityToStartClass.name)
        startActivity(intent)
    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()

        emulatorMediator.isRestarting = false
        val isAfterProcessRestart =
            intent.extras?.getBoolean(RestarterActivity.EXTRA_AFTER_RESTART) ?: false

        intent.removeExtra(RestarterActivity.EXTRA_AFTER_RESTART)
        val shouldRestart = emulatorMediator.decreaseResumesToRestart() == 0
        if (!isAfterProcessRestart && shouldRestart && emulatorMediator.canRestart) {
            emulatorMediator.resetProcessResetCounter()
            restartProcess(this.javaClass)
            return
        }
        emulatorMediator.canRestart = true
        if (exceptionOccurred) {
            return
        }
        when (getDisplayRotation(this)) {
            ROTATION.AUTO -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ROTATION.PORT -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ROTATION.LAND -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        pm = packageManager
        pn = packageName
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (exceptionOccurred) {
            return
        }
        emulatorMediator.gameControlProxy.onGameStarted(game)
    }

    override fun startActivity(intent: Intent) {
        emulatorMediator.setShouldPauseOnResume(false)
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        emulatorMediator.setShouldPauseOnResume(false)
        super.startActivity(intent, options)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        i(TAG, "activity key up event:$keyCode")
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> true
            KeyEvent.KEYCODE_BACK -> {
                if (event.isAltPressed) {
                    true
                } else super.onKeyUp(keyCode, event)
            }

            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_3D_MODE,
            KeyEvent.KEYCODE_APP_SWITCH -> super.onKeyUp(
                keyCode,
                event
            )

            else -> true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        i(TAG, "activity key down event:$keyCode")
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                emulatorMediator.gameMenuProxy.openGameMenu()
                true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (event.isAltPressed) {
                    true
                } else super.onKeyDown(keyCode, event)
            }

            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_3D_MODE,
            KeyEvent.KEYCODE_APP_SWITCH -> super.onKeyDown(
                keyCode,
                event
            )

            else -> true
        }
    }

    companion object {
        const val EXTRA_GAME = "game"
        const val EXTRA_SLOT = "slot"
        const val EXTRA_FROM_GALLERY = "fromGallery"
        const val TAG = "EmulatorActivity"
        const val OPEN_GL_BENCHMARK = "openGL"
        const val EMULATION_BENCHMARK = "emulation"
        const val REQUEST_SAVE = 1
        const val REQUEST_LOAD = 2
        var pm: PackageManager? = null
        var pn: String? = null
        var sd: String? = null
        var oldConfig = 0
    }
}