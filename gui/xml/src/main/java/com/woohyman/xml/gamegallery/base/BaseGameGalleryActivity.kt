package com.woohyman.xml.gamegallery.base

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import androidx.annotation.MainThread
import androidx.documentfile.provider.DocumentFile
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.rom.RomsFinder
import com.woohyman.keyboard.rom.RomsFinder.OnRomsFinderListener
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.DialogUtils.show
import com.woohyman.keyboard.utils.FileUtils.isSDCardRWMounted
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.xml.R
import com.woohyman.xml.base.BaseActivity
import com.woohyman.xml.emulator.EmulatorActivity
import com.woohyman.xml.permission.FileAccessPermissionDialogFragment

abstract class BaseGameGalleryActivity<VB : ViewBinding>(bindingFactory: (LayoutInflater) -> VB) :
    BaseActivity<VB>(bindingFactory), OnRomsFinderListener {
    protected var exts: Set<String>? = null
    protected var inZipExts: Set<String>? = null
    protected var reloadGames = true
    protected var reloading = false
    private var romsFinder: RomsFinder? = null
    private var dbHelper: DatabaseHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val exts = HashSet(romExtensions)
        exts.addAll(archiveExtensions)
        dbHelper = DatabaseHelper(this)
        val pref = getSharedPreferences("android50comp", MODE_PRIVATE)
        val androidVersion = Build.VERSION.RELEASE
        if (pref.getString("androidVersion", "") != androidVersion) {
            val db = dbHelper!!.writableDatabase
            dbHelper!!.onUpgrade(db, Int.MAX_VALUE - 1, Int.MAX_VALUE)
            db.close()
            val editor = pref.edit()
            editor.putString("androidVersion", androidVersion)
            editor.apply()
            i(TAG, "Reinit DB $androidVersion")
        }
        reloadGames = true
    }

    override fun onStart() {
        super.onStart()
        if(requestCode  == FileAccessPermissionDialogFragment.GET_ALL_FILE_ACCESS && FileAccessPermissionDialogFragment.isStorageAccess){
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivityForResult(intent, FileAccessPermissionDialogFragment.GET_ROM_PATH_ACCESS)
            requestCode = null
        }
    }

    private var requestCode:Int? = null
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
        this.requestCode = requestCode
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        NLog.e("test111","isStorageAccess = "+FileAccessPermissionDialogFragment.isStorageAccess+",requestCode = "+requestCode)
        if (resultCode == RESULT_OK && requestCode == FileAccessPermissionDialogFragment.GET_ROM_PATH_ACCESS) {
            val dfile = DocumentFile.fromTreeUri(Utils.getApp(), data!!.data!!)
            val file = UriUtils.uri2File(dfile!!.uri)

            NLog.e("test999","file ===> "+file)

            val editor = Utils.getApp().getSharedPreferences("user", MODE_PRIVATE).edit()
            editor?.putString("path", file.absolutePath)
            editor?.commit()

            reloadGames(true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isSDCardRWMounted) {
            showSDCardFailed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        romsFinder?.stopSearch()
    }

    protected fun reloadGames(searchNew: Boolean) {
        if (FileAccessPermissionDialogFragment.romPathFile == null) {
            return
        }
        romsFinder = RomsFinder(
            exts,
            inZipExts,
            { showSDCardFailed() },
            this,
            searchNew,
            FileAccessPermissionDialogFragment.romPathFile
        )
        reloadGames = false
        romsFinder?.start()
    }

    override fun onRomsFinderFoundGamesInCache(oldRoms: ArrayList<GameDescription>) {
        NLog.e("test333","onRomsFinderFoundGamesInCache "+oldRoms.size)
        setLastGames(oldRoms)
    }

    override fun onRomsFinderNewGames(roms: ArrayList<GameDescription>) {
        NLog.e("test333","onRomsFinderNewGames "+roms.size)
        setNewGames(roms)
    }

    override fun onRomsFinderEnd(searchNew: Boolean) {
        romsFinder = null
        reloading = false
    }

    override fun onRomsFinderCancel(searchNew: Boolean) {
        romsFinder = null
        reloading = false
    }

    protected fun stopRomsFinding() {
        romsFinder?.stopSearch()
    }

    @MainThread
    private fun showSDCardFailed() {
        val dialog = AlertDialog.Builder(this@BaseGameGalleryActivity)
            .setTitle(R.string.error)
            .setMessage(R.string.gallery_sd_card_not_mounted)
            .setOnCancelListener { dialog1: DialogInterface? -> finish() }
            .setPositiveButton(R.string.exit) { dialog1: DialogInterface?, which: Int -> finish() }
            .create()
        show(dialog, true)
    }

    abstract val emulatorActivityClass: Class<out EmulatorActivity?>?
    abstract fun setLastGames(games: ArrayList<GameDescription>)
    abstract fun setNewGames(games: ArrayList<GameDescription>)
    protected abstract val romExtensions: Set<String>?
    abstract val emulatorInstance: Emulator?
    protected val archiveExtensions: Set<String>
        get() {
            val set = HashSet<String>()
            set.add("zip")
            return set
        }

    companion object {
        private const val TAG = "BaseGameGalleryActivity"
    }
}