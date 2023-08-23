package com.woohyman.gui.ui.videwmodels

import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.RowItem
import com.woohyman.keyboard.download.RomDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownLoadViewModel @Inject constructor(
    private val downLoader: RomDownloader
) : ViewModel() {
    val downLoadState = downLoader.downLoadState

    private var curDownLoadGame: GameDescription? = null

    fun getGamePosition(list:ArrayList<RowItem>): Int {
        list.forEachIndexed { index, rowItem ->
            if(rowItem.game == curDownLoadGame){
                return index
            }
        }
        return -1
    }

    fun isGameDownLoading(gameDescription: GameDescription): Boolean {
        if (gameDescription != curDownLoadGame) {
            return false
        }
        return downLoadState.value is RomDownloader.DownLoadResult.DownLoading
                || downLoadState.value is RomDownloader.DownLoadResult.Start
    }

    fun startDownload(gameDescription: GameDescription) {
        curDownLoadGame = gameDescription
        downLoader.startDownload(gameDescription)
    }

    fun getFilePath(gameDescription: GameDescription): String {
        return Utils.getApp().filesDir.absolutePath + File.separator + gameDescription.name + ".nes"
    }
}