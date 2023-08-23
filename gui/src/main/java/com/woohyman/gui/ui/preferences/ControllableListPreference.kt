package com.woohyman.gui.ui.preferences

import android.content.Context
import android.os.Bundle
import android.preference.ListPreference
import android.util.AttributeSet

class ControllableListPreference : ListPreference {
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    override fun showDialog(state: Bundle) {
        super.showDialog(state)
    }
}