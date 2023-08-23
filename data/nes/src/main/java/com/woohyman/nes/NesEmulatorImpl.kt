package com.woohyman.nes

import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.data.entity.SfxProfile
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.nes.util.EmulatorConst
import com.woohyman.nes.util.EmulatorConst.cleanNames
import com.woohyman.nes.util.EmulatorConst.pal
import java.util.Locale
import javax.inject.Inject

class NesEmulatorImpl @Inject constructor(
    override val info: NesEmulatorInfo,
    override val bridge: Core,
) : NesEmulator() {

    override fun autoDetectGfx(game: GameDescription): GfxProfile {
        val name = game.cleanName.lowercase(Locale.getDefault())
        cleanNames.forEach {
            if (name.contains(it)) {
                return pal
            }
        }

        for (palKeyword in EmulatorConst.palExclusiveKeywords) {
            if (palKeyword.startsWith("$")) {
                palKeyword.substring(1).also {
                    if (name.startsWith(it)) {
                        return pal
                    }
                }
            } else {
                var kws = listOf(palKeyword)
                if (palKeyword.startsWith(".")) {
                    palKeyword.substring(1).also {
                        kws = it.split("\\|".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toList()
                    }
                }
                kws.forEach {
                    if (name.contains(it)) {
                        return pal
                    }
                }
            }
        }

        return if (listOf(*EmulatorConst.palHashes).contains(game.checksum)) {
            pal
        } else info.defaultGfxProfile
    }

    override fun autoDetectSfx(game: GameDescription): SfxProfile = info.defaultSfxProfile
}