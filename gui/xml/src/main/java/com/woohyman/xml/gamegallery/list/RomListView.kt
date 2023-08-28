package com.woohyman.xml.gamegallery.list

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
import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.keyboard.rom.RomDownloader
import com.woohyman.keyboard.rom.RomLauncher
import com.woohyman.xml.R
import com.woohyman.xml.gamegallery.adapter.GalleryAdapter
import com.woohyman.xml.gamegallery.adapter.GalleryPagerAdapter
import com.woohyman.xml.gamegallery.adapter.StoreAdapter
import com.woohyman.xml.gamegallery.model.TabInfo
import com.woohyman.xml.gamegallery.viewmodel.DownLoadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("ViewConstructor")
@AndroidEntryPoint
class RomListView(
    activity: AppCompatActivity,
    tabInfo: TabInfo,
) : ListView(activity) {

    @Inject
    lateinit var romLauncher: IRomLauncher

    init {
        cacheColorHint = 0x00000000
        isFastScrollEnabled = true
        setSelector(R.drawable.row_game_item_list_selector)
        adapter =
            if (tabInfo is TabInfo.StoreRomList) StoreAdapter(
                activity,
                romLauncher
            ) else GalleryAdapter(
                activity
            )
    }

    fun registerScrollStateChangedListener(callback: (scrollState: Int, firstVisiblePosition: Int) -> Unit) {
        setOnScrollListener(object : OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                callback.invoke(scrollState, firstVisiblePosition)
            }

            override fun onScroll(
                view: AbsListView, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int
            ) {
            }
        })
    }
}