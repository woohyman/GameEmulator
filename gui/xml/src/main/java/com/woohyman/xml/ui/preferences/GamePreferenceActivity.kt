package com.woohyman.xml.ui.preferences

import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import android.view.MenuItem
import com.woohyman.xml.R
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.utils.EmuUtils

class GamePreferenceActivity : AppCompatPreferenceActivity() {
    private var game: GameDescription? = null
    override fun isValidFragment(fragmentName: String): Boolean {
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        game = intent.getSerializableExtra(EXTRA_GAME) as GameDescription?
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onBuildHeaders(target: List<Header>) {
        loadHeadersFromResource(R.xml.game_preferences_header, target)
    }

    companion object {
        const val EXTRA_GAME = "EXTRA_GAME"
        fun initZapper(zapper: Preference?, zapperCategory: PreferenceCategory) {
            if (!EmuUtils.getEmulatorInfo().hasZapper()) {
                zapperCategory.removePreference(zapper)
            }
        }

        fun initVideoPreference(
            preference: ListPreference,
            category: PreferenceCategory, screen: PreferenceScreen
        ) {
            val profiles = EmuUtils.getEmulatorInfo().availableGfxProfiles
            if (profiles!!.size > 1) {
                val res = arrayOfNulls<CharSequence>(
                    EmuUtils.getEmulatorInfo().availableSfxProfiles!!.size + 1
                )
                res[0] = "Auto"
                var i = 1
                for (gfx in profiles) {
                    res[i] = gfx!!.name
                    i++
                }
                preference.entries = res
                preference.entryValues = res
                if (preference.value == null) {
                    preference.value = "Auto"
                }
            } else {
                category.removePreference(preference)
                screen.removePreference(category)
            }
        }
    }
}