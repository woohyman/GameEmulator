package com.woohyman.xml.ui.gamegallery.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.woohyman.keyboard.download.RomDownloader
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.R
import com.woohyman.xml.databinding.RowGameListBinding
import com.woohyman.xml.ui.gamegallery.base.BaseGalleryAdapter
import com.woohyman.xml.ui.gamegallery.data.TestRemoteRomSource
import com.woohyman.xml.ui.videwmodels.DownLoadViewModel

class StoreAdapter(activity: AppCompatActivity) : BaseGalleryAdapter(activity) {
    init {
        addGames(TestRemoteRomSource.remoteRomList)
    }

    private val downLoaderViewModel by lazy {
        ViewModelProvider(activity)[DownLoadViewModel::class.java]
    }

    init {
        activity.lifecycleScope.launchWhenCreated {
            /*val position = downLoaderViewModel.getGamePosition(filterGames)*/
            downLoaderViewModel.downLoadState.collect {
                NLog.e("RomDownloader", "state3 =========> $it")
                notifyDataSetChanged()
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
        val state = downLoaderViewModel.downLoadState.value
        if (downLoaderViewModel.isGameDownLoading(game) && state is RomDownloader.DownLoadResult.DownLoading) {
            binding.downloadProgressBar.progress = state.progress
        }
        binding.downloadProgressBar.isVisible = downLoaderViewModel.isGameDownLoading(game)
        return view
    }

}