package com.woohyman.keyboard.rom

import android.app.AlertDialog
import android.content.DialogInterface
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.DatabaseHelper
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.ZipRomFile
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.io.IOException
import javax.inject.Inject

interface IRomLauncher{

    val isDBEmpty :Boolean

    suspend fun LauncherRom(game: GameDescription)

    val romLauncherState: SharedFlow<Result<GameDescription>>
}