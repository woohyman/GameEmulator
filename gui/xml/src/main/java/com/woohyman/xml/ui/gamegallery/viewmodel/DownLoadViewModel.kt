package com.woohyman.xml.ui.gamegallery.viewmodel

import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.RowItem
import com.woohyman.keyboard.rom.RomDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownLoadViewModel @Inject constructor(
    private val downLoader: RomDownloader
) : ViewModel() {
    val downLoadState = downLoader.downLoadState

    private var curDownLoadGame: GameDescription? = null

    fun getGamePosition(list: ArrayList<RowItem>): Int {
        list.forEachIndexed { index, rowItem ->
            if (rowItem.game == curDownLoadGame) {
                return index
            }
        }
        return -1
    }

    fun startDownload(gameDescription: GameDescription) {
        curDownLoadGame = gameDescription
        downLoader.startDownload(gameDescription)
    }

    //坚持Rom文件是否存在
    fun checkRomExist(gameDescription: GameDescription): Boolean {
        val filePath =
            Utils.getApp().filesDir.absolutePath + File.separator + gameDescription.name + ".nes"
        return FileUtils.isFileExists(filePath)
    }

    fun syncRomPath(rowItems: ArrayList<RowItem>) {
        rowItems.forEach {
            it.game?.let { game ->
                val filePath =
                    Utils.getApp().filesDir.absolutePath + File.separator + game.name + ".nes"
                val isFileExist = FileUtils.isFileExists(filePath)
                if (game.path.isEmpty() && isFileExist) {
                    game.path = filePath
                }
            }
        }
    }
}