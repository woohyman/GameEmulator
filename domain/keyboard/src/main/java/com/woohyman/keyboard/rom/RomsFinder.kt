package com.woohyman.keyboard.rom

import android.os.Environment
import android.os.Process
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.EmuUtils.getExt
import com.woohyman.keyboard.utils.EmuUtils.getMD5Checksum
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.SDCardUtil.allStorageLocations
import com.woohyman.keyboard.utils.ZipRomFile
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class RomsFinder(
    exts: Set<String>?,
    inZipExts: Set<String>?,
    private val SDCardFindFailed:()->Unit,
    private val listener: OnRomsFinderListener,
    searchNew: Boolean, selectedFolder: File?
) : Thread() {
    private val filenameExtFilter: FilenameExtFilter
    private val inZipFileNameExtFilter: FilenameExtFilter
    private var androidAppDataFolder = ""
    private val oldGames = HashMap<String, GameDescription?>()
    private val dbHelper: DatabaseHelper
    private var games = ArrayList<GameDescription>()
    private var searchNew = true
    private val selectedFolder: File?
    private val running = AtomicBoolean(false)

    init {
        this.searchNew = searchNew
        this.selectedFolder = selectedFolder
        filenameExtFilter = FilenameExtFilter(exts, true, false)
        inZipFileNameExtFilter = FilenameExtFilter(inZipExts, true, false)
        androidAppDataFolder = Environment.getExternalStorageDirectory().absolutePath + "/Android"
        dbHelper = DatabaseHelper(Utils.getApp())
    }

    private fun getRomAndPackedFiles(
        root: File,
        result: MutableList<File>,
        usedPaths: HashSet<String>
    ) {
        var dirPath: String? = null
        val dirStack = Stack<DirInfo>()
        dirStack.removeAllElements()
        dirStack.add(DirInfo(root, 0))
        val MAX_LEVEL = 12
        while (running.get() && !dirStack.empty()) {
            val dir = dirStack.removeAt(0)
            try {
                dirPath = dir.file.canonicalPath
            } catch (e1: IOException) {
                e(TAG, "search error", e1)
            }
            if (dirPath != null && !usedPaths.contains(dirPath) && dir.level <= MAX_LEVEL) {
                usedPaths.add(dirPath)
                val files = dir.file.listFiles(filenameExtFilter)
                if (files != null) {
                    for (file in files) {
                        if (file.isDirectory) {
                            var canonicalPath: String? = null
                            try {
                                canonicalPath = file.canonicalPath
                            } catch (e: IOException) {
                                e(TAG, "search error", e)
                            }
                            if (canonicalPath != null
                                && !usedPaths.contains(canonicalPath)
                            ) {
                                if (canonicalPath == androidAppDataFolder) {
                                    i(TAG, "ignore $androidAppDataFolder")
                                } else {
                                    dirStack.add(DirInfo(file, dir.level + 1))
                                }
                            } else {
                                i(TAG, "cesta $canonicalPath jiz byla prohledana")
                            }
                        } else {
                            result.add(file)
                        }
                    }
                }
            } else {
                i(TAG, "cesta $dirPath jiz byla prohledana")
            }
        }
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        running.set(true)
        i(TAG, "start")
        listener.onRomsFinderStart(searchNew)
        var oldRoms = getAllGames(dbHelper)
        oldRoms = removeNonExistRoms(oldRoms)
        val roms = oldRoms
        i(TAG, "old games " + oldRoms.size)
        listener.onRomsFinderFoundGamesInCache(roms)
        if (searchNew) {
            for (desc in oldRoms) {
                oldGames[desc.path] = desc
            }
            startFileSystemMode(oldRoms)
        } else {
            listener.onRomsFinderEnd(false)
        }
    }

    private fun checkZip(zipFile: File) {
        val externalCache = Utils.getApp().externalCacheDir
        if (externalCache != null) {
            val cacheDir = externalCache.absolutePath
            i(TAG, "check zip" + zipFile.absolutePath)
            val hash = ZipRomFile.computeZipHash(zipFile)
            var zipRomFile = dbHelper.selectObjFromDb(
                ZipRomFile::class.java,
                "WHERE hash=\"$hash\""
            )
            var zip: ZipFile? = null
            if (zipRomFile == null) {
                zipRomFile = ZipRomFile()
                zipRomFile.path = zipFile.absolutePath
                zipRomFile.hash = hash
                try {
                    var ze: ZipEntry
                    val dir = File(cacheDir)
                    var counterRoms = 0
                    var counterEntry = 0
                    zip = ZipFile(zipFile)
                    val max = zip.size()
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        ze = entries.nextElement()
                        counterEntry++
                        if (running.get() && !ze.isDirectory) {
                            val filename = ze.name
                            if (inZipFileNameExtFilter.accept(dir, filename)) {
                                counterRoms++
                                val `is` = zip.getInputStream(ze)
                                val checksum = getMD5Checksum(`is`!!)
                                try {
                                    if (`is` != null) {
                                        `is`.close()
                                    }
                                } catch (ignored: Exception) {
                                }
                                val game = GameDescription(ze.name, "", checksum)
                                game.inserTime = System.currentTimeMillis()
                                zipRomFile.games.add(game)
                                games.add(game)
                            }
                        }
                        if (counterEntry > 20 && counterRoms == 0) {
                            listener.onRomsFinderFoundZipEntry(
                                """
    ${zipFile.name}
    ${ze.name}
    """.trimIndent(),
                                max - 20 - 1
                            )
                            i(
                                TAG,
                                "Predcasne ukonceni prohledavani zipu. V prvnich 20 zaznamech v zipu neni ani jeden rom"
                            )
                            break
                        } else {
                            var name = ze.name
                            val idx = name.lastIndexOf('/')
                            if (idx != -1) {
                                name = name.substring(idx + 1)
                            }
                            if (name.length > 20) {
                                name = name.substring(0, 20)
                            }
                            listener.onRomsFinderFoundZipEntry(
                                """
    ${zipFile.name}
    $name
    """.trimIndent(), 0
                            )
                        }
                    }
                    if (running.get()) {
                        dbHelper.insertObjToDb(zipRomFile)
                    }
                } catch (e: Exception) {
                    e(TAG, "", e)
                } finally {
                    try {
                        zip?.close()
                    } catch (e: IOException) {
                        e(TAG, "", e)
                    }
                }
            } else {
                games.addAll(zipRomFile.games)
                listener.onRomsFinderFoundZipEntry(zipFile.name, zipRomFile.games.size)
                i(TAG, "found zip in cache " + zipRomFile.games.size)
            }
        } else {
            e(TAG, "external cache dir is null")
            SDCardFindFailed()
        }
    }

    private fun startFileSystemMode(oldRoms: ArrayList<GameDescription>) {
        var roots = HashSet<File>()
        if (selectedFolder == null) {
            roots = allStorageLocations
        } else {
            roots.add(selectedFolder)
        }
        val result = ArrayList<File>()
        val startTime = System.currentTimeMillis()
        i(TAG, "start searching in file system")
        val usedPaths = HashSet<String>()
        for (root in roots) {
            i(TAG, "exploring " + root.absolutePath)
            getRomAndPackedFiles(root, result, usedPaths)
        }
        i(TAG, "found " + result.size + " files")
        i(TAG, "compute checksum")
        var zipEntriesCount = 0
        val zips = ArrayList<File>()
        for (file in result) {
            val path = file.absolutePath
            if (running.get()) {
                val ext = getExt(path).lowercase(Locale.getDefault())
                if (ext == "zip") {
                    zips.add(file)
                    try {
                        val zzFile = ZipFile(file)
                        zipEntriesCount += zzFile.size()
                    } catch (e: Exception) {
                        e(TAG, "", e)
                    }
                    continue
                }
                var game: GameDescription
                if (oldGames.containsKey(path)) {
                    game = oldGames[path]!!
                } else {
                    game = GameDescription(file)
                    game.inserTime = System.currentTimeMillis()
                    dbHelper.insertObjToDb(game)
                    listener.onRomsFinderFoundFile(game.name)
                }
                games.add(game)
            }
        }
        for (zip in zips) {
            if (running.get()) {
                listener.onRomsFinderZipPartStart(zipEntriesCount)
                checkZip(zip)
            }
        }
        if (running.get()) {
            i(TAG, "found games: " + games.size)
            games = removeNonExistRoms(games)
        }
        i(TAG, "compute checksum- done")
        if (running.get()) {
            i(TAG, "onRomsFinderEnd- done")
            listener.onRomsFinderNewGames(games)
            listener.onRomsFinderEnd(true)
        }
        i(TAG, "time:" + (System.currentTimeMillis() - startTime) / 1000)
    }

    fun stopSearch() {
        if (running.get()) {
            listener.onRomsFinderCancel(true)
        }
        running.set(false)
        i(TAG, "cancel search")
    }

    private fun removeNonExistRoms(roms: ArrayList<GameDescription>): ArrayList<GameDescription> {
        val hashs = HashSet<String>()
        val newRoms = ArrayList<GameDescription>(roms.size)
        val zipsMap: MutableMap<Long, ZipRomFile> = HashMap()
        for (zip in dbHelper.selectObjsFromDb<ZipRomFile>(
            ZipRomFile::class.java,
            false, null, null
        )) {
            val zipFile = File(zip.path)
            if (zipFile.exists()) {
                zipsMap[zip._id] = zip
            } else {
                dbHelper.deleteObjFromDb(zip)
                dbHelper.deleteObjsFromDb(
                    GameDescription::class.java,
                    "where zipfile_id=" + zip._id
                )
            }
        }
        for (game in roms) {
            if (!game!!.isInArchive) {
                val path = File(game.path)
                if (path.exists()) {
                    if (!hashs.contains(game.checksum)) {
                        newRoms.add(game)
                        hashs.add(game.checksum)
                    }
                } else {
                    dbHelper.deleteObjFromDb(game)
                }
            } else {
                val zip = zipsMap[game.zipfile_id]
                if (zip != null) {
                    if (!hashs.contains(game.checksum)) {
                        newRoms.add(game)
                        hashs.add(game.checksum)
                    }
                }
            }
        }
        return newRoms
    }

    interface OnRomsFinderListener {
        fun onRomsFinderStart(searchNew: Boolean)
        fun onRomsFinderFoundGamesInCache(oldRoms: ArrayList<GameDescription>)
        fun onRomsFinderFoundFile(name: String?)
        fun onRomsFinderZipPartStart(countEntries: Int)
        fun onRomsFinderFoundZipEntry(message: String?, skipEntries: Int)
        fun onRomsFinderNewGames(roms: ArrayList<GameDescription>)
        fun onRomsFinderEnd(searchNew: Boolean)
        fun onRomsFinderCancel(searchNew: Boolean)
    }

    private inner class DirInfo(var file: File, var level: Int)
    companion object {
        private const val TAG = "RomsFinder"
        fun getAllGames(helper: DatabaseHelper): ArrayList<GameDescription> {
            return helper.selectObjsFromDb(
                GameDescription::class.java,
                false,
                "GROUP BY checksum",
                null
            )
        }
    }
}