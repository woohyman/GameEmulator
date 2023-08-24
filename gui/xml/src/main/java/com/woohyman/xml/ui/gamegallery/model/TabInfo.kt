package com.woohyman.xml.ui.gamegallery.model

import com.blankj.utilcode.util.Utils
import com.woohyman.xml.R

val names: Array<String> = Utils.getApp().resources.getStringArray(R.array.gallery_page_tab_names)

enum class SortType(_tabName: String) {
    SORT_BY_NAME_ALPHA(names[0]),
    SORT_BY_INSERT_DATE(names[1]),
    SORT_BY_MOST_PLAYED(names[2]),
    SORT_BY_LAST_PLAYED(names[3]);

    val tabName = _tabName
}

sealed class TabInfo {
    data class StoreRomList(
        override val sortType: SortType,
        override val tabName: String = "游戏商店",
    ) : TabInfo()

    data class LocalRomList(override val sortType: SortType) : TabInfo() {
        override val tabName: String
            get() = sortType.tabName
    }

    abstract val tabName: String
    abstract val sortType: SortType
}