package com.woohyman.xml.gamegallery.list

import android.annotation.SuppressLint
import android.widget.AbsListView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.rom.IRomLauncher
import com.woohyman.xml.R
import com.woohyman.xml.gamegallery.adapter.GalleryAdapter
import com.woohyman.xml.gamegallery.adapter.StoreAdapter
import com.woohyman.xml.gamegallery.base.BaseGalleryAdapter
import com.woohyman.xml.gamegallery.model.TabInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("ViewConstructor")
@AndroidEntryPoint
class RomListView(
    activity: AppCompatActivity,
    tabInfo: TabInfo,
) : ListView(activity) {

    @Inject
    lateinit var romLauncher: IRomLauncher

    private var galleryAdapter: BaseGalleryAdapter

    init {
        cacheColorHint = 0x00000000
        isFastScrollEnabled = true
        setSelector(R.drawable.row_game_item_list_selector)
        adapter =
            if (tabInfo is TabInfo.StoreRomList) StoreAdapter(
                activity,
                romLauncher
            ) else GalleryAdapter(
                activity,
                romLauncher
            )

        galleryAdapter = adapter as BaseGalleryAdapter
        galleryAdapter.setSortType(tabInfo)
    }

    fun setLocalData(games: ArrayList<GameDescription>) {
        if (galleryAdapter is GalleryAdapter) {
            galleryAdapter.games = games
        }
    }

    fun addLocalData(games: ArrayList<GameDescription>) {
        if (galleryAdapter is GalleryAdapter) {
            galleryAdapter.addGames(games)
        }
    }

    fun setFilter(filter: String) {
        if (galleryAdapter is GalleryAdapter) {
            galleryAdapter.setFilter(filter)
        }
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