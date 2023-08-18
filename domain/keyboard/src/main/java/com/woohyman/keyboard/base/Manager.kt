package com.woohyman.keyboard.base

import android.content.Context
import com.woohyman.keyboard.data.database.GameDescription
import nostalgia.framework.emulator.Emulator
import nostalgia.framework.emulator.EmulatorException
import com.woohyman.keyboard.emulator.EmulatorRunner
import com.woohyman.mylibrary.R
import nostalgia.framework.utils.FileUtils
import nostalgia.framework.utils.NLog
import java.io.File
import java.util.Locale

class Manager(emulator: Emulator?, context: Context?) : EmulatorRunner(
    emulator!!, context!!
) {
    fun setFastForwardEnabled(enabled: Boolean) {
        emulator.setFastForwardEnabled(enabled)
    }

    fun setFastForwardFrameCount(frames: Int) {
        emulator.setFastForwardFrameCount(frames)
    }

    fun copyAutoSave(slot: Int) {
        if (!emulator.isGameLoaded) {
            throw EmulatorException("game not loaded")
        }
        val md5: String? = emulator.loadedGame?.md5
        val base = EmulatorUtils.getBaseDir(context)
        val source = SlotUtils.getSlotPath(base, md5, 0)
        val target = SlotUtils.getSlotPath(base, md5, slot)
        val sourcePng = SlotUtils.getScreenshotPath(base, md5, 0)
        val targetPng = SlotUtils.getScreenshotPath(base, md5, slot)
        try {
            FileUtils.copyFile(File(source), File(target))
            FileUtils.copyFile(File(sourcePng), File(targetPng))
        } catch (e: Exception) {
            throw EmulatorException(R.string.act_emulator_save_state_failed)
        }
    }

    fun enableCheats(ctx: Context?, game: GameDescription): Int {
        var numCheats = 0
        for (cheatChars in Cheat.getAllEnableCheats(ctx, game.checksum)) {
            if (cheatChars.contains(":")) {
                if (EmulatorHolder.info!!.supportsRawCheats()) {
                    var rawValues: IntArray? = null
                    rawValues = try {
                        Cheat.rawToValues(cheatChars)
                    } catch (e: Exception) {
                        throw EmulatorException(
                            R.string.act_emulator_invalid_cheat, cheatChars
                        )
                    }
                    enableRawCheat(rawValues?.get(0)!!, rawValues?.get(1)!!, rawValues?.get(2)!!)
                } else {
                    throw EmulatorException(R.string.act_emulator_invalid_cheat, cheatChars)
                }
            } else {
                enableCheat(cheatChars.uppercase(Locale.getDefault()))
            }
            numCheats++
        }
        return numCheats
    }

    fun benchMark() {
        emulator.reset()
        val t1 = System.currentTimeMillis()
        for (i in 0..2999) {
            emulator.emulateFrame(0)
            try {
                Thread.sleep(2)
            } catch (ignored: Exception) {
            }
        }
        val t2 = System.currentTimeMillis()
        NLog.e("benchmark", "bechmark: " + (t2 - t1) / 1000f)
    }
}