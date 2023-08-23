package com.woohyman.xml.ui.gamegallery

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.viewpager.widget.ViewPager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.woohyman.xml.R
import com.woohyman.xml.base.EmulatorActivity
import com.woohyman.xml.ui.gamegallery.adapter.GalleryPagerAdapter
import com.woohyman.xml.ui.preferences.GeneralPreferenceActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceFragment
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.DialogUtils.show
import com.woohyman.keyboard.utils.EmuUtils.extractFile
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.NLog.w
import com.woohyman.keyboard.utils.PreferenceUtil.getLastGalleryTab
import com.woohyman.keyboard.utils.PreferenceUtil.saveLastGalleryTab
import com.woohyman.keyboard.utils.ZipRomFile
import java.io.File
import java.io.IOException
import java.text.NumberFormat

abstract class GalleryActivity : BaseGameGalleryActivity(),
    GalleryPagerAdapter.OnItemClickListener {
    var searchDialog: ProgressDialog? = null
    private var pager: ViewPager? = null
    private var dbHelper: DatabaseHelper? = null
    private var adapter: GalleryPagerAdapter? = null
    private val importing = false
    private var rotateAnim = false
    private var mTabLayout: TabLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)
        setContentView(R.layout.activity_gallery)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        adapter = GalleryPagerAdapter(this, this)
        adapter!!.onRestoreInstanceState(savedInstanceState)
        pager = findViewById(R.id.game_gallery_pager)
        pager?.setAdapter(adapter)
        mTabLayout = findViewById(R.id.game_gallery_tab)
        mTabLayout?.setupWithViewPager(pager)
        mTabLayout?.setTabMode(TabLayout.MODE_FIXED)
        if (savedInstanceState != null) {
            pager?.setCurrentItem(savedInstanceState.getInt(EXTRA_TABS_IDX, 0))
        } else {
            pager?.setCurrentItem(getLastGalleryTab(this))
        }
        exts = romExtensions
        exts = exts?.plus(archiveExtensions)
        inZipExts = romExtensions
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gallery_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.gallery_menu_pref) {
            val i = Intent(this, GeneralPreferenceActivity::class.java)
            i.putExtra(
                PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                GeneralPreferenceFragment::class.java.name
            )
            i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
            startActivity(i)
            return true
        } else if (itemId == R.id.gallery_menu_reload) {
            reloadGames(true, null)
            return true
        } else if (itemId == R.id.gallery_menu_exit) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (rotateAnim) {
            rotateAnim = false
        }
        adapter!!.notifyDataSetChanged()
        if (reloadGames && !importing) {
            val isDBEmpty = dbHelper!!.countObjsInDb(GameDescription::class.java, null) == 0
            reloadGames(isDBEmpty, null)
        }
    }

    override fun onPause() {
        super.onPause()
        saveLastGalleryTab(this, pager!!.currentItem)
    }

    override fun onItemClick(game: GameDescription?) {
        var gameFile = File(game!!.path)
        i(TAG, "select $game")
        if (game.isInArchive) {
            gameFile = File(externalCacheDir, game.checksum)
            game.path = gameFile.absolutePath
            val zipRomFile = dbHelper!!.selectObjFromDb(
                ZipRomFile::class.java,
                "WHERE _id=" + game.zipfile_id, false
            )
            val zipFile = File(zipRomFile.path)
            if (!gameFile.exists()) {
                try {
                    extractFile(zipFile, game.name, gameFile)
                } catch (e: IOException) {
                    e(TAG, "", e)
                }
            }
        }
        if (gameFile.exists()) {
            game.lastGameTime = System.currentTimeMillis()
            game.runCount++
            dbHelper!!.updateObjToDb(game, arrayOf("lastGameTime", "runCount"))
            onGameSelected(game, 0)
        } else {
            w(TAG, "rom file:" + gameFile.absolutePath + " does not exist")
            val dialog = AlertDialog.Builder(this)
                .setMessage(getString(R.string.gallery_rom_not_found))
                .setTitle(R.string.error)
                .setPositiveButton(R.string.gallery_rom_not_found_reload) { dialog1: DialogInterface?, which: Int ->
                    reloadGames(
                        true,
                        null
                    )
                }
                .setCancelable(false)
                .create()
            dialog.setOnDismissListener { dialog12: DialogInterface? -> reloadGames(true, null) }
            dialog.show()
        }
    }

    fun onGameSelected(game: GameDescription?, slot: Int): Boolean {
        val intent = Intent(this, emulatorActivityClass)
        intent.putExtra(EmulatorActivity.EXTRA_GAME, game)
        intent.putExtra(EmulatorActivity.EXTRA_SLOT, slot)
        intent.putExtra(EmulatorActivity.EXTRA_FROM_GALLERY, true)
        startActivity(intent)
        return true
    }

    override fun setLastGames(games: ArrayList<GameDescription>?) {
        adapter!!.setGames(games)
    }

    override fun setNewGames(games: ArrayList<GameDescription>?) {
        adapter!!.addGames(games)
    }

    private fun showSearchProgressDialog(zipMode: Boolean) {
        if (searchDialog == null) {
            searchDialog = ProgressDialog(this)
            searchDialog!!.max = 100
            searchDialog!!.setCancelable(false)
            searchDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            searchDialog!!.isIndeterminate = true
            searchDialog!!.setProgressNumberFormat("")
            searchDialog!!.setProgressPercentFormat(null)
            searchDialog!!.setButton(
                DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel)
            ) { dialog: DialogInterface?, which: Int -> stopRomsFinding() }
        }
        searchDialog!!.setMessage(getString(if (zipMode) R.string.gallery_zip_search_label else R.string.gallery_sdcard_search_label))
        show(searchDialog!!, false)
    }

    fun onSearchingEnd(count: Int, showToast: Boolean) {
        runOnUiThread {
            if (searchDialog != null) {
                searchDialog!!.dismiss()
                searchDialog = null
            }
            if (showToast) {
                if (count > 0) {
                    Snackbar.make(
                        pager!!, getString(R.string.gallery_count_of_found_games, count),
                        Snackbar.LENGTH_LONG
                    ).setAction("Action", null).show()
                }
            }
        }
    }

    override fun onRomsFinderStart(searchNew: Boolean) {
        if (searchNew) {
            showSearchProgressDialog(false)
        }
    }

    override fun onRomsFinderZipPartStart(countEntries: Int) {
        if (searchDialog != null) {
            runOnUiThread {
                if (searchDialog != null) {
                    searchDialog!!.setProgressNumberFormat("%1d/%2d")
                    searchDialog!!.setProgressPercentFormat(NumberFormat.getPercentInstance())
                    searchDialog!!.setMessage(getString(R.string.gallery_start_sip_search_label))
                    searchDialog!!.isIndeterminate = false
                    searchDialog!!.max = countEntries
                }
            }
        }
    }

    override fun onRomsFinderCancel(searchNew: Boolean) {
        super.onRomsFinderCancel(searchNew)
        onSearchingEnd(0, searchNew)
    }

    override fun onRomsFinderEnd(searchNew: Boolean) {
        super.onRomsFinderEnd(searchNew)
        onSearchingEnd(0, searchNew)
    }

    override fun onRomsFinderNewGames(roms: ArrayList<GameDescription>) {
        super.onRomsFinderNewGames(roms)
        onSearchingEnd(roms.size, true)
    }

    override fun onRomsFinderFoundZipEntry(message: String?, skipEntries: Int) {
        if (searchDialog != null) {
            runOnUiThread {
                if (searchDialog != null) {
                    searchDialog!!.setMessage(message)
                    searchDialog!!.progress = searchDialog!!.progress + 1 + skipEntries
                }
            }
        }
    }

    override fun onRomsFinderFoundFile(name: String?) {
        if (searchDialog != null) {
            runOnUiThread {
                if (searchDialog != null) {
                    searchDialog!!.setMessage(name)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_TABS_IDX, pager!!.currentItem)
        adapter!!.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_TABS_IDX = "EXTRA_TABS_IDX"
        private val TAG = GalleryActivity::class.java.simpleName
    }
}