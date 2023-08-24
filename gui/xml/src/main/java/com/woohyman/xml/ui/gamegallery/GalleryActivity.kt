package com.woohyman.xml.ui.gamegallery

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MainThread
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.keyboard.rom.RomLauncher
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.DialogUtils.show
import com.woohyman.keyboard.utils.EmuUtils.extractFile
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.NLog.w
import com.woohyman.keyboard.utils.PreferenceUtil.getLastGalleryTab
import com.woohyman.keyboard.utils.PreferenceUtil.saveLastGalleryTab
import com.woohyman.keyboard.utils.ZipRomFile
import com.woohyman.xml.R
import com.woohyman.xml.base.EmulatorActivity
import com.woohyman.xml.databinding.ActivityGalleryBinding
import com.woohyman.xml.ui.gamegallery.adapter.GalleryPagerAdapter
import com.woohyman.xml.ui.gamegallery.base.BaseGameGalleryActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceActivity
import com.woohyman.xml.ui.preferences.GeneralPreferenceFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import javax.inject.Inject

abstract class GalleryActivity :
    BaseGameGalleryActivity<ActivityGalleryBinding>(ActivityGalleryBinding::inflate) {

    @Inject
    lateinit var romLauncher: IRomLauncher

    private var searchDialog: ProgressDialog? = null
    private val adapter by lazy {
        GalleryPagerAdapter(this)
    }
    private val importing = false
    private var rotateAnim = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        binding.gameGalleryPager.adapter = adapter
        binding.gameGalleryTab.setupWithViewPager(binding.gameGalleryPager)
        binding.gameGalleryTab.tabMode = TabLayout.MODE_FIXED
        binding.gameGalleryPager.currentItem =
            savedInstanceState?.getInt(EXTRA_TABS_IDX, 0) ?: getLastGalleryTab(this)
        exts = romExtensions
        exts = exts?.plus(archiveExtensions)
        inZipExts = romExtensions

        lifecycleScope.launch {
            romLauncher.romLauncherState.collect{

                it.onSuccess {game ->
                    onGameSelected(game, 0)
                }

                it.onFailure {
                    val dialog = AlertDialog.Builder(this@GalleryActivity)
                        .setMessage(getString(R.string.gallery_rom_not_found))
                        .setTitle(R.string.error)
                        .setPositiveButton(R.string.gallery_rom_not_found_reload) { dialog1: DialogInterface?, which: Int ->
                            reloadGames(true, null)
                        }
                        .setCancelable(false)
                        .create()
                    dialog.setOnDismissListener { dialog12: DialogInterface? -> reloadGames(true, null) }
                    dialog.show()
                }
            }
        }
    }

//    override fun onCreateOptionsMenu(menu: Menu): Boolean {
//        menuInflater.inflate(R.menu.gallery_main_menu, menu)
//        return super.onCreateOptionsMenu(menu)
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.gallery_menu_pref -> {
                val i = Intent(this, GeneralPreferenceActivity::class.java)
                i.putExtra(
                    PreferenceActivity.EXTRA_SHOW_FRAGMENT,
                    GeneralPreferenceFragment::class.java.name
                )
                i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true)
                startActivity(i)
                return true
            }

            R.id.gallery_menu_reload -> {
                reloadGames(true, null)
                return true
            }

            R.id.gallery_menu_exit -> {
                finish()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        if (rotateAnim) {
            rotateAnim = false
        }
        adapter.notifyDataSetChanged()
        if (reloadGames && !importing) {
            reloadGames(romLauncher.isDBEmpty, null)
        }
    }

    override fun onPause() {
        super.onPause()
        saveLastGalleryTab(this, binding.gameGalleryPager.currentItem)
    }

    private fun onGameSelected(game: GameDescription?, slot: Int): Boolean {
        val intent = Intent(this, emulatorActivityClass)
        intent.putExtra(EmulatorActivity.EXTRA_GAME, game)
        intent.putExtra(EmulatorActivity.EXTRA_SLOT, slot)
        intent.putExtra(EmulatorActivity.EXTRA_FROM_GALLERY, true)
        startActivity(intent)
        return true
    }

    override fun setLastGames(games: ArrayList<GameDescription>) {
        adapter.setGames(games)
    }

    override fun setNewGames(games: ArrayList<GameDescription>) {
        adapter.addGames(games)
    }

    @MainThread
    private fun showSearchProgressDialog(zipMode: Boolean) {
        searchDialog ?: ProgressDialog(this).also {
            it.max = 100
            it.setCancelable(false)
            it.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            it.isIndeterminate = true
            it.setProgressNumberFormat("")
            it.setProgressPercentFormat(null)
            it.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.cancel)
            ) { dialog: DialogInterface?, which: Int -> stopRomsFinding() }
        }.also {
            it.setMessage(getString(if (zipMode) R.string.gallery_zip_search_label else R.string.gallery_sdcard_search_label))
            show(it, false)
        }

    }

    @MainThread
    private fun onSearchingEnd(count: Int, showToast: Boolean) {
        if (searchDialog != null) {
            searchDialog!!.dismiss()
            searchDialog = null
        }
        if (showToast) {
            if (count > 0) {
                Snackbar.make(
                    binding.gameGalleryPager,
                    getString(R.string.gallery_count_of_found_games, count),
                    Snackbar.LENGTH_LONG
                ).setAction("Action", null).show()
            }
        }
    }

    override fun onRomsFinderStart(searchNew: Boolean) {
        if (searchNew) {
            showSearchProgressDialog(false)
        }
    }

    @MainThread
    override fun onRomsFinderZipPartStart(countEntries: Int) {
        searchDialog?.setProgressNumberFormat("%1d/%2d")
        searchDialog?.setProgressPercentFormat(NumberFormat.getPercentInstance())
        searchDialog?.setMessage(getString(R.string.gallery_start_sip_search_label))
        searchDialog?.isIndeterminate = false
        searchDialog?.max = countEntries
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

    @MainThread
    override fun onRomsFinderFoundZipEntry(message: String?, skipEntries: Int) {
        searchDialog?.setMessage(message)
        searchDialog?.progress = searchDialog!!.progress + 1 + skipEntries
    }

    @MainThread
    override fun onRomsFinderFoundFile(name: String?) {
        searchDialog?.setMessage(name)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_TABS_IDX, binding.gameGalleryPager.currentItem)
        adapter.onSaveInstanceState(outState)
    }

    companion object {
        const val EXTRA_TABS_IDX = "EXTRA_TABS_IDX"
        private val TAG = GalleryActivity::class.java.simpleName
    }
}