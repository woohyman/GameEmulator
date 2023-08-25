package com.woohyman.xml.base.emulator

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap.CompressFormat
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.Benchmark.BenchmarkCallback
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.emulator.EmulatorRunner.OnNotRespondingListener
import com.woohyman.keyboard.emulator.EmulatorView
import com.woohyman.keyboard.utils.DialogUtils.show
import com.woohyman.keyboard.utils.EmuUtils.checkGL20Support
import com.woohyman.keyboard.utils.EmuUtils.createScreenshotBitmap
import com.woohyman.keyboard.utils.NLog.d
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.PreferenceUtil.ROTATION
import com.woohyman.keyboard.utils.PreferenceUtil.getDisplayRotation
import com.woohyman.keyboard.utils.PreferenceUtil.getEmulationQuality
import com.woohyman.keyboard.utils.PreferenceUtil.isAutoHideControls
import com.woohyman.keyboard.utils.PreferenceUtil.isFastForwardToggleable
import com.woohyman.keyboard.utils.PreferenceUtil.isTimeshiftEnabled
import com.woohyman.keyboard.utils.PreferenceUtil.setBenchmarked
import com.woohyman.keyboard.utils.PreferenceUtil.setEmulationQuality
import com.woohyman.xml.BaseApplication
import com.woohyman.xml.R
import com.woohyman.xml.ui.cheats.CheatsActivity
import com.woohyman.xml.ui.control.RestarterActivity
import com.woohyman.xml.ui.gamegallery.Constants.DIALOAG_TYPE_LOAD
import com.woohyman.xml.ui.gamegallery.Constants.DIALOAG_TYPE_SAVE
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_BASE_DIRECTORY
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_DIALOG_TYPE_INT
import com.woohyman.xml.ui.gamegallery.SlotSelectionActivity
import com.woohyman.xml.ui.menu.GameMenu
import com.woohyman.xml.ui.menu.GameMenu.GameMenuItem
import com.woohyman.xml.ui.menu.GameMenu.OnGameMenuListener
import com.woohyman.xml.ui.opengl.OpenGLView
import com.woohyman.xml.ui.preferences.GamePreferenceActivity
import com.woohyman.xml.ui.preferences.GamePreferenceFragment
import com.woohyman.xml.ui.preferences.GeneralPreferenceActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceFragment
import com.woohyman.xml.ui.timetravel.TimeTravelDialog
import com.woohyman.xml.ui.widget.UnacceleratedView
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class EmulatorActivity : AppCompatActivity(), OnGameMenuListener, OnNotRespondingListener {
    private val maxPRC = 10
    var isRestarting = false
    var canRestart = false
    var runTimeMachine = false
    val dialog: TimeTravelDialog by lazy {
        TimeTravelDialog(this, gameManagerProxy, game)
    }
    val gameMenu: GameMenu by lazy {
        GameMenu(this, this)
    }
    private val game: GameDescription by lazy {
        intent.getSerializableExtra(EXTRA_GAME) as GameDescription
    }

    private var autoHide = false
    private var warningShowing = atomic(false)
    private var isFF = false
    private var isToggleFF = false
    private var isFFPressed = false
    private var exceptionOccurred = false
    var slotToRun: Int? = null
    var slotToSave: Int? = null
    var emulatorView: EmulatorView? = null
    val benchmarkCallback: BenchmarkCallback = object : BenchmarkCallback {
        private var numTests = 0
        private var numOk = 0
        override fun onBenchmarkReset(benchmark: Benchmark) {}
        override fun onBenchmarkEnded(benchmark: Benchmark, steps: Int, totalTime: Long) {
            val millisPerFrame = totalTime / steps.toFloat()
            numTests++
            if (benchmark.name == OPEN_GL_BENCHMARK) {
                if (millisPerFrame < 17) {
                    numOk++
                }
            }
            if (benchmark.name == EMULATION_BENCHMARK) {
                if (millisPerFrame < 17) {
                    numOk++
                }
            }
            if (numTests == 2) {
                setBenchmarked(this@EmulatorActivity, true)
                if (numOk == 2) {
                    emulatorView?.setQuality(2)
                    setEmulationQuality(this@EmulatorActivity, 2)
                }
            }
        }
    }

    var baseDir: String? = null
    abstract val emulatorInstance: Emulator
    abstract val fragmentShader: String
    fun getTextureBounds(emulator: Emulator?): IntArray? {
        return null
    }

    val gLTextureSize = 256

    fun hasGLPalette(): Boolean {
        return true
    }

    private val emulatorControlProxy by lazy {
        EmulatorControlProxy(this, emulatorInstance, game)
    }

    val gameManagerProxy by lazy {
        GameManagerProxy(this, emulatorInstance, game)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(emulatorControlProxy)
        lifecycle.addObserver(gameManagerProxy)

        val extras = intent.extras
        if (extras != null && extras.getBoolean(EXTRA_FROM_GALLERY)) {
            setShouldPauseOnResume(false)
            intent.removeExtra(EXTRA_FROM_GALLERY)
        }
        canRestart = true
        try {
            baseDir = EmulatorUtils.getBaseDir(this)
        } catch (e: EmulatorException) {
            handleException(e)
            exceptionOccurred = true
            return
        }
        d(TAG, "onCreate - BaseActivity")
        val hasOpenGL20 = checkGL20Support(applicationContext)
        slotToRun = -1
        val wParams = window.attributes
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        window.attributes = wParams

        val paddingLeft = 0
        var paddingTop = 0
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paddingTop = resources.getDimensionPixelSize(R.dimen.top_panel_touchcontroler_height)
        }
        val shader = fragmentShader
        var openGLView: OpenGLView? = null

        if (hasOpenGL20) {
            openGLView = OpenGLView(this, emulatorInstance, paddingLeft, paddingTop, shader)
            if (gameManagerProxy.needsBenchmark) {
                openGLView.setBenchmark(Benchmark(OPEN_GL_BENCHMARK, 200, benchmarkCallback))
            }
        }

        emulatorView =
            openGLView ?: UnacceleratedView(this, emulatorInstance, paddingLeft, paddingTop)
        emulatorControlProxy.group.addView(emulatorView?.asView())
        setContentView(emulatorControlProxy.group)
    }

    fun hideTouchController() {
        i(TAG, "hide controler")
        if (autoHide) {
            emulatorControlProxy.hideTouchController()
        }
    }

    @MainThread
    override fun onNotResponding() {
        warningShowing.getAndUpdate {
            if (!it) {
                true
            } else {
                return
            }
        }
        val dialog = AlertDialog.Builder(this)
            .setMessage(R.string.too_slow)
            .create()
        dialog.setOnDismissListener { dialog1: DialogInterface? -> finish() }
        gameManagerProxy.pauseEmulation()
        show(dialog, true)
    }

    val viewPort: ViewPort?
        get() = emulatorView?.viewPort

    override fun onDestroy() {
        super.onDestroy()
        if (exceptionOccurred) {
            return
        }
        oldConfig = changingConfigurations
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setShouldPauseOnResume(false)
        if (resultCode == RESULT_OK) {
            canRestart = false
            val slotIdx = data?.getIntExtra(EXTRA_SLOT, -1)
            when (requestCode) {
                REQUEST_SAVE -> {
                    slotToSave = slotIdx
                    slotToRun = 0
                }

                REQUEST_LOAD -> {
                    slotToRun = slotIdx
                    slotToSave = null
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val res = super.dispatchTouchEvent(ev)
        emulatorControlProxy.dispatchTouchEvent(ev)
        return res
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        val res = super.dispatchKeyEvent(ev)
        emulatorControlProxy.dispatchKeyEvent(ev)
        return res
    }

    private fun setShouldPauseOnResume(b: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putBoolean("emulator_activity_pause", b)
            .apply()
    }

    fun shouldPause(): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("emulator_activity_pause", false)
    }

    override fun onPause() {
        super.onPause()
        if (isRestarting) {
            finish()
            return
        }
        if (exceptionOccurred) {
            return
        }
        pm = null
        if (gameMenu.isOpen) {
            gameMenu.dismiss()
        }
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun onFastForwardDown() {
        if (isToggleFF) {
            if (!isFFPressed) {
                isFFPressed = true
                isFF = !isFF
                gameManagerProxy.setFastForwardEnabled(isFF)
            }
        } else {
            gameManagerProxy.setFastForwardEnabled(true)
        }
    }

    fun onFastForwardUp() {
        if (!isToggleFF) {
            gameManagerProxy.setFastForwardEnabled(false)
        }
        isFFPressed = false
    }

    fun handleException(e: EmulatorException) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(e.getMessage(this)).create()
        dialog.setOnDismissListener { dialog1: DialogInterface? -> runOnUiThread { finish() } }
        show(dialog, true)
    }

    private fun restartProcess(activityToStartClass: Class<*>) {
        isRestarting = true
        val intent = Intent(this, RestarterActivity::class.java)
        intent.putExtras(getIntent())
        intent.putExtra(RestarterActivity.EXTRA_PID, Process.myPid())
        val className = activityToStartClass.name
        intent.putExtra(RestarterActivity.EXTRA_CLASS, className)
        startActivity(intent)
    }

    private fun decreaseResumesToRestart(): Int {
        var prc = PreferenceManager.getDefaultSharedPreferences(this).getInt("PRC", maxPRC)
        if (prc > 0) {
            prc--
        }
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putInt("PRC", prc)
        editor.apply()
        return prc
    }

    private fun resetProcessResetCounter() {
        val editor = PreferenceManager.getDefaultSharedPreferences(this).edit()
        editor.putInt("PRC", maxPRC)
        editor.apply()
    }

    fun quickSave() {
        gameManagerProxy.saveState(10)
        runOnUiThread {
            Toast.makeText(
                this,
                "state saved", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun quickLoad() {
        gameManagerProxy.loadState(10)
    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()
        isRestarting = false
        val extras = intent.extras
        var isAfterProcessRestart = false
        if (extras != null) {
            isAfterProcessRestart =
                extras.getBoolean(RestarterActivity.EXTRA_AFTER_RESTART)
        }
        intent.removeExtra(RestarterActivity.EXTRA_AFTER_RESTART)
        val shouldRestart = decreaseResumesToRestart() == 0
        if (!isAfterProcessRestart && shouldRestart && canRestart) {
            resetProcessResetCounter()
            restartProcess(this.javaClass)
            return
        }
        canRestart = true
        if (exceptionOccurred) {
            return
        }
        autoHide = isAutoHideControls(this)
        isToggleFF = isFastForwardToggleable(this)
        isFF = false
        isFFPressed = false
        when (getDisplayRotation(this)) {
            ROTATION.AUTO -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ROTATION.PORT -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            ROTATION.LAND -> this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        pm = packageManager
        pn = packageName
        try {
            setShouldPauseOnResume(true)
            slotToRun = 0
            val quality = getEmulationQuality(this)
            emulatorView?.setQuality(quality)
            emulatorView?.onResume()
            enableCheats()
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    @MainThread
    private fun enableCheats() {
        var numCheats = 0
        try {
            numCheats = gameManagerProxy.enableCheats(this, game)
        } catch (e: EmulatorException) {
            Toast.makeText(
                this, e.getMessage(this),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (numCheats > 0) {
            Toast.makeText(
                this, getString(R.string.toast_cheats_enabled, numCheats),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (exceptionOccurred) {
            return
        }
        emulatorControlProxy.onGameStarted(game)
    }

    fun openGameMenu() {
        gameMenu.open()
    }

    override fun onGameMenuCreate(menu: GameMenu) {
        menu.add(R.string.game_menu_reset, R.drawable.ic_reload)
        menu.add(R.string.game_menu_save, R.drawable.ic_save)
        menu.add(R.string.game_menu_load, R.drawable.ic_load)
        menu.add(R.string.game_menu_cheats, R.drawable.ic_cheats)
        menu.add(R.string.game_menu_back_to_past, R.drawable.ic_time_machine)
        menu.add(R.string.game_menu_screenshot, R.drawable.ic_make_screenshot)
        val ea = application as BaseApplication
        val settingsStringRes =
            if (ea.hasGameMenu()) R.string.game_menu_settings else R.string.gallery_menu_pref
        menu.add(settingsStringRes, R.drawable.ic_game_settings)
    }

    override fun onGameMenuPrepare(menu: GameMenu) {
        val backToPast = menu.getItem(R.string.game_menu_back_to_past)
        backToPast?.enable = isTimeshiftEnabled(this)
        i(TAG, "prepare menu")
    }

    override fun onGameMenuClosed(menu: GameMenu) {
        try {
            if (runTimeMachine || menu.isOpen) {
                return
            }
            gameManagerProxy.resumeEmulation()
            emulatorControlProxy.onGameStarted(game)
            emulatorControlProxy.onResume()
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    override fun onGameMenuOpened(menu: GameMenu?) {
        i(TAG, "on game menu open")
        try {
            gameManagerProxy.pauseEmulation()
            emulatorControlProxy.onGamePaused(game)
            emulatorControlProxy.onPause()
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    override fun startActivity(intent: Intent) {
        setShouldPauseOnResume(false)
        super.startActivity(intent)
    }

    override fun startActivity(intent: Intent, options: Bundle?) {
        setShouldPauseOnResume(false)
        super.startActivity(intent, options)
    }

    private fun freeStartActivityForResult(activity: Activity, intent: Intent, requestCode: Int) {
        setShouldPauseOnResume(false)
        startActivityForResult(intent, requestCode)
    }

    private fun freeStartActivity(activity: Activity, intent: Intent) {
        setShouldPauseOnResume(false)
        startActivity(intent)
    }

    override fun onGameMenuItemSelected(menu: GameMenu?, item: GameMenuItem) {
        try {
            if (item.id == R.string.game_menu_back_to_past) {
                onGameBackToPast()
            } else if (item.id == R.string.game_menu_reset) {
                gameManagerProxy.resetEmulator()
                enableCheats()
            } else if (item.id == R.string.game_menu_save) {
                val i = Intent(this, SlotSelectionActivity::class.java)
                i.putExtra(EXTRA_GAME, game)
                i.putExtra(EXTRA_BASE_DIRECTORY, baseDir)
                i.putExtra(
                    EXTRA_DIALOG_TYPE_INT,
                    DIALOAG_TYPE_SAVE
                )
                freeStartActivityForResult(this, i, REQUEST_SAVE)
            } else if (item.id == R.string.game_menu_load) {
                val i = Intent(this, SlotSelectionActivity::class.java)
                i.putExtra(EXTRA_GAME, game)
                i.putExtra(EXTRA_BASE_DIRECTORY, baseDir)
                i.putExtra(
                    EXTRA_DIALOG_TYPE_INT,
                    DIALOAG_TYPE_LOAD
                )
                freeStartActivityForResult(this, i, REQUEST_LOAD)
            } else if (item.id == R.string.game_menu_cheats) {
                val i = Intent(this, CheatsActivity::class.java)
                i.putExtra(CheatsActivity.EXTRA_IN_GAME_HASH, game.checksum)
                freeStartActivity(this, i)
            } else if (item.id == R.string.game_menu_settings) {
                val i = Intent(this, GamePreferenceActivity::class.java)
                i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                i.putExtra(
                    PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    GamePreferenceFragment::class.java.name
                )
                i.putExtra(GamePreferenceActivity.EXTRA_GAME, game)
                startActivity(i)
            } else if (item.id == R.string.gallery_menu_pref) {
                val i = Intent(this, GeneralPreferenceActivity::class.java)
                i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                i.putExtra(
                    PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    GeneralPreferenceFragment::class.java.name
                )
                startActivity(i)
            } else if (item.id == R.string.game_menu_screenshot) {
                saveScreenshotWithPermission()
            }
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    private fun onGameBackToPast() {
        if (gameManagerProxy.historyItemCount > 1) {
            dialog.setOnDismissListener { dialog: DialogInterface? ->
                runTimeMachine = false
                try {
                    gameManagerProxy.resumeEmulation()
                } catch (e: EmulatorException) {
                    handleException(e)
                }
            }
            show(dialog, true)
            runTimeMachine = true
        }
    }

    private fun saveScreenshotWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    saveGameScreenshot()
                }

                override fun onDenied() {}
            }).request()
    }

    private fun saveGameScreenshot() {
        val name = game.cleanName + "-screenshot"
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            info?.name?.replace(' ', '_')
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        var to = dir
        var counter = 0
        while (to.exists()) {
            val nn = name + (if (counter == 0) "" else "($counter)") + ".png"
            to = File(dir, nn)
            counter++
        }
        try {
            val fos = FileOutputStream(to)
            createScreenshotBitmap(this@EmulatorActivity, game)
                .compress(CompressFormat.PNG, 90, fos)
            fos.close()
            Toast.makeText(
                this@EmulatorActivity,
                getString(
                    R.string.act_game_screenshot_saved,
                    to.absolutePath
                ), Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            e(TAG, "", e)
            throw EmulatorException(getString(R.string.act_game_screenshot_failed))
        }
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

            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_3D_MODE, KeyEvent.KEYCODE_APP_SWITCH -> super.onKeyUp(
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
                openGameMenu()
                true
            }

            KeyEvent.KEYCODE_BACK -> {
                if (event.isAltPressed) {
                    true
                } else super.onKeyDown(keyCode, event)
            }

            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_MUTE, KeyEvent.KEYCODE_3D_MODE, KeyEvent.KEYCODE_APP_SWITCH -> super.onKeyDown(
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
        private const val TAG = "EmulatorActivity"
        private const val OPEN_GL_BENCHMARK = "openGL"
        const val EMULATION_BENCHMARK = "emulation"
        private const val REQUEST_SAVE = 1
        private const val REQUEST_LOAD = 2
        var pm: PackageManager? = null
        var pn: String? = null
        var sd: String? = null
        var oldConfig = 0
    }
}