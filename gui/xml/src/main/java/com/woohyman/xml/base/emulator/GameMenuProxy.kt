package com.woohyman.xml.base.emulator

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.preference.PreferenceActivity
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.EmulatorHolder
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.keyboard.utils.DialogUtils
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.BaseApplication
import com.woohyman.xml.R
import com.woohyman.xml.ui.cheats.CheatsActivity
import com.woohyman.xml.ui.gamegallery.Constants
import com.woohyman.xml.ui.gamegallery.SlotSelectionActivity
import com.woohyman.xml.ui.menu.GameMenu
import com.woohyman.xml.ui.preferences.GamePreferenceActivity
import com.woohyman.xml.ui.preferences.GamePreferenceFragment
import com.woohyman.xml.ui.preferences.GeneralPreferenceActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceFragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GameMenuProxy constructor(
    private val emulatorMediator: EmulatorMediator,
) : GameMenu.OnGameMenuListener, DefaultLifecycleObserver {
    var runTimeMachine = false

    val gameMenu: GameMenu by lazy {
        GameMenu(Utils.getApp(), this)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        if (gameMenu.isOpen) {
            gameMenu.dismiss()
        }
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
        val ea = Utils.getApp() as BaseApplication
        val settingsStringRes =
            if (ea.hasGameMenu()) R.string.game_menu_settings else R.string.gallery_menu_pref
        menu.add(settingsStringRes, R.drawable.ic_game_settings)
    }

    override fun onGameMenuPrepare(menu: GameMenu) {
        val backToPast = menu.getItem(R.string.game_menu_back_to_past)
        backToPast?.enable = PreferenceUtil.isTimeshiftEnabled(Utils.getApp())
        NLog.i(EmulatorActivity.TAG, "prepare menu")
    }

    override fun onGameMenuClosed(menu: GameMenu) {
        if (runTimeMachine || menu.isOpen) {
            return
        }
        try {
            emulatorMediator.emulatorManagerProxy.resumeEmulation()
            emulatorMediator.gameControlProxy.onGameStarted(emulatorMediator.game)
            emulatorMediator.gameControlProxy.onResume()
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
        }
    }

    override fun onGameMenuItemSelected(menu: GameMenu?, item: GameMenu.GameMenuItem) {
        try {
            when (item.id) {
                R.string.game_menu_back_to_past -> {
                    onGameBackToPast()
                }

                R.string.game_menu_reset -> {
                    emulatorMediator.emulatorManagerProxy.resetEmulator()
                    emulatorMediator.emulatorManagerProxy.enableCheats()
                }

                R.string.game_menu_save -> {
                    val i = Intent(emulatorMediator.activity, SlotSelectionActivity::class.java)
                    i.putExtra(EmulatorActivity.EXTRA_GAME, emulatorMediator.game)
                    i.putExtra(Constants.EXTRA_BASE_DIRECTORY, emulatorMediator.baseDir)
                    i.putExtra(
                        Constants.EXTRA_DIALOG_TYPE_INT,
                        Constants.DIALOAG_TYPE_SAVE
                    )
                    freeStartActivityForResult(i, EmulatorActivity.REQUEST_SAVE)
                }

                R.string.game_menu_load -> {
                    val i = Intent(emulatorMediator.activity, SlotSelectionActivity::class.java)
                    i.putExtra(EmulatorActivity.EXTRA_GAME, emulatorMediator.game)
                    i.putExtra(Constants.EXTRA_BASE_DIRECTORY, emulatorMediator.baseDir)
                    i.putExtra(
                        Constants.EXTRA_DIALOG_TYPE_INT,
                        Constants.DIALOAG_TYPE_LOAD
                    )
                    freeStartActivityForResult(i, EmulatorActivity.REQUEST_LOAD)
                }

                R.string.game_menu_cheats -> {
                    val i = Intent(emulatorMediator.activity, CheatsActivity::class.java)
                    i.putExtra(CheatsActivity.EXTRA_IN_GAME_HASH, emulatorMediator.game.checksum)
                    freeStartActivity(i)
                }

                R.string.game_menu_settings -> {
                    val i = Intent(emulatorMediator.activity, GamePreferenceActivity::class.java)
                    i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                    i.putExtra(
                        PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        GamePreferenceFragment::class.java.name
                    )
                    i.putExtra(GamePreferenceActivity.EXTRA_GAME, emulatorMediator.game)
                    emulatorMediator.activity?.startActivity(i)
                }

                R.string.gallery_menu_pref -> {
                    val i = Intent(emulatorMediator.activity, GeneralPreferenceActivity::class.java)
                    i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                    i.putExtra(
                        PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                        GeneralPreferenceFragment::class.java.name
                    )
                    emulatorMediator.activity?.startActivity(i)
                }

                R.string.game_menu_screenshot -> {
                    saveScreenshotWithPermission()
                }
            }
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
        }
    }

    override fun onGameMenuOpened(menu: GameMenu?) {
        NLog.i(EmulatorActivity.TAG, "on game menu open")
        try {
            emulatorMediator.emulatorManagerProxy.pauseEmulation()
            emulatorMediator.gameControlProxy.onGamePaused(emulatorMediator.game)
            emulatorMediator.gameControlProxy.onPause()
        } catch (e: EmulatorException) {
            emulatorMediator.handleException(e)
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
        val name = emulatorMediator.game.cleanName + "-screenshot"
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            EmulatorHolder.info?.name?.replace(' ', '_').toString()
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
            EmuUtils.createScreenshotBitmap(emulatorMediator.activity, emulatorMediator.game)
                .compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.close()
            Toast.makeText(
                emulatorMediator.activity,
                Utils.getApp().getString(
                    R.string.act_game_screenshot_saved,
                    to.absolutePath
                ), Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            NLog.e(EmulatorActivity.TAG, "", e)
            throw EmulatorException(Utils.getApp().getString(R.string.act_game_screenshot_failed))
        }
    }

    private fun freeStartActivityForResult(intent: Intent, requestCode: Int) {
        emulatorMediator.setShouldPauseOnResume(false)
        emulatorMediator.activity?.startActivityForResult(intent, requestCode)
    }

    private fun freeStartActivity(intent: Intent) {
        emulatorMediator.setShouldPauseOnResume(false)
        emulatorMediator.activity?.startActivity(intent)
    }

    private fun onGameBackToPast() {
        if (emulatorMediator.emulatorManagerProxy.historyItemCount > 1) {
            emulatorMediator.dialog.setOnDismissListener { dialog: DialogInterface? ->
                runTimeMachine = false
                try {
                    emulatorMediator.emulatorManagerProxy.resumeEmulation()
                } catch (e: EmulatorException) {
                    emulatorMediator.handleException(e)
                }
            }
            DialogUtils.show(emulatorMediator.dialog, true)
            runTimeMachine = true
        }
    }
}