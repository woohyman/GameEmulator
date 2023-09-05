package com.woohyman.keyboard.data.entity

import com.woohyman.keyboard.controllers.KeyAction
import com.woohyman.keyboard.keyboard.KeyboardProfile

interface EmulatorInfo {
    val name: String?
    fun hasZapper(): Boolean
    fun supportsRawCheats(): Boolean
    val cheatInvalidCharsRegex: String?
    val defaultGfxProfile: GfxProfile
    val defaultSfxProfile: SfxProfile
    val defaultKeyboardProfile: KeyboardProfile?
    val availableGfxProfiles: List<GfxProfile?>?
    val availableSfxProfiles: List<SfxProfile?>?
    fun getMappingValue(action: Int): Int
    fun getMappingValue(action: KeyAction): Int
    val numQualityLevels: Int
    val deviceKeyboardCodes: IntArray
    val deviceKeyboardNames: Array<String>
    val deviceKeyboardDescriptions: Array<String>
    val isMultiPlayerSupported: Boolean
}