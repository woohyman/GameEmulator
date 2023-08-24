package com.woohyman.xml.ui.gamegallery.list

import android.annotation.SuppressLint
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.woohyman.keyboard.data.entity.RowItem
import com.woohyman.keyboard.download.RomDownloader
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.R
import com.woohyman.xml.ui.gamegallery.adapter.GalleryAdapter
import com.woohyman.xml.ui.gamegallery.adapter.GalleryPagerAdapter
import com.woohyman.xml.ui.gamegallery.adapter.StoreAdapter
import com.woohyman.xml.ui.gamegallery.model.TabInfo
import com.woohyman.xml.ui.videwmodels.DownLoadViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@SuppressLint("ViewConstructor")
class RomListView(
    activity: AppCompatActivity,
    tabInfo: TabInfo,
    listener: GalleryPagerAdapter.OnItemClickListener
) : ListView(activity) {

    private val downLoaderViewModel by lazy {
        ViewModelProvider(activity)[DownLoadViewModel::class.java]
    }

    init {
        downLoaderViewModel.downLoadState.onEach {
            if (it is RomDownloader.DownLoadResult.Success) {
                listener.onItemClick(it.gameDescription)
            }
        }.launchIn(activity.lifecycleScope)

        cacheColorHint = 0x00000000
        isFastScrollEnabled = true
        setSelector(R.drawable.row_game_item_list_selector)
        adapter =
            if (tabInfo is TabInfo.StoreRomList) StoreAdapter(activity) else GalleryAdapter(
                activity
            )
        onItemClickListener =
            OnItemClickListener { parent: AdapterView<*>?, view: View, position: Int, id: Long ->
                (adapter.getItem(position) as RowItem).game?.let {
                    if (!downLoaderViewModel.checkRomExistAndSync(it)) {
                        downLoaderViewModel.startDownload(it)
                    } else {
                        listener.onItemClick(it)
                    }
                }
            }
    }

    fun registerScrollStateChangedListener(callback: (scrollState: Int,firstVisiblePosition: Int) -> Unit) {
        setOnScrollListener(object : OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                callback.invoke(scrollState,firstVisiblePosition)
            }

            override fun onScroll(
                view: AbsListView, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int
            ) {
            }
        })
    }
}