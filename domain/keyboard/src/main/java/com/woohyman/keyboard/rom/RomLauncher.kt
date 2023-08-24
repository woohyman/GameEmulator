package com.woohyman.keyboard.rom

import android.app.AlertDialog
import android.content.DialogInterface
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.ZipRomFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject

class RomLauncher @Inject constructor():IRomLauncher {
    private val TAG = javaClass.simpleName

    private val _romLauncherState = MutableSharedFlow<Result<GameDescription>>(replay = 1)
    override val romLauncherState: SharedFlow<Result<GameDescription>> = _romLauncherState

    private val dbHelper by lazy {
        DatabaseHelper(Utils.getApp())
    }

    override val isDBEmpty = dbHelper.countObjsInDb(GameDescription::class.java, null) == 0

    override suspend fun LauncherRom(game: GameDescription) {
        var gameFile = File(game.path)
        NLog.i(TAG, "select $game")
        if (game.isInArchive) {
            gameFile = File(Utils.getApp().externalCacheDir, game.checksum)
            game.path = gameFile.absolutePath
            val zipRomFile = dbHelper.selectObjFromDb(
                ZipRomFile::class.java,
                "WHERE _id=" + game.zipfile_id, false
            )
            val zipFile = File(zipRomFile.path)
            if (!gameFile.exists()) {
                try {
                    EmuUtils.extractFile(zipFile, game.name, gameFile)
                } catch (e: IOException) {
                    NLog.e(TAG, "", e)
                }
            }
        }
        if (gameFile.exists()) {
            game.lastGameTime = System.currentTimeMillis()
            game.runCount++
            dbHelper.updateObjToDb(game, arrayOf("lastGameTime", "runCount"))
            _romLauncherState.emit(Result.success(game))
        } else {
            NLog.w(TAG, "rom file:" + gameFile.absolutePath + " does not exist")
            _romLauncherState.emit(Result.failure(Exception()))
        }
    }

}