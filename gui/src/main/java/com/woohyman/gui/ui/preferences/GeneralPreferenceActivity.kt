package com.woohyman.gui.ui.preferences

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.ListPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceCategory
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.woohyman.gui.R
import com.woohyman.gui.ui.preferences.KeyboardSettingsActivity
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.keyboard.KeyboardProfile.Companion.getProfilesNames

class GeneralPreferenceActivity : AppCompatPreferenceActivity() {
    override fun isValidFragment(fragmentName: String): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        NEW_PROFILE = getText(R.string.key_profile_new).toString()
        initKeyboardProfiles()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBuildHeaders(target: List<Header>) {
        loadHeadersFromResource(R.xml.general_preferences_header, target)
    }

    override fun onResume() {
        super.onResume()
        initKeyboardProfiles()
        var found = false
        for (keyboardProfileName in keyboardProfileNames!!) {
            if (keyboardProfileName == selProfile!!.value) {
                found = true
                break
            }
        }
        if (!found) {
            val pref = PreferenceManager.getDefaultSharedPreferences(this)
            val edit = pref.edit()
            edit.putString("pref_game_keyboard_profile", "default")
            edit.apply()
            setNewProfile(selProfile, edProfile, "default")
            selProfile!!.value = "default"
            selProfile!!.entries = keyboardProfileNames
            selProfile!!.entryValues = keyboardProfileNames
        }
    }

    override fun onPause() {
        super.onPause()
    }

    private fun initKeyboardProfiles() {
        val names = getProfilesNames(this)
        keyboardProfileNames = arrayOfNulls(names.size + 1)
        var i = 0
        for (name in names) {
            keyboardProfileNames!![i++] = name
        }
        keyboardProfileNames!![names.size] = NEW_PROFILE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (resultCode != KeyboardSettingsActivity.RESULT_NAME_CANCEL) {
            val profileNames = getProfilesNames(this)
            keyboardProfileNames = arrayOfNulls(profileNames.size + 1)
            var i = 0
            for (name in profileNames) {
                keyboardProfileNames!![i++] = name
            }
            keyboardProfileNames!![profileNames.size] = NEW_PROFILE
            val name = data.getStringExtra(KeyboardSettingsActivity.EXTRA_PROFILE_NAME)
            selProfile!!.value = name
            setNewProfile(selProfile, edProfile, name)
            initProfiles(this, selProfile, edProfile)
        }
    }

    companion object {
        var keyboardProfileNames: Array<String?>? = null
        var selProfile: ListPreference? = null
        var edProfile: Preference? = null
        private var NEW_PROFILE: String? = null
        fun initProPreference(pref: Preference?, activity: Activity?) {}
        fun initDDPAD(ddpad: CheckBoxPreference?, activity: Activity?) {}
        fun initScreenSettings(screenSettings: Preference?, activity: Activity?) {}
        fun initInputMethodPreference(imPreference: Preference, activity: Activity) {
            imPreference.onPreferenceClickListener =
                OnPreferenceClickListener { preference: Preference? ->
                    val imeManager = activity.applicationContext.getSystemService(
                        INPUT_METHOD_SERVICE
                    ) as InputMethodManager
                    if (imeManager != null) {
                        imeManager.showInputMethodPicker()
                    } else {
                        Toast.makeText(
                            activity,
                            R.string.pref_keyboard_cannot_change_input_method, Toast.LENGTH_LONG
                        ).show()
                    }
                    false
                }
        }

        fun initAboutGamePreference(pref: Preference, activity: Activity) {
            pref.onPreferenceClickListener = OnPreferenceClickListener { preference: Preference? ->
                activity.startActivity(Intent(activity, AboutActivity::class.java))
                false
            }
        }

        fun initFastForward(ff: CheckBoxPreference?, activity: Activity?) {}
        fun initQuality(cat: PreferenceCategory, pref: Preference?) {
            if (info!!.numQualityLevels == 0) {
                cat.removePreference(pref)
            }
        }

        fun setNewProfile(listProfile: ListPreference?, editProfile: Preference?, name: String?) {
            listProfile!!.summary = name
            editProfile!!.summary = name
            editProfile.setTitle(R.string.key_profile_edit)
            editProfile.intent.putExtra(KeyboardSettingsActivity.EXTRA_PROFILE_NAME, name)
        }

        fun initProfiles(
            context: Activity, selectProfile: ListPreference?,
            editProfile: Preference?
        ) {
            selProfile = selectProfile
            edProfile = editProfile
            selectProfile!!.entries = keyboardProfileNames
            selectProfile.entryValues = keyboardProfileNames
            selectProfile.setDefaultValue("default")
            if (selectProfile.value == null) {
                selectProfile.value = "default"
            }
            selectProfile.onPreferenceChangeListener =
                OnPreferenceChangeListener { preference: Preference?, newValue: Any ->
                    if (newValue == NEW_PROFILE) {
                        val i = Intent(context, KeyboardSettingsActivity::class.java)
                        i.putExtra(KeyboardSettingsActivity.EXTRA_PROFILE_NAME, "default")
                        i.putExtra(KeyboardSettingsActivity.EXTRA_NEW_BOOL, true)
                        context.startActivityForResult(i, 0)
                        return@OnPreferenceChangeListener false
                    } else {
                        setNewProfile(selectProfile, editProfile, newValue as String)
                        return@OnPreferenceChangeListener true
                    }
                }
        }
    }
}