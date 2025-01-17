package com.woohyman.xml.gamegallery.adapter

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.gamegallery.base.BaseGalleryAdapter
import com.woohyman.xml.gamegallery.list.RomListView
import com.woohyman.xml.gamegallery.model.SortType
import com.woohyman.xml.gamegallery.model.TabInfo

class GalleryPagerAdapter(
    private val activity: AppCompatActivity,
) : PagerAdapter() {

    private val tabTypes = arrayOf(
        TabInfo.StoreRomList(SortType.SORT_BY_NAME_ALPHA),
        TabInfo.LocalRomList(SortType.SORT_BY_NAME_ALPHA),
//        TabInfo.LocalRomList(SortType.SORT_BY_LAST_PLAYED)
    )

    private val listViews: ArrayList<RomListView> = ArrayList<RomListView>().also {
        for (type in tabTypes) {
            it.add(RomListView(activity, type))
        }
    }

    private var yOffsets: IntArray? = IntArray(tabTypes.size)
    private val lists = arrayOfNulls<RomListView>(tabTypes.size)

    override fun getCount(): Int {
        return tabTypes.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return tabTypes[position].tabName
    }

    override fun isViewFromObject(arg0: View, arg1: Any): Boolean {
        return arg0 == arg1
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val list = listViews[position]

        list.setSelection(yOffsets!![position])
        list.registerScrollStateChangedListener { scrollState, firstVisiblePosition ->
            NLog.i("list", "$position:$scrollState")
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                yOffsets?.set(position, firstVisiblePosition)
            }
        }
        lists[position] = list
        container.addView(list)
        return list
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    fun setGames(games: ArrayList<GameDescription>) {
        for (listView in listViews) {
            listView.setLocalData(games)
        }
    }

    fun addGames(newGames: ArrayList<GameDescription>) {
        for (listView in listViews) {
            listView.addLocalData(newGames)
        }
    }

    fun setFilter(filter: String) {
        for (listView in listViews) {
            listView.setFilter(filter)
        }
    }

    override fun notifyDataSetChanged() {
        for (i in tabTypes.indices) {
            val adapter = listViews[i].adapter as BaseGalleryAdapter
            adapter.notifyDataSetChanged()
            if (lists[i] != null) lists[i]?.setSelection(yOffsets?.get(i) ?: 0)
        }
        super.notifyDataSetChanged()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putIntArray(EXTRA_POSITIONS, yOffsets)
    }

    fun onRestoreInstanceState(inState: Bundle?) {
        yOffsets = inState?.getIntArray(EXTRA_POSITIONS) ?: IntArray(listViews.size)
    }

    companion object {
        const val EXTRA_POSITIONS = "EXTRA_POSITIONS"
    }
}