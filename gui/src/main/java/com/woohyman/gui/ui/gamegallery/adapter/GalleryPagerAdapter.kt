package com.woohyman.gui.ui.gamegallery.adapter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.PagerAdapter
import com.blankj.utilcode.util.FileUtils
import com.woohyman.gui.R
import com.woohyman.gui.ui.videwmodels.DownLoadViewModel
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.RowItem
import com.woohyman.keyboard.download.RomDownloader
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.NLog.i

class GalleryPagerAdapter(
    private val activity: AppCompatActivity,
    private val listener: OnItemClickListener
) : PagerAdapter() {

    private val downLoaderViewModel by lazy {
        ViewModelProvider(activity)[DownLoadViewModel::class.java]
    }

    init {
        activity.lifecycleScope.launchWhenCreated {
            downLoaderViewModel.downLoadState.collect {
                NLog.e("RomDownloader", "state =========> $it")
                if (it is RomDownloader.DownLoadResult.Success) {
                    listener.onItemClick(it.gameDescription)
                }
            }
        }
    }

    private val tabTypes = arrayOf(
        GalleryAdapter.SORT_TYPES.SORT_BY_NAME_ALPHA,
        GalleryAdapter.SORT_TYPES.SORT_BY_LAST_PLAYED
    )
    private var yOffsets: IntArray? = IntArray(tabTypes.size + 1)
    private val lists = arrayOfNulls<ListView>(tabTypes.size + 1)
    private val listAdapters = arrayOfNulls<GalleryAdapter>(tabTypes.size)

    init {
        for (i in tabTypes.indices) {
            listAdapters[i] = GalleryAdapter(activity)
            val adapter = listAdapters[i]
            adapter!!.setSortType(tabTypes[i])
        }
    }

    override fun getCount(): Int {
        return tabTypes.size + 1
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return if (position == 0) {
            "游戏商店"
        } else {
            tabTypes[position - 1].tabName
        }
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 == arg1
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val list = ListView(activity)
        list.cacheColorHint = 0x00000000
        list.isFastScrollEnabled = true
        list.setSelector(R.drawable.row_game_item_list_selector)
        if (position == 0) {
            val adapter: ListAdapter = AppStoreAdapter(activity)
            list.adapter = adapter
            list.onItemClickListener =
                AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View, position: Int, id: Long ->
                    val (game) = adapter.getItem(position) as RowItem
                    val path = downLoaderViewModel.getFilePath(game!!)
                    val isFileExist = FileUtils.isFileExists(path)
                    if (game.path.isEmpty() && isFileExist) {
                        game.path = path
                    }
                    if (game.path.isEmpty()) {
                        downLoaderViewModel.startDownload(game)
                    } else {
                        listener.onItemClick(game)
                    }
                }
        } else {
            val adapter: ListAdapter? = listAdapters[position - 1]
            list.adapter = adapter
            list.onItemClickListener =
                AdapterView.OnItemClickListener { arg0: AdapterView<*>?, arg1: View?, arg2: Int, arg3: Long ->
                    val (game) = adapter!!.getItem(arg2) as RowItem
                    listener.onItemClick(game)
                }
        }
        list.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                i("list", "$position:$scrollState")
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    yOffsets!![position] = list.firstVisiblePosition
                }
            }

            override fun onScroll(
                view: AbsListView, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int
            ) {
            }
        })
        list.setSelection(yOffsets!![position])
        lists[position] = list
        container.addView(list)
        return list
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    fun setGames(games: ArrayList<GameDescription>?) {
        for (adapter in listAdapters) {
            adapter!!.setGames(ArrayList(games))
        }
    }

    fun addGames(newGames: ArrayList<GameDescription>?): Int {
        var result = 0
        for (adapter in listAdapters) {
            result = adapter!!.addGames(ArrayList(newGames))
        }
        return result
    }

    fun setFilter(filter: String?) {
        for (adapter in listAdapters) {
            adapter!!.setFilter(filter!!)
        }
    }

    override fun notifyDataSetChanged() {
        for (i in tabTypes.indices) {
            val adapter = listAdapters[i]
            adapter!!.notifyDataSetChanged()
            if (lists[i] != null) lists[i]!!.setSelection(yOffsets!![i])
        }
        super.notifyDataSetChanged()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(EXTRA_POSITIONS, yOffsets)
    }

    fun onRestoreInstanceState(inState: Bundle?) {
        if (inState != null) {
            yOffsets = inState.getIntArray(EXTRA_POSITIONS)
            if (yOffsets == null) yOffsets = IntArray(listAdapters.size)
        }
    }

    interface OnItemClickListener {
        fun onItemClick(game: GameDescription?)
    }

    companion object {
        const val EXTRA_POSITIONS = "EXTRA_POSITIONS"
    }
}