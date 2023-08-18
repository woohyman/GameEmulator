package nostalgia.appgg

import android.util.SparseIntArray
import nostalgia.framework.emulator.EmulatorException
import nostalgia.framework.keyboard.KeyboardProfile
import nostalgia.framework.base.JniBridge
import nostalgia.framework.base.JniEmulator
import nostalgia.framework.controllers.EmulatorController
import nostalgia.framework.controllers.KeyboardController
import nostalgia.framework.data.database.GameDescription
import nostalgia.framework.data.entity.EmulatorInfo
import nostalgia.framework.data.entity.GfxProfile
import nostalgia.framework.data.entity.SfxProfile
import nostalgia.framework.data.entity.SfxProfile.SoundEncoding
import nostalgia.framework.keyboard.BasicEmulatorInfo

class GGEmulator private constructor() : JniEmulator() {
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
        var addrVal = -1
        var valVal = -1
        gg = gg.replace("-", "")
        if (gg.startsWith("00") && gg.length == 8) {
            val addr = gg.substring(2, 5 + 1)
            val `val` = gg.substring(6)
            addrVal = addr.toInt(16)
            valVal = `val`.toInt(16)
        }
        if (addrVal < 0 || valVal < 0
            || !bridge.enableRawCheat(addrVal, valVal, -1)
        ) {
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
            get() = "Nostalgia.GG"
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
                mapping.put(EmulatorController.KEY_UP, 0x01)
                mapping.put(EmulatorController.KEY_DOWN, 0x02)
                mapping.put(EmulatorController.KEY_LEFT, 0x04)
                mapping.put(EmulatorController.KEY_RIGHT, 0x08)
                mapping.put(EmulatorController.KEY_A, 0x10)
                mapping.put(EmulatorController.KEY_B, 0x20)
                mapping.put(EmulatorController.KEY_START, 0x80)
                mapping.put(EmulatorController.KEY_SELECT, -1)
                mapping.put(EmulatorController.KEY_A_TURBO, 0x10 + 1000)
                mapping.put(EmulatorController.KEY_B_TURBO, 0x20 + 1000)
                return mapping
            }
        override val deviceKeyboardCodes: IntArray
            get() = intArrayOf(
                EmulatorController.KEY_UP,
                EmulatorController.KEY_DOWN,
                EmulatorController.KEY_RIGHT,
                EmulatorController.KEY_LEFT,
                EmulatorController.KEY_START,
                EmulatorController.KEY_A, EmulatorController.KEY_B,
                EmulatorController.KEY_A_TURBO,
                EmulatorController.KEY_B_TURBO,
                KeyboardController.KEYS_LEFT_AND_UP,
                KeyboardController.KEYS_RIGHT_AND_UP,
                KeyboardController.KEYS_RIGHT_AND_DOWN,
                KeyboardController.KEYS_LEFT_AND_DOWN,
                KeyboardController.KEY_SAVE_SLOT_0,
                KeyboardController.KEY_LOAD_SLOT_0,
                KeyboardController.KEY_SAVE_SLOT_1,
                KeyboardController.KEY_LOAD_SLOT_1,
                KeyboardController.KEY_SAVE_SLOT_2,
                KeyboardController.KEY_LOAD_SLOT_2,
                KeyboardController.KEY_MENU,
                KeyboardController.KEY_FAST_FORWARD,
                KeyboardController.KEY_BACK
            )
        override val deviceKeyboardNames: Array<String>
            get() = arrayOf(
                "UP", "DOWN", "RIGHT", "LEFT", "START", "1",
                "2", "TURBO 1", "TURBO 2", "LEFT+UP", "RIGHT+UP",
                "RIGHT+DOWN", "LEFT+DOWN", "SAVE STATE 1",
                "LOAD STATE 1",
                "SAVE STATE 2",
                "LOAD STATE 2",
                "SAVE STATE 3",
                "LOAD STATE 3",
                "MENU", "FAST FORWARD", "EXIT"
            )
        override val numQualityLevels: Int
            get() = 3

        private class GGGfxProfile : GfxProfile() {
            override fun toInt(): Int {
                return 0
            }
        }

        private class GGSfxProfile : SfxProfile() {
            override fun toInt(): Int {
                return rate
            }
        }

        companion object {
            var profiles: MutableList<GfxProfile> = ArrayList()
            var sfxProfiles: MutableList<SfxProfile> = ArrayList()

            init {
                val prof: GfxProfile = GGGfxProfile()
                prof.fps = 60
                prof.name = "default"
                prof.originalScreenWidth = 160
                prof.originalScreenHeight = 144
                profiles.add(prof)
                var sfx: SfxProfile = GGSfxProfile()
                sfx.name = "low"
                sfx.isStereo = true
                sfx.encoding = SoundEncoding.PCM16
                sfx.bufferSize = 2048 * 8 * 2
                sfx.quality = 0
                sfx.rate = 22050
                sfxProfiles.add(sfx)
                sfx = GGSfxProfile()
                sfx.name = "medium"
                sfx.isStereo = true
                sfx.encoding = SoundEncoding.PCM16
                sfx.bufferSize = 2048 * 8 * 2
                sfx.rate = 44100
                sfx.quality = 1
                sfxProfiles.add(sfx)
                sfx = GGSfxProfile()
                sfx.name = "high"
                sfx.isStereo = true
                sfx.encoding = SoundEncoding.PCM16
                sfx.bufferSize = 2048 * 8 * 2
                sfx.rate = 44100
                sfx.quality = 2
                sfxProfiles.add(sfx)
            }
        }
    }

    companion object {
        const val PACK_SUFFIX = "nggs"
        @JvmStatic
        var instance: GGEmulator? = null
            get() {
                if (field == null) {
                    field = GGEmulator()
                }
                return field
            }
            private set
        private var info: EmulatorInfo? = Info()
    }
}