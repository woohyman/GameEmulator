package com.woohyman.gui

import android.view.View
import android.widget.ProgressBar
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.Utils
import com.liulishuo.okdownload.DownloadListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.breakpoint.BreakpointInfo
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.woohyman.gui.ui.gamegallery.adapter.GalleryPagerAdapter
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.NLog

class DownloaderDelegate(
    private val listener: GalleryPagerAdapter.OnItemClickListener,
    private val progressBar: ProgressBar,
    private val gameDescription: GameDescription
) {
    fun startDownload() {
        task.enqueue(downloadListener)
    }

    var task: DownloadTask
    private var downloadListener: DownloadListener = object : DownloadListener {
        var contentLength = 0L
        var curBytes = 0L
        override fun taskStart(task: DownloadTask) {
            NLog.e("test111", "taskStart ==> ")
            progressBar.progress = 0
            progressBar.visibility = View.VISIBLE
        }

        override fun connectTrialStart(
            task: DownloadTask,
            requestHeaderFields: Map<String, List<String>>
        ) {
        }

        override fun connectTrialEnd(
            task: DownloadTask,
            responseCode: Int,
            responseHeaderFields: Map<String, List<String>>
        ) {
        }

        override fun downloadFromBeginning(
            task: DownloadTask,
            info: BreakpointInfo,
            cause: ResumeFailedCause
        ) {
        }

        override fun downloadFromBreakpoint(task: DownloadTask, info: BreakpointInfo) {}
        override fun connectStart(
            task: DownloadTask,
            blockIndex: Int,
            requestHeaderFields: Map<String, List<String>>
        ) {
        }

        override fun connectEnd(
            task: DownloadTask,
            blockIndex: Int,
            responseCode: Int,
            responseHeaderFields: Map<String, List<String>>
        ) {
        }

        override fun fetchStart(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            this.contentLength = contentLength
            curBytes = 0L
            NLog.e("test111", "contentLength ==> \$contentLength")
        }

        override fun fetchProgress(task: DownloadTask, blockIndex: Int, increaseBytes: Long) {
            curBytes += increaseBytes
            val progress = (curBytes * 100 / contentLength).toInt()
            NLog.e("test111", "progress ==> \$progress")
            progressBar.progress = progress
            gameDescription.path = task.file!!.absolutePath
        }

        override fun fetchEnd(task: DownloadTask, blockIndex: Int, contentLength: Long) {
            listener.onItemClick(gameDescription)
            NLog.e("test111", "fetchEnd ==> \$blockIndex")
        }

        override fun taskEnd(task: DownloadTask, cause: EndCause, realCause: Exception?) {
            if (cause != EndCause.COMPLETED) {
                ToastUtils.showLong("错误信息 EndCause：" + cause + "，Exception：" + realCause?.message)
            }
            progressBar.visibility = View.INVISIBLE
        }
    }

    init {
        task = DownloadTask.Builder(gameDescription.url, Utils.getApp().filesDir)
            .setFilename(gameDescription.name + ".nes")
            .setMinIntervalMillisCallbackProcess(30) // 下载进度回调的间隔时间（毫秒）
            .setPassIfAlreadyCompleted(false) // 任务过去已完成是否要重新下载
            .setPriority(10)
            .build()
    }
}