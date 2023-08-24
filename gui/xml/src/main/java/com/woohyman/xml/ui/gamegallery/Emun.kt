package com.woohyman.xml.ui.gamegallery

import com.blankj.utilcode.util.Utils
import com.woohyman.xml.R

enum class SORT_TYPES {
    SORT_BY_NAME_ALPHA {
        override val tabName: String
            get() = names[0]
    },
    SORT_BY_INSERT_DATE {
        override val tabName: String
            get() = names[1]
    },
    SORT_BY_MOST_PLAYED {
        override val tabName: String
            get() = names[2]
    },
    SORT_BY_LAST_PLAYED {
        override val tabName: String
            get() = names[3]
    };

    protected val names: Array<String> =
        Utils.getApp().resources.getStringArray(R.array.gallery_page_tab_names)
    abstract val tabName: String
}