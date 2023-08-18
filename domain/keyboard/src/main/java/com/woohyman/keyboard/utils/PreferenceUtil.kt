package nostalgia.framework.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.data.database.GameDescription
import nostalgia.framework.data.entity.GfxProfile
import nostalgia.framework.emulator.Emulator
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.PrintWriter

object PreferenceUtil {
    const val EXPORT = 1
    const val IMPORT = 2
    const val GAME_PREF_SUFFIX = ".gamepref"
    private const val escapedI = "{escapedI:-)}"
    private const val escapedN = "{escapedN:-)}"
    private const val escapedNull = "{escapedNULL:-)}"
    fun isBatterySaveBugFixed(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("bs_bug_fixed", false)
    }

    fun setBatterySaveBugFixed(context: Context?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("bs_bug_fixed", true)
        editor.apply()
    }

    @JvmStatic
    fun isQuickSaveEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_quicksave", false)
    }

    @JvmStatic
    fun getFastForwardFrameCount(context: Context?): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val speed = pref.getInt("general_pref_ff_speed", 4)
        return (speed + 1) * 2
    }

    @JvmOverloads
    fun migratePreferences(
        type: Int,
        pref: SharedPreferences,
        file: File,
        handling: NotFoundHandling = NotFoundHandling.FAIL
    ) {
        if (type == EXPORT) {
            exportPreferences(pref, file)
        } else if (type == IMPORT) {
            importPreferences(pref, file, handling)
        } else throw IllegalArgumentException()
    }

    fun exportPreferences(pref: SharedPreferences, file: File?) {
        val prefs = pref.all
        if (prefs.size == 0) {
            return
        }
        var writer: PrintWriter? = null
        try {
            writer = PrintWriter(file)
            for (entry in prefs.entries) {
                val o = entry.value!!
                var type: String? = null
                var value = entry.value
                if (o.javaClass == Int::class.java) {
                    type = "I"
                }
                if (o.javaClass == Long::class.java) {
                    type = "L"
                }
                if (o.javaClass == String::class.java) {
                    type = "S"
                    var `val` = value as String?
                    `val` = `val`!!.replace("|", escapedI)
                    `val` = `val`.replace("\n", escapedN)
                    value = `val`
                }
                if (o.javaClass == Float::class.java) {
                    type = "F"
                }
                if (o.javaClass == Boolean::class.java) {
                    type = "B"
                }
                if (type == null) {
                    throw RuntimeException("unknown type")
                }
                if (value == null) {
                    value = escapedNull
                }
                var name = entry.key
                name = name.replace("|", escapedI)
                writer.write("$type|$name|$value\n")
            }
        } catch (e: FileNotFoundException) {
            throw RuntimeException(e)
        } finally {
            writer?.close()
        }
    }

    fun importPreferences(pref: SharedPreferences, file: File, handling: NotFoundHandling) {
        if (handling == NotFoundHandling.IGNORE && !file.exists()) {
            return
        }
        var reader: BufferedReader? = null
        try {
            val r = FileReader(file)
            reader = BufferedReader(r)
            var line: String
            val editor = pref.edit()
            while (reader.readLine().also { line = it } != null) {
                val parts =
                    line.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = parts[0]
                var name = parts[1]
                name = name.replace(escapedI, "|")
                var value: String? = parts[2]
                if (value == escapedNull) {
                    value = null
                }
                if (type == "I") {
                    editor.putInt(name, value?.toInt()!!)
                }
                if (type == "B") {
                    editor.putBoolean(
                        name,
                        (if (value != null) java.lang.Boolean.parseBoolean(value) else null)!!
                    )
                }
                if (type == "F") {
                    editor.putFloat(name, value?.toFloat()!!)
                }
                if (type == "L") {
                    editor.putLong(name, value?.toLong()!!)
                }
                if (type == "S") {
                    if (value != null) {
                        value = value.replace(escapedI, "|")
                        value = value.replace(escapedN, "\n")
                    }
                    editor.putString(name, value)
                }
            }
            editor.apply()
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (ignored: Exception) {
                }
            }
        }
    }

    @JvmStatic
    fun setViewPort(
        context: Context?, vp: ViewPort,
        physicalScreenWidth: Int, physicalScreenHeight: Int
    ) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = pref.edit()
        val tmp = physicalScreenWidth.toString() + "x" + physicalScreenHeight
        val x = "vp-x-$tmp"
        val y = "vp-y-$tmp"
        val width = "vp-width-$tmp"
        val height = "vp-height-$tmp"
        edit.putInt(x, vp.x)
        edit.putInt(y, vp.y)
        edit.putInt(width, vp.width)
        edit.putInt(height, vp.height)
        edit.apply()
    }

    @JvmStatic
    fun removeViewPortSave(context: Context?) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = pref.edit()
        for (key in pref.all.keys) {
            if (key.startsWith("vp-")) {
                edit.remove(key)
            }
        }
        edit.apply()
    }

    fun getViewPort(
        context: Context?,
        physicalScreenWidth: Int,
        physicalScreenHeight: Int
    ): ViewPort? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val tmp = physicalScreenWidth.toString() + "x" + physicalScreenHeight
        val x = "vp-x-$tmp"
        val y = "vp-y-$tmp"
        val width = "vp-width-$tmp"
        val height = "vp-height-$tmp"
        val vp = ViewPort()
        vp.x = pref.getInt(x, -1)
        vp.y = pref.getInt(y, -1)
        vp.width = pref.getInt(width, -1)
        vp.height = pref.getInt(height, -1)
        return if (vp.x == -1 || vp.y == -1 || vp.width == -1 || vp.height == -1) {
            null
        } else vp
    }

    @JvmStatic
    fun getFragmentShader(context: Context?): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getInt("fragment_shader", -1)
    }

    @JvmStatic
    fun setFragmentShader(context: Context?, shader: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putInt("fragment_shader", shader)
        editor.apply()
    }

    fun isSoundEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val muted = pref.getBoolean("general_pref_mute", false)
        return !muted
    }

    fun isLoadSavFiles(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_load_sav_files", true)
    }

    fun isSaveSavFiles(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_save_sav_files", true)
    }

    @JvmStatic
    fun isBenchmarked(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("is_benchmarked", false)
    }

    @JvmStatic
    fun setBenchmarked(context: Context?, value: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = pref.edit()
        edit.putBoolean("is_benchmarked", value)
        edit.apply()
    }

    fun getVibrationDuration(context: Context): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return getVibrationDuration(context, pref)
    }

    @JvmStatic
    fun setEmulationQuality(context: Context?, quality: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = pref.edit()
        edit.putString("general_pref_quality", quality.toString() + "")
        edit.apply()
    }

    @JvmStatic
    fun getEmulationQuality(context: Context?): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getString("general_pref_quality", "1")!!.toInt()
    }

    fun isTurboEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_turbo", false)
    }

    fun isABButtonEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_ab_button", false)
    }

    fun isFullScreenEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_fullscreen", false)
    }

    fun getControlsOpacity(context: Context?): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return (pref.getInt("general_pref_ui_opacity", 100) / 100f * 255).toInt()
    }

    @JvmStatic
    fun isAutoHideControls(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_ui_autohide", true)
    }

    private fun getVibrationDuration(context: Context, pref: SharedPreferences): Int {
        return pref.getInt("game_pref_ui_strong_vibration", 0) * 10
    }

    fun getVideoProfile(context: Context, emulator: Emulator?, game: GameDescription): GfxProfile? {
        val gfxProfileName = getVideoMode(context, emulator, game.checksum)
        var gfx: GfxProfile? = null
        if (gfxProfileName != null) {
            for (profile in info!!.availableGfxProfiles!!) {
                if (profile!!.name!!.lowercase() ==
                    gfxProfileName.lowercase()
                ) {
                    gfx = profile
                    break
                }
            }
        }
        if (gfx == null && emulator != null) {
            gfx = emulator.autoDetectGfx(game)
        }
        return gfx
    }

    @JvmStatic
    fun getLastGfxProfile(context: Context?): GfxProfile? {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val name = pref.getString("_lastGfx", null)
        try {
            val profiles = info!!.availableGfxProfiles
            for (profile in profiles!!) {
                if (profile!!.name == name) {
                    return profile
                }
            }
        } catch (ignored: Exception) {
        }
        return info!!.defaultGfxProfile
    }

    fun setLastGfxProfile(context: Context?, profile: GfxProfile) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val edit = pref.edit()
        edit.putString("_lastGfx", profile.name)
        edit.apply()
    }

    fun getVideoMode(
        context: Context, emulator: Emulator?,
        gameHash: String?
    ): String? {
        return if (gameHash == null) {
            null
        } else {
            val pref = context.getSharedPreferences(
                gameHash
                        + GAME_PREF_SUFFIX, Context.MODE_PRIVATE
            )
            getVideoMode(context, emulator, pref)
        }
    }

    private fun getVideoMode(
        context: Context,
        emulator: Emulator?,
        pref: SharedPreferences
    ): String? {
        return pref.getString("game_pref_ui_pal_ntsc_switch", null)
    }

    @JvmStatic
    fun isZapperEnabled(context: Context, gameHash: String): Boolean {
        val pref = context.getSharedPreferences(gameHash + GAME_PREF_SUFFIX, Context.MODE_PRIVATE)
        return isZapperEnable(context, pref)
    }

    private fun isZapperEnable(context: Context, pref: SharedPreferences): Boolean {
        return pref.getBoolean("game_pref_zapper", false)
    }

    @JvmStatic
    fun getDisplayRotation(context: Context): ROTATION {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return getDisplayRotation(context, pref)
    }

    @JvmStatic
    fun getLastGalleryTab(context: Context?): Int {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getInt("LastGalleryTab", 0)
    }

    @JvmStatic
    fun saveLastGalleryTab(context: Context?, tabIdx: Int) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putInt("LastGalleryTab", tabIdx)
        editor.apply()
    }

    private fun getDisplayRotation(context: Context, pref: SharedPreferences): ROTATION {
        val i = pref.getString("general_pref_rotation", "0")!!.toInt()
        return ROTATION.values()[i]
    }

    @JvmStatic
    fun isTimeshiftEnabled(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return isTimeshiftEnable(context, pref)
    }

    private fun isTimeshiftEnable(context: Context, pref: SharedPreferences): Boolean {
        return pref.getBoolean("game_pref_ui_timeshift", false)
    }

    fun isWifiServerEnable(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return isWifiServerEnable(context, pref)
    }

    private fun isWifiServerEnable(context: Context, pref: SharedPreferences): Boolean {
        return pref.getBoolean("general_pref_wifi_server_enable", false)
    }

    fun setWifiServerEnable(context: Context, enable: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        setWifiServerEnable(context, pref, enable)
    }

    private fun setWifiServerEnable(context: Context, pref: SharedPreferences, enable: Boolean) {
        val edit = pref.edit()
        edit.putBoolean("general_pref_wifi_server_enable", enable)
        edit.apply()
    }

    fun isOpenGLEnable(context: Context): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return isOpenGLEnable(context, pref)
    }

    private fun isOpenGLEnable(context: Context, pref: SharedPreferences): Boolean {
        return pref.getBoolean("general_pref_opengl", true)
    }

    @JvmStatic
    fun isDynamicDPADEnable(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_ddpad", false)
    }

    fun setDynamicDPADEnable(context: Context?, enable: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("general_pref_ddpad", enable)
        editor.apply()
    }

    @JvmStatic
    fun setDynamicDPADUsed(context: Context?, used: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("general_pref_ddpad_used", used)
        editor.apply()
    }

    fun isDynamicDPADUsed(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_ddpad_used", false)
    }

    fun isFastForwardUsed(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_fastforward_used", false)
    }

    @JvmStatic
    fun setFastForwardUsed(context: Context?, used: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("general_pref_fastforward_used", used)
        editor.apply()
    }

    fun isScreenLayoutUsed(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_screen_layout_used", false)
    }

    @JvmStatic
    fun setScreenLayoutUsed(context: Context?, used: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("general_pref_screen_layout_used", used)
        editor.apply()
    }

    @JvmStatic
    fun isFastForwardEnabled(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_fastforward", false)
    }

    @JvmStatic
    fun isScreenSettingsSaved(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        for (key in pref.all.keys) {
            if (key.startsWith("vp-")) return true
        }
        return false
    }

    @JvmStatic
    fun isFastForwardToggleable(context: Context?): Boolean {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        return pref.getBoolean("general_pref_fastforward_toggle", true)
    }

    fun setFastForwardEnable(context: Context?, enable: Boolean) {
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref.edit()
        editor.putBoolean("general_pref_fastforward", enable)
        editor.apply()
    }

    enum class NotFoundHandling {
        IGNORE, FAIL
    }

    enum class ROTATION {
        AUTO, PORT, LAND
    }
}