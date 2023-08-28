package com.woohyman.keyboard.rom

import com.blankj.utilcode.util.Utils
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.NLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject

class RomDownloader @Inject constructor() {
    sealed class DownLoadResult {
        object Idle : DownLoadResult()
        data class Start(val gameDescription: GameDescription) : DownLoadResult()
        data class DownLoading(val gameDescription: GameDescription, val progress: Int) :
            DownLoadResult()

        data class Success(val gameDescription: GameDescription) : DownLoadResult()
        data class Fail(val throwable: Throwable?) : DownLoadResult()
    }

    private val _downLoadState = MutableStateFlow<DownLoadResult>(DownLoadResult.Idle)
    val downLoadState:StateFlow<DownLoadResult> = _downLoadState

    fun startDownload(gameDescription: GameDescription) {
        FileDownloader.setup(Utils.getApp())
        val romDownloadListener = RomDownloadListener(gameDescription)

        val filePath =
            Utils.getApp().filesDir.absolutePath + File.separator + gameDescription.name + ".nes"
        val task = FileDownloader.getImpl().create(gameDescription.url)
            .setPath(filePath)
            .setListener(romDownloadListener)
        task.start()
    }

    inner class RomDownloadListener constructor(
        private val gameDescription: GameDescription
    ) : FileDownloadListener() {
        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {

        }

        override fun connected(
            task: BaseDownloadTask?,
            etag: String?,
            isContinue: Boolean,
            soFarBytes: Int,
            totalBytes: Int
        ) {
            super.connected(task, etag, isContinue, soFarBytes, totalBytes)
            _downLoadState.value = DownLoadResult.Start(gameDescription)
        }

        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
            _downLoadState.value =
                DownLoadResult.DownLoading(gameDescription, soFarBytes * 100 / totalBytes)
        }

        override fun completed(task: BaseDownloadTask?) {
            gameDescription.path = task!!.path
            _downLoadState.value = DownLoadResult.Success(gameDescription)
        }

        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {

        }

        override fun error(task: BaseDownloadTask?, e: Throwable?) {
            _downLoadState.value = DownLoadResult.Fail(e)
        }

        override fun warn(task: BaseDownloadTask?) {

        }

    }

}