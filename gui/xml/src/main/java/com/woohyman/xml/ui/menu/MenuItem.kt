package com.woohyman.xml.ui.menu

import android.content.Intent
import android.graphics.drawable.Drawable

class MenuItem {
    @JvmField
    var itemId = 0
    @JvmField
    var title: String? = null
    @JvmField
    var icon: Drawable? = null
    var intent: Intent? = null
}