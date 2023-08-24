package com.woohyman.xml.ui.preferences

import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.xml.R
import com.woohyman.xml.base.GameMenu
import com.woohyman.xml.base.GameMenu.GameMenuItem
import com.woohyman.xml.base.GameMenu.OnGameMenuListener
import com.woohyman.xml.ui.multitouchbutton.MultitouchLayer
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.PreferenceUtil.getLastGfxProfile

class TouchControllerSettingsActivity : AppCompatActivity(), OnGameMenuListener {
    var mtLayer: MultitouchLayer? = null
    var gameHash = ""
    var dbHelper: DatabaseHelper? = null
    var lastGameScreenshot: Bitmap? = null
    private var gameMenu: GameMenu? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.controler_layout)
        gameMenu = GameMenu(this, this)
        mtLayer = findViewById(R.id.touch_layer)
        dbHelper = DatabaseHelper(this)
    }

    override fun onResume() {
        super.onResume()
        mtLayer!!.editMode = MultitouchLayer.EDIT_MODE.TOUCH
        val games = dbHelper!!.selectObjFromDb(
            GameDescription::class.java,
            "where lastGameTime!=0 ORDER BY lastGameTime DESC LIMIT 1"
        )
        val gfxProfile: GfxProfile?
        lastGameScreenshot = null
        if (games != null) {
            val (_, _, _, screenShot) = SlotUtils.getSlot(
                EmulatorUtils.getBaseDir(this),
                games.checksum, 0
            )
            lastGameScreenshot = screenShot
        }
        gfxProfile = getLastGfxProfile(this)
        mtLayer!!.setLastgameScreenshot(
            lastGameScreenshot,
            gfxProfile?.name!!
        )
    }

    override fun onPause() {
        super.onPause()
        mtLayer!!.saveEditElements()
        mtLayer!!.stopEditMode()
        if (lastGameScreenshot != null) {
            lastGameScreenshot!!.recycle()
            lastGameScreenshot = null
        }
    }

    override fun onGameMenuCreate(menu: GameMenu) {
        menu.add(R.string.act_tcs_reset, R.drawable.ic_restart)
    }

    override fun onGameMenuPrepare(menu: GameMenu) {}
    override fun onGameMenuOpened(menu: GameMenu?) {

    }

    override fun onGameMenuClosed(menu: GameMenu) {}
    override fun onGameMenuItemSelected(menu: GameMenu?, item: GameMenuItem) {
        runOnUiThread { mtLayer!!.resetEditElement(gameHash) }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_MENU) {
            openGameMenu()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    fun openGameMenu() {
        gameMenu!!.open()
    }

    override fun openOptionsMenu() {
        gameMenu!!.open()
    }
}