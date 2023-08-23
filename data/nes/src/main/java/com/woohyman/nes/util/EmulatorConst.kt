package com.woohyman.nes.util

import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.data.entity.SfxProfile
import com.woohyman.nes.NesEmulatorInfo

object EmulatorConst {

    val palExclusiveKeywords = arrayOf(
        ".beauty|beast",
        ".hammerin|harry", ".noah|ark", ".rockets|rivals",
        ".formula|sensation", ".trolls|crazyland", "asterix", "elite",
        "smurfs", "international cricket", "turrican", "valiant",
        "aladdin", "aussie rules", "banana prince", "chevaliers",
        "crackout", "devil world", "kick off", "hyper soccer", "ufouria",
        "lion king", "gimmick", "dropzone", "drop zone", "\$mario bros",
        "road fighter", "rodland", "parasol stars", "parodius",
        "over horizon", "championship rally", "aussio rules"
    )

    val palHashes = arrayOf(
        "85ce1107c922600990884d63c75cfec4",
        "6f6d5cc27354e1527fc88ec97c8b7c27",
        "83c8b2142884965c2214196f3f71f6ec",
        "caf9d44ae71fa8ade852fb453d797798",
        "fe36a09cd6c94916d48ea61776978cc8",
        "3eb49813c3c5b6088bfed3f1d7ecaa0e",
        "b40b25a9bc54eb8f46310fae45723759",
        "d91a5f3e924916eb16bb6a3255f532bc"
    )

    val cleanNames = arrayOf(
        "(e)",
        "(europe)",
        "(f)",
        "(g)",
        "(i)",
        "(pal)",
        "[e]",
        "[f]",
        "[g]",
        "[i]",
        "[europe]",
        "[pal]"
    )

    val keyMapping = mapOf(
        EmulatorController.KEY_A to 0x01,
        EmulatorController.KEY_B to 0x02,
        EmulatorController.KEY_SELECT to 0x04,
        EmulatorController.KEY_START to 0x08,
        EmulatorController.KEY_UP to 0x10,
        EmulatorController.KEY_DOWN to 0x20,
        EmulatorController.KEY_LEFT to 0x40,
        EmulatorController.KEY_RIGHT to 0x80,
        EmulatorController.KEY_A_TURBO to 0x01 + 1000,
        EmulatorController.KEY_B_TURBO to 0x02 + 1000,
    )

    val pal = NesEmulatorInfo.NesGfxProfile(
        fps = 50,
        name = "PAL",
        originalScreenWidth = 256,
        originalScreenHeight = 240,
    )

    val ntsc = NesEmulatorInfo.NesGfxProfile(
        fps = 60,
        name = "NTSC",
        originalScreenWidth = 256,
        originalScreenHeight = 224,
    )

    val low = NesEmulatorInfo.NesSfxProfile(
        name = "low",
        bufferSize = 2048 * 8 * 2,
        encoding = SfxProfile.SoundEncoding.PCM16,
        isStereo = true,
        rate = 11025,
        quality = 0,
    )

    val medium = NesEmulatorInfo.NesSfxProfile(
        name = "medium",
        bufferSize = 2048 * 8 * 2,
        encoding = SfxProfile.SoundEncoding.PCM16,
        isStereo = true,
        rate = 22050,
        quality = 1,
    )

    val high = NesEmulatorInfo.NesSfxProfile(
        name = "high",
        bufferSize = 2048 * 8 * 2,
        encoding = SfxProfile.SoundEncoding.PCM16,
        isStereo = true,
        rate = 44100,
        quality = 2,
    )
}