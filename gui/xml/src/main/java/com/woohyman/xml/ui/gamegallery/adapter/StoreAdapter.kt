package com.woohyman.xml.ui.gamegallery.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.StringUtils
import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.keyboard.rom.RomDownloader
import com.woohyman.keyboard.rom.RomLauncher
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.databinding.RowGameListBinding
import com.woohyman.xml.ui.gamegallery.base.BaseGalleryAdapter
import com.woohyman.xml.ui.gamegallery.data.TestRemoteRomSource
import com.woohyman.xml.ui.gamegallery.viewmodel.DownLoadViewModel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.WeakHashMap

class StoreAdapter(
    private val activity: AppCompatActivity, private val romLauncher: IRomLauncher
) : BaseGalleryAdapter(activity) {

    private data class ProgressInfo(
        val progressBar: WeakReference<ProgressBar>,
        var progress: Int,
        val url: String,
    )

    private var romDownLoadProgress: List<ProgressInfo> = emptyList()

    private val downLoaderViewModel by lazy {
        ViewModelProvider(activity)[DownLoadViewModel::class.java]
    }

    init {
        games = TestRemoteRomSource.remoteRomList
        activity.lifecycleScope.launch {
            downLoaderViewModel.syncRomPath(filterGames)
        }

        activity.lifecycleScope.launch {
            downLoaderViewModel.downLoadState.collect {
                when (it) {
                    is RomDownloader.DownLoadResult.Success -> {
                        romLauncher.LauncherRom(it.gameDescription)
                        romDownLoadProgress.forEach { progressInfo ->
                            if (progressInfo.url == it.gameDescription.url) {
                                progressInfo.progressBar.get()?.isVisible = false
                            }
                        }
                    }

                    is RomDownloader.DownLoadResult.Start -> {
                        romDownLoadProgress.forEach { progressInfo ->
                            if (progressInfo.url == it.gameDescription.url) {
                                progressInfo.progressBar.get()?.isVisible = true
                            }
                        }
                    }

                    is RomDownloader.DownLoadResult.DownLoading -> {
                        romDownLoadProgress.forEach { progressInfo ->
                            if (progressInfo.url == it.gameDescription.url) {
                                progressInfo.progress = it.progress
                                progressInfo.progressBar.get()?.progress = it.progress
                                progressInfo.progressBar.get()?.isVisible = true
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val (game) = filterGames[position]
        if (game == null) {
            return view
        }
        val binding = RowGameListBinding.bind(view)
        romDownLoadProgress.forEach {
            if (game.url == it.url) {
                binding.downloadProgressBar.progress = it.progress
                return@forEach
            }
        }

        view.setOnClickListener {
            if (!downLoaderViewModel.checkRomExist(game)) {
                downLoaderViewModel.startDownload(game)
            } else {
                activity.lifecycleScope.launch {
                    romLauncher.LauncherRom(game)
                }
            }
            romDownLoadProgress = romDownLoadProgress.plus(
                ProgressInfo(
                    WeakReference(binding.downloadProgressBar), 0, game.url
                )
            )
        }
        return view
    }

}