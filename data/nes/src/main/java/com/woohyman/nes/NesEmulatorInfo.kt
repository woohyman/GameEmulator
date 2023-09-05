package com.woohyman.nes

import com.woohyman.keyboard.controllers.KeyAction
import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.data.entity.SfxProfile
import com.woohyman.keyboard.keyboard.BasicEmulatorInfo
import com.woohyman.nes.util.EmulatorConst.high
import com.woohyman.nes.util.EmulatorConst.low
import com.woohyman.nes.util.EmulatorConst.medium
import com.woohyman.nes.util.EmulatorConst.ntsc
import com.woohyman.nes.util.EmulatorConst.pal
import javax.inject.Inject

class NesEmulatorInfo @Inject constructor() : BasicEmulatorInfo() {
    override val defaultGfxProfile: NesGfxProfile = ntsc

    private val gfxProfiles: List<GfxProfile> = listOf(defaultGfxProfile, pal)
    private val sfxProfiles: List<SfxProfile> = listOf(low, medium, high)
    override fun getMappingValue(action: Int): Int {
        return when (action) {
            KeyAction.KEY_A.key -> 0x01
            KeyAction.KEY_B.key -> 0x02
            KeyAction.KEY_SELECT.key -> 0x04
            KeyAction.KEY_START.key -> 0x08
            KeyAction.KEY_UP.key -> 0x10
            KeyAction.KEY_DOWN.key -> 0x20
            KeyAction.KEY_LEFT.key -> 0x40
            KeyAction.KEY_RIGHT.key -> 0x80
            KeyAction.KEY_A_TURBO.key -> 0x01 + 1000
            KeyAction.KEY_B_TURBO.key -> 0x02 + 1000
            else -> -1
        }
    }

    override fun getMappingValue(action: KeyAction): Int {
        return getMappingValue(action.key)
    }

    override fun hasZapper(): Boolean {
        return true
    }

    override val name: String = "Nostalgia.NES"
    override val defaultSfxProfile: SfxProfile = sfxProfiles[0]
    override val availableGfxProfiles: List<GfxProfile?> = gfxProfiles
    override val availableSfxProfiles: List<SfxProfile> = sfxProfiles

    override fun supportsRawCheats(): Boolean {
        return true
    }

    override val numQualityLevels: Int = 3

    class NesGfxProfile(
        name: String,
        originalScreenWidth: Int,
        originalScreenHeight: Int,
        fps: Int
    ) : GfxProfile(
        name,
        originalScreenWidth,
        originalScreenHeight,
        fps
    ) {
        override fun toInt() = if (fps == 50) 1 else 0
    }

    class NesSfxProfile(
        name: String,
        isStereo: Boolean,
        rate: Int,
        bufferSize: Int,
        encoding: SoundEncoding,
        quality: Int
    ) : SfxProfile(
        name,
        isStereo,
        rate,
        bufferSize,
        encoding,
        quality
    ) {
        override fun toInt(): Int {
            var x = rate / 11025
            x += quality * 100
            return x
        }
    }
}