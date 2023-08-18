package nostalgia.framework.data.entity

import android.util.SparseIntArray
import nostalgia.framework.keyboard.KeyboardProfile

interface EmulatorInfo {
    val name: String?
    fun hasZapper(): Boolean
    fun supportsRawCheats(): Boolean
    val cheatInvalidCharsRegex: String?
    val defaultGfxProfile: GfxProfile?
    val defaultSfxProfile: SfxProfile?
    val defaultKeyboardProfile: KeyboardProfile?
    val availableGfxProfiles: List<GfxProfile?>?
    val availableSfxProfiles: List<SfxProfile?>?
    val keyMapping: SparseIntArray?
    val numQualityLevels: Int
    val deviceKeyboardCodes: IntArray?
    val deviceKeyboardNames: Array<String>?
    val deviceKeyboardDescriptions: Array<String>?
    val isMultiPlayerSupported: Boolean
}