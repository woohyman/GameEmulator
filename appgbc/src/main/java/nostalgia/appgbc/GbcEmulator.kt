package nostalgia.appgbc

import android.util.SparseIntArray
import nostalgia.framework.emulator.EmulatorException
import nostalgia.framework.keyboard.KeyboardProfile
import nostalgia.framework.base.JniBridge
import nostalgia.framework.base.JniEmulator
import nostalgia.framework.controllers.EmulatorController
import nostalgia.framework.data.database.GameDescription
import nostalgia.framework.data.entity.EmulatorInfo
import nostalgia.framework.data.entity.GfxProfile
import nostalgia.framework.data.entity.SfxProfile
import nostalgia.framework.data.entity.SfxProfile.SoundEncoding
import nostalgia.framework.keyboard.BasicEmulatorInfo

class GbcEmulator private constructor() : JniEmulator() {
    override val info: EmulatorInfo?
        get() {
            if (Companion.info == null) {
                Companion.info = Info()
            }
            return Companion.info
        }
    override val bridge: JniBridge
        get() = Core.getInstance()

    override fun enableCheat(gg: String) {
        var gg = gg
        var type = -1
        gg = gg.replace("-", "")
        if (gg.length == 9 || gg.length == 6) {
            type = 0
            val part1 = gg.substring(0, 3)
            val part2 = gg.substring(3, 6)
            gg = "$part1-$part2"
            if (gg.length == 6) {
                val part3 = gg.substring(6, 9)
                gg = "$gg-$part3"
            }
        } else if (gg.startsWith("01") && gg.length == 8) {
            type = 1
        }
        if (type == -1 || !bridge.enableCheat(gg, type)) {
            throw EmulatorException(R.string.act_emulator_invalid_cheat, gg)
        }
    }

    override fun autoDetectGfx(game: GameDescription): GfxProfile {
        return info!!.defaultGfxProfile!!
    }

    override fun autoDetectSfx(game: GameDescription): SfxProfile {
        return info!!.defaultSfxProfile!!
    }

    private class Info : BasicEmulatorInfo() {
        override fun hasZapper(): Boolean {
            return false
        }

        override val isMultiPlayerSupported: Boolean
            get() = false
        override val name: String
            get() = "Nostalgia.GBC"
        override val defaultGfxProfile: GfxProfile
            get() = profiles[0]
        override val defaultSfxProfile: SfxProfile
            get() = sfxProfiles[0]
        override val defaultKeyboardProfile: KeyboardProfile?
            get() = null
        override val availableGfxProfiles: List<GfxProfile>
            get() = profiles
        override val availableSfxProfiles: List<SfxProfile>
            get() = sfxProfiles

        override fun supportsRawCheats(): Boolean {
            return false
        }

        override val keyMapping: SparseIntArray
            get() {
                val mapping = SparseIntArray()
                mapping.put(EmulatorController.KEY_A, 0x01)
                mapping.put(EmulatorController.KEY_B, 0x02)
                mapping.put(EmulatorController.KEY_SELECT, 0x04)
                mapping.put(EmulatorController.KEY_START, 0x08)
                mapping.put(EmulatorController.KEY_UP, 0x40)
                mapping.put(EmulatorController.KEY_DOWN, 0x80)
                mapping.put(EmulatorController.KEY_LEFT, 0x20)
                mapping.put(EmulatorController.KEY_RIGHT, 0x10)
                mapping.put(EmulatorController.KEY_A_TURBO, 0x01 + 1000)
                mapping.put(EmulatorController.KEY_B_TURBO, 0x02 + 1000)
                return mapping
            }
        override val numQualityLevels: Int
            get() = 2

        private class GbcGfxProfile : GfxProfile() {
            override fun toInt(): Int {
                return 0
            }
        }

        private class GbcSfxProfile : SfxProfile() {
            override fun toInt(): Int {
                return 0
            }
        }

        companion object {
            var profiles: MutableList<GfxProfile> = ArrayList()
            var sfxProfiles: MutableList<SfxProfile> = ArrayList()

            init {
                val prof: GfxProfile = GbcGfxProfile()
                prof.fps = 60
                prof.name = "default"
                prof.originalScreenWidth = 160
                prof.originalScreenHeight = 144
                profiles.add(prof)
                val sfx: SfxProfile = GbcSfxProfile()
                sfx.name = "default"
                sfx.isStereo = true
                sfx.encoding = SoundEncoding.PCM16
                sfx.bufferSize = 2048 * 8
                sfx.rate = 22050
                sfxProfiles.add(sfx)
            }
        }
    }

    companion object {
        const val PACK_SUFFIX = "ngbcs"
        @JvmStatic
        var instance: GbcEmulator? = null
            get() {
                if (field == null) {
                    field = GbcEmulator()
                }
                return field
            }
            private set
        private var info: EmulatorInfo? = Info()
    }
}