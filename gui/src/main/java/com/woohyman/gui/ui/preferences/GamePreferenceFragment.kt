package com.woohyman.gui.ui.preferences

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import com.woohyman.gui.R
import com.woohyman.keyboard.data.database.GameDescription

class GamePreferenceFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val game = activity.intent
            .getSerializableExtra(GamePreferenceActivity.Companion.EXTRA_GAME) as GameDescription?
        val prefMgr = preferenceManager
        prefMgr.sharedPreferencesName = game!!.checksum + ".gamepref"
        addPreferencesFromResource(R.xml.game_preferences)
        val videoProfile = findPreference("game_pref_ui_pal_ntsc_switch") as ListPreference
        val videoProfileCategory =
            findPreference("game_pref_ui_pal_ntsc_switch_category") as PreferenceCategory
        val zapperCategory = findPreference("game_pref_other_category") as PreferenceCategory
        val zapper = findPreference("game_pref_zapper")
        GamePreferenceActivity.Companion.initZapper(zapper, zapperCategory)
        GamePreferenceActivity.Companion.initVideoPreference(
            videoProfile,
            videoProfileCategory,
            preferenceScreen
        )
    }
}