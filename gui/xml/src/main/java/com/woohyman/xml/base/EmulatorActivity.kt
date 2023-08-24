package com.woohyman.xml.base

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
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.woohyman.xml.BaseApplication
import com.woohyman.xml.R
import com.woohyman.xml.base.GameMenu.GameMenuItem
import com.woohyman.xml.base.GameMenu.OnGameMenuListener
import com.woohyman.xml.controllers.DynamicDPad
import com.woohyman.xml.controllers.KeyboardController
import com.woohyman.xml.controllers.QuickSaveController
import com.woohyman.xml.controllers.TouchController
import com.woohyman.xml.controllers.ZapperGun
import com.woohyman.xml.ui.cheats.CheatsActivity
import com.woohyman.xml.ui.gamegallery.SlotSelectionActivity
import com.woohyman.xml.ui.preferences.GamePreferenceActivity
import com.woohyman.xml.ui.preferences.GamePreferenceFragment
import com.woohyman.xml.ui.preferences.GeneralPreferenceActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceFragment
import com.woohyman.xml.ui.timetravel.TimeTravelDialog
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.Benchmark.BenchmarkCallback
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.emulator.EmulatorRunner.OnNotRespondingListener
import com.woohyman.keyboard.utils.DialogUtils.show
import com.woohyman.keyboard.utils.EmuUtils.checkGL20Support
import com.woohyman.keyboard.utils.EmuUtils.createScreenshotBitmap
import com.woohyman.keyboard.utils.EmuUtils.getDisplayHeight
import com.woohyman.keyboard.utils.EmuUtils.getDisplayWidth
import com.woohyman.keyboard.utils.NLog.d
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.PreferenceUtil.ROTATION
import com.woohyman.keyboard.utils.PreferenceUtil.getDisplayRotation
import com.woohyman.keyboard.utils.PreferenceUtil.getEmulationQuality
import com.woohyman.keyboard.utils.PreferenceUtil.getFastForwardFrameCount
import com.woohyman.keyboard.utils.PreferenceUtil.isAutoHideControls
import com.woohyman.keyboard.utils.PreferenceUtil.isBenchmarked
import com.woohyman.keyboard.utils.PreferenceUtil.isDynamicDPADEnable
import com.woohyman.keyboard.utils.PreferenceUtil.isFastForwardEnabled
import com.woohyman.keyboard.utils.PreferenceUtil.isFastForwardToggleable
import com.woohyman.keyboard.utils.PreferenceUtil.isScreenSettingsSaved
import com.woohyman.keyboard.utils.PreferenceUtil.isTimeshiftEnabled
import com.woohyman.keyboard.utils.PreferenceUtil.setBenchmarked
import com.woohyman.keyboard.utils.PreferenceUtil.setDynamicDPADUsed
import com.woohyman.keyboard.utils.PreferenceUtil.setEmulationQuality
import com.woohyman.keyboard.utils.PreferenceUtil.setFastForwardUsed
import com.woohyman.keyboard.utils.PreferenceUtil.setScreenLayoutUsed
import com.woohyman.xml.ui.gamegallery.Constants.DIALOAG_TYPE_LOAD
import com.woohyman.xml.ui.gamegallery.Constants.DIALOAG_TYPE_SAVE
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_BASE_DIRECTORY
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_DIALOG_TYPE_INT
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

abstract class EmulatorActivity : AppCompatActivity(), OnGameMenuListener, OnNotRespondingListener {
    private val maxPRC = 10
    var isRestarting = false
    var canRestart = false
    var runTimeMachine = false
    var dialog: TimeTravelDialog? = null
    private var gameMenu: GameMenu? = null
    private var game: GameDescription? = null
    private var dynamic: DynamicDPad? = null
    private var touchController: TouchController? = null
    private var autoHide = false
    private var warningShowing = false
    private var isFF = false
    private var isToggleFF = false
    private var isFFPressed = false
    private var exceptionOccurred = false
    private var slotToRun: Int? = null
    private var slotToSave: Int? = null
    private var controllers: MutableList<EmulatorController?>? = null
    var manager: Manager? = null
        private set
    private var emulatorView: EmulatorView? = null
    private val benchmarkCallback: BenchmarkCallback = object : BenchmarkCallback {
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
                    emulatorView!!.setQuality(2)
                    setEmulationQuality(this@EmulatorActivity, 2)
                } else {
                }
            }
        }
    }
    private var controllerViews: MutableList<View>? = null
    private var group: ViewGroup? = null
    private var baseDir: String? = null
    abstract val emulatorInstance: Emulator
    abstract val fragmentShader: String
    fun getTextureBounds(emulator: Emulator?): IntArray? {
        return null
    }

    val gLTextureSize: Int
        get() = 256

    fun hasGLPalette(): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        gameMenu = GameMenu(this, this)
        game = intent.getSerializableExtra(EXTRA_GAME) as GameDescription?
        slotToRun = -1
        val wParams = window.attributes
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        wParams.flags = wParams.flags or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        window.attributes = wParams
        val emulator = emulatorInstance
        val paddingLeft = 0
        var paddingTop = 0
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paddingTop = resources.getDimensionPixelSize(R.dimen.top_panel_touchcontroler_height)
        }
        val shader = fragmentShader
        var openGLView: OpenGLView? = null
        val quality = getEmulationQuality(this)
        val alreadyBenchmarked = isBenchmarked(this)
        val needsBenchmark = quality != 2 && !alreadyBenchmarked
        if (hasOpenGL20) {
            openGLView = OpenGLView(this, emulator, paddingLeft, paddingTop, shader)
            if (needsBenchmark) {
                openGLView.setBenchmark(Benchmark(OPEN_GL_BENCHMARK, 200, benchmarkCallback))
            }
        }
        emulatorView = openGLView ?: UnacceleratedView(this, emulator, paddingLeft, paddingTop)
        controllers = ArrayList()
        touchController = TouchController(this)
        controllers?.add(touchController)
        touchController!!.connectToEmulator(0, emulator)
        dynamic = DynamicDPad(this, windowManager.defaultDisplay, touchController)
        controllers?.add(dynamic)
        dynamic!!.connectToEmulator(0, emulator)
        val qsc = QuickSaveController(this, touchController)
        controllers?.add(qsc)
        val kc = KeyboardController(emulator, applicationContext, game!!.checksum, this)
        val zapper = ZapperGun(applicationContext, this)
        zapper.connectToEmulator(1, emulator)
        controllers?.add(zapper)
        controllers?.add(kc)
        group = FrameLayout(this)
        val display = windowManager.defaultDisplay
        val w = getDisplayWidth(display)
        val h = getDisplayHeight(display)
        val params = ViewGroup.LayoutParams(w, h)
        group?.setLayoutParams(params)
        group?.addView(emulatorView!!.asView())
        controllerViews = ArrayList()
        for (controller in controllers!!) {
            val controllerView = controller!!.view
            if (controllerView != null) {
                controllerViews?.add(controllerView)
                group?.addView(controllerView)
            }
        }
        group?.addView(object : View(applicationContext) {
            override fun onTouchEvent(event: MotionEvent): Boolean {
                return true
            }
        })
        setContentView(group)
        manager = Manager(emulator, applicationContext)
        manager!!.setOnNotRespondingListener(this)
        if (needsBenchmark) {
            manager!!.setBenchmark(Benchmark(EMULATION_BENCHMARK, 1000, benchmarkCallback))
        }
    }

    fun hideTouchController() {
        i(TAG, "hide controler")
        if (autoHide) {
            if (touchController != null) {
                touchController!!.hide()
            }
        }
    }

    override fun onNotResponding() {
        synchronized(warningShowing) {
            warningShowing = if (!warningShowing) {
                true
            } else {
                return
            }
        }
        runOnUiThread {
            val dialog = AlertDialog.Builder(this)
                .setMessage(R.string.too_slow)
                .create()
            dialog.setOnDismissListener { dialog1: DialogInterface? -> finish() }
            try {
                manager!!.pauseEmulation()
            } catch (ignored: EmulatorException) {
            }
            show(dialog, true)
        }
    }

    val viewPort: ViewPort?
        get() = emulatorView?.viewPort

    override fun onDestroy() {
        super.onDestroy()
        if (exceptionOccurred) {
            return
        }
        oldConfig = changingConfigurations
        group!!.removeAllViews()
        controllerViews!!.clear()
        try {
            manager!!.destroy()
        } catch (ignored: EmulatorException) {
        }
        for (controller in controllers!!) {
            controller!!.onDestroy()
        }
        controllers!!.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        setShouldPauseOnResume(false)
        if (resultCode == RESULT_OK) {
            canRestart = false
            val slotIdx = data!!.getIntExtra(EXTRA_SLOT, -1)
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
        if (touchController != null) {
            touchController!!.show()
        }
        for (controllerView in controllerViews!!) {
            controllerView.dispatchTouchEvent(ev)
        }
        return res
    }

    override fun dispatchKeyEvent(ev: KeyEvent): Boolean {
        val res = super.dispatchKeyEvent(ev)
        for (controllerView in controllerViews!!) {
            controllerView.dispatchKeyEvent(ev)
        }
        return res
    }

    fun setShouldPauseOnResume(b: Boolean) {
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
        if (gameMenu != null && gameMenu!!.isOpen) {
            gameMenu!!.dismiss()
        }
        if (dialog != null && dialog!!.isShowing) {
            dialog!!.dismiss()
        }
        for (controller in controllers!!) {
            controller!!.onPause()
            controller.onGamePaused(game)
        }
        try {
            manager!!.stopGame()
        } catch (e: EmulatorException) {
            handleException(e)
        } finally {
            emulatorView!!.onPause()
        }
    }

    fun onFastForwardDown() {
        if (isToggleFF) {
            if (!isFFPressed) {
                isFFPressed = true
                isFF = !isFF
                manager!!.setFastForwardEnabled(isFF)
            }
        } else {
            manager!!.setFastForwardEnabled(true)
        }
    }

    fun onFastForwardUp() {
        if (!isToggleFF) {
            manager!!.setFastForwardEnabled(false)
        }
        isFFPressed = false
    }

    private fun handleException(e: EmulatorException) {
        val dialog = AlertDialog.Builder(this)
            .setMessage(e.getMessage(this)).create()
        dialog.setOnDismissListener { dialog1: DialogInterface? -> runOnUiThread { finish() } }
        show(dialog, true)
    }

    private fun restartProcess(activityToStartClass: Class<*>) {
        isRestarting = true
        val intent = Intent(this, RestarterActivity::class.java)
        intent.putExtras(getIntent())
        intent.putExtra(RestarterActivity.Companion.EXTRA_PID, Process.myPid())
        val className = activityToStartClass.name
        intent.putExtra(RestarterActivity.Companion.EXTRA_CLASS, className)
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
        manager!!.saveState(10)
        runOnUiThread {
            Toast.makeText(
                this,
                "state saved", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun quickLoad() {
        manager!!.loadState(10)
    }

    @SuppressLint("DefaultLocale")
    override fun onResume() {
        super.onResume()
        isRestarting = false
        val extras = intent.extras
        var isAfterProcessRestart = false
        if (extras != null) {
            isAfterProcessRestart =
                extras.getBoolean(RestarterActivity.Companion.EXTRA_AFTER_RESTART)
        }
        intent.removeExtra(RestarterActivity.Companion.EXTRA_AFTER_RESTART)
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
        manager!!.setFastForwardFrameCount(getFastForwardFrameCount(this))
        if (isDynamicDPADEnable(this)) {
            if (!controllers!!.contains(dynamic)) {
                controllers!!.add(dynamic)
                controllerViews!!.add(dynamic!!.view)
            }
            setDynamicDPADUsed(this, true)
        } else {
            controllers!!.remove(dynamic)
            controllerViews!!.remove(dynamic!!.view)
        }
        if (isFastForwardEnabled(this)) {
            setFastForwardUsed(this, true)
        }
        if (isScreenSettingsSaved(this)) {
            setScreenLayoutUsed(this, true)
        }
        pm = packageManager
        pn = packageName
        for (controller in controllers!!) {
            controller!!.onResume()
        }
        try {
            manager!!.startGame(game!!)
            for (controller in controllers!!) {
                controller!!.onGameStarted(game)
            }
            if (slotToRun != -1) {
                manager!!.loadState(slotToRun!!)
            } else {
                if (SlotUtils.autoSaveExists(baseDir, game!!.checksum)) {
                    manager!!.loadState(0)
                }
            }
            if (slotToSave != null) {
                manager!!.copyAutoSave(slotToSave!!)
            }
            val wasRotated =
                oldConfig and ActivityInfo.CONFIG_ORIENTATION == ActivityInfo.CONFIG_ORIENTATION
            oldConfig = 0
            if (shouldPause() && !wasRotated) {
                gameMenu!!.open()
            }
            setShouldPauseOnResume(true)
            if (gameMenu != null && gameMenu!!.isOpen) {
                manager!!.pauseEmulation()
            }
            slotToRun = 0
            val quality = getEmulationQuality(this)
            emulatorView!!.setQuality(quality)
            emulatorView!!.onResume()
            enableCheats()
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    private fun enableCheats() {
        var numCheats = 0
        try {
            numCheats = manager!!.enableCheats(this, game!!)
        } catch (e: EmulatorException) {
            runOnUiThread {
                Toast.makeText(
                    this, e.getMessage(this),
                    Toast.LENGTH_SHORT
                ).show()
            }
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
        for (controller in controllers!!) {
            controller!!.onGameStarted(game)
        }
    }

    fun openGameMenu() {
//        gameMenu!!.open()
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
        backToPast!!.enable = isTimeshiftEnabled(this)
        i(TAG, "prepare menu")
    }

    override fun onGameMenuClosed(menu: GameMenu) {
        try {
            if (!runTimeMachine) {
                if (!menu.isOpen) {
                    manager!!.resumeEmulation()
                    for (controller in controllers!!) {
                        controller!!.onGameStarted(game)
                        controller.onResume()
                    }
                }
            }
        } catch (e: EmulatorException) {
            handleException(e)
        }
    }

    override fun onGameMenuOpened(menu: GameMenu?) {
        i(TAG, "on game menu open")
        try {
            if (manager != null) {
                manager!!.pauseEmulation()
                for (controller in controllers!!) {
                    controller!!.onGamePaused(game)
                    controller.onPause()
                }
            }
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
                manager!!.resetEmulator()
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
                i.putExtra(CheatsActivity.EXTRA_IN_GAME_HASH, game!!.checksum)
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
        if (manager!!.historyItemCount > 1) {
            dialog = TimeTravelDialog(this, manager!!, game!!)
            dialog!!.setOnDismissListener { dialog: DialogInterface? ->
                runTimeMachine = false
                try {
                    manager!!.resumeEmulation()
                } catch (e: EmulatorException) {
                    handleException(e)
                }
            }
            show(dialog!!, true)
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
        val name = game!!.cleanName + "-screenshot"
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            info!!.name!!.replace(' ', '_')
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
            createScreenshotBitmap(this@EmulatorActivity, game!!)
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
        private const val EMULATION_BENCHMARK = "emulation"
        private const val REQUEST_SAVE = 1
        private const val REQUEST_LOAD = 2
        var pm: PackageManager? = null
        var pn: String? = null
        var sd: String? = null
        private var oldConfig = 0
    }
}