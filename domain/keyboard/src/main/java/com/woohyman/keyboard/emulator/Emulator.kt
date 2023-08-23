package com.woohyman.keyboard.emulator

import android.graphics.Bitmap
import android.graphics.Canvas
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.EmulatorInfo
import com.woohyman.keyboard.data.entity.GameInfo
import com.woohyman.keyboard.data.entity.GfxProfile
import com.woohyman.keyboard.data.entity.SfxProfile

interface Emulator {
    val info: EmulatorInfo
    fun start(cfg: GfxProfile, sfx: SfxProfile, settings: EmulatorSettings)
    val activeGfxProfile: GfxProfile?
    val activeSfxProfile: SfxProfile?
    fun reset()
    fun saveState(slot: Int)
    fun loadState(slot: Int)
    fun loadHistoryState(pos: Int)
    val historyItemCount: Int
    fun renderHistoryScreenshot(bmp: Bitmap, pos: Int)
    fun setBaseDir(baseDir: String)
    fun loadGame(fileName: String, batterySaveDir: String, batterySaveFullPath: String)
    fun onEmulationResumed()
    fun onEmulationPaused()
    fun enableCheat(gg: String)
    fun enableRawCheat(addr: Int, `val`: Int, comp: Int)
    val isGameLoaded: Boolean
    val loadedGame: GameInfo?
    fun setKeyPressed(port: Int, key: Int, isPressed: Boolean)
    fun setTurboEnabled(port: Int, key: Int, isEnabled: Boolean)
    fun setViewPortSize(w: Int, h: Int)
    fun resetKeys()
    fun fireZapper(x: Float, y: Float)
    fun setFastForwardEnabled(enabled: Boolean)
    fun setFastForwardFrameCount(frames: Int)
    fun emulateFrame(numFramesToSkip: Int)
    fun readSfxData()
    fun renderSfx()
    fun readPalette(palette: IntArray)
    fun renderGfx()
    fun renderGfxGL()
    fun draw(canvas: Canvas, x: Int, y: Int)
    fun stop()
    val isReady: Boolean
    fun autoDetectGfx(game: GameDescription): GfxProfile?
    fun autoDetectSfx(game: GameDescription): SfxProfile?
}