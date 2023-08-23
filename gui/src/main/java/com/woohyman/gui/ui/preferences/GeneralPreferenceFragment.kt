package com.woohyman.gui.ui.preferences

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import com.woohyman.gui.R

class GeneralPreferenceFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.general_preferences)
        val vibration =
            findPreference("game_pref_ui_strong_vibration") as SeekBarVibrationPreference
        val vs = Context.VIBRATOR_SERVICE
        val mVibrator = activity.getSystemService(vs) as Vibrator
        vibration.isEnabled = mVibrator.hasVibrator()
        if (!mVibrator.hasVibrator()) {
            vibration.setSummary(R.string.game_pref_ui_vibration_no_vibrator)
        }
        val cat = findPreference("pref_general_settings_cat") as PreferenceCategory
        val quality = findPreference("general_pref_quality")
        val selectProfile = findPreference("pref_game_keyboard_profile") as ListPreference
        val editProfile = findPreference("pref_game_keyboard_edit_profile")
        GeneralPreferenceActivity.Companion.initQuality(cat, quality)
        GeneralPreferenceActivity.Companion.initProfiles(activity, selectProfile, editProfile)
        GeneralPreferenceActivity.Companion.setNewProfile(
            selectProfile,
            editProfile,
            selectProfile.value
        )
        val quicksave = findPreference("general_pref_quicksave")
        GeneralPreferenceActivity.Companion.initProPreference(quicksave, activity)
        val autoHide = findPreference("general_pref_ui_autohide")
        GeneralPreferenceActivity.Companion.initProPreference(autoHide, activity)
        val opacity = findPreference("general_pref_ui_opacity")
        GeneralPreferenceActivity.Companion.initProPreference(opacity, activity)
        val ddpad = findPreference("general_pref_ddpad") as CheckBoxPreference
        GeneralPreferenceActivity.Companion.initDDPAD(ddpad, activity)
        val screen = findPreference("general_pref_screen_layout")
        GeneralPreferenceActivity.Companion.initScreenSettings(screen, activity)
        val ff = findPreference("general_pref_fastforward") as CheckBoxPreference
        GeneralPreferenceActivity.Companion.initFastForward(ff, activity)
        val keyCat = findPreference("pref_keyboard_cat") as PreferenceCategory
        val inputMethod = keyCat.findPreference("pref_game_keyboard_select_input_method")
        GeneralPreferenceActivity.Companion.initInputMethodPreference(inputMethod, activity)
        val otherCat = findPreference("pref_others_cat") as PreferenceCategory
        val aboutPreference = otherCat.findPreference("pref_game_others_about_game")
        GeneralPreferenceActivity.Companion.initAboutGamePreference(aboutPreference, activity)
    }
}