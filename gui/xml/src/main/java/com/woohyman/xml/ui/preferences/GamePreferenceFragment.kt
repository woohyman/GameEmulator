package com.woohyman.xml.ui.preferences

import android.os.Bundle
import android.preference.ListPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import com.woohyman.xml.R
import com.woohyman.keyboard.utils.EmuUtils

class GamePreferenceFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefMgr = preferenceManager
        prefMgr.sharedPreferencesName = EmuUtils.fetchProxy.game.checksum + ".gamepref"
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