package com.woohyman.nes

import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.data.entity.SfxProfile
import com.woohyman.keyboard.keyboard.BasicEmulatorInfo
import com.woohyman.nes.util.EmulatorConst
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

    override fun hasZapper(): Boolean {
        return true
    }

    override val keyMapping = EmulatorConst.keyMapping

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