package com.woohyman.keyboard.base

import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.mylibrary.R
import nostalgia.framework.emulator.Emulator
import nostalgia.framework.emulator.EmulatorException
import nostalgia.framework.emulator.EmulatorSettings
import nostalgia.framework.keyboard.KeyboardProfile
import nostalgia.framework.data.entity.GameInfo
import nostalgia.framework.data.entity.GfxProfile
import nostalgia.framework.data.entity.SfxProfile
import nostalgia.framework.utils.EmuUtils
import nostalgia.framework.utils.NLog
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

abstract class JniEmulator : Emulator {
    private val readyLock = Any()
    private val loadLock = Any()
    private val testX = Array(2) { ShortArray(SIZE) }
    private val sfxLock = Any()
    private val viewPortLock = Any()
    private val ready = AtomicBoolean()
    private val totalWritten = AtomicInteger()
    private var baseDir: String? = null
    private var loadFailed = false
    private var cur = 0
    private val lenX = IntArray(2)
    private var fastForward = false
    private var numFastForwardFrames = 0
    private var minSize = 0
    private val useOpenGL = false
    private var gameInfo: GameInfo? = null
    private var bitmap: Bitmap? = null
    private var sfx: SfxProfile? = null
    private var gfx: GfxProfile? = null
    private var track: AudioTrack? = null
    private var sfxBuffer: ShortArray? = null
    private val jni: JniBridge
    private var keys = 0
    private var turbos = 0.inv()
    private var viewPortWidth = 0
    private var viewPortHeight = 0

    init {
        val info = info
        KeyboardProfile.BUTTON_NAMES = info?.deviceKeyboardNames
        KeyboardProfile.BUTTON_KEY_EVENT_CODES = info?.deviceKeyboardCodes
        KeyboardProfile.BUTTON_DESCRIPTIONS = info?.deviceKeyboardDescriptions
        jni = bridge
    }

    abstract val bridge: JniBridge
    abstract override fun autoDetectGfx(game: GameDescription): GfxProfile
    abstract override fun autoDetectSfx(game: GameDescription): SfxProfile

    override val historyItemCount: Int
        get() = jni.historyItemCount

    override fun setFastForwardFrameCount(frames: Int) {
        numFastForwardFrames = frames
    }

    override fun loadHistoryState(pos: Int) {
        if (!jni.loadHistoryState(pos)) {
            throw EmulatorException("load history state failed")
        }
    }

    override fun renderHistoryScreenshot(bmp: Bitmap, pos: Int) {
        if (!jni.renderHistory(bmp, pos, bmp.width, bmp.height)) {
            throw EmulatorException("render history failed")
        }
    }

    override val isGameLoaded: Boolean
        get() = synchronized(loadLock) { return gameInfo != null }

    override val loadedGame: GameInfo?
        get() = synchronized(loadLock) { return gameInfo!! }

    override fun start(gfx: GfxProfile, sfx: SfxProfile, settings: EmulatorSettings) {
        synchronized(readyLock) {
            ready.set(false)
            setFastForwardEnabled(false)
            if (sfx != null) {
                sfxBuffer = ShortArray(sfx.bufferSize)
                initSound(sfx)
            }
            this.sfx = sfx
            this.gfx = gfx
            if (!jni.start(gfx.toInt(), sfx?.toInt() ?: -1, settings.toInt())) {
                throw EmulatorException("init failed")
            }
            synchronized(loadLock) { gameInfo = null }
            ready.set(true)
        }
    }

    override val activeGfxProfile
        get() = gfx!!

    override val activeSfxProfile
        get() = sfx!!

    override fun reset() {
        synchronized(readyLock) {
            ready.set(false)
            if (track != null) {
                track!!.flush()
            }
            synchronized(testX) {
                lenX[0] = 0
                lenX[1] = 0
            }
            if (!jni.reset()) {
                throw EmulatorException("reset failed")
            }
            ready.set(true)
        }
    }

    override fun setBaseDir(path: String) {
        baseDir = path
        if (!jni.setBaseDir(path)) {
            throw EmulatorException("could not set base dir")
        }
    }

    override fun saveState(slot: Int) {
        val fileName = SlotUtils.getSlotPath(baseDir, getMD5(null), slot)
        var screen: Bitmap? = null
        try {
            screen = Bitmap.createBitmap(
                gfx!!.originalScreenWidth,
                gfx!!.originalScreenHeight,
                Bitmap.Config.ARGB_8888
            )
        } catch (ignored: OutOfMemoryError) {
        }
        if (screen != null) {
            if (!jni.renderVP(screen, gfx!!.originalScreenWidth, gfx!!.originalScreenHeight)) {
                throw EmulatorException(R.string.act_game_screenshot_failed)
            }
        }
        if (!jni.saveState(fileName, slot)) {
            throw EmulatorException(R.string.act_emulator_save_state_failed)
        }
        if (screen != null) {
            val pngFileName = SlotUtils.getScreenshotPath(baseDir, getMD5(null), slot)
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(pngFileName)
                screen.compress(Bitmap.CompressFormat.PNG, 60, out)
            } catch (e: Exception) {
                throw EmulatorException(R.string.act_game_screenshot_failed)
            } finally {
                if (out != null) {
                    try {
                        out.flush()
                        out.close()
                    } catch (ignored: Exception) {
                    }
                }
            }
            val file = File(pngFileName)
            NLog.i(TAG, "SCREEN: " + file.length())
            screen.recycle()
        } else {
            throw EmulatorException(R.string.act_game_screenshot_failed)
        }
    }

    override fun loadState(slot: Int) {
        val fileName = SlotUtils.getSlotPath(baseDir, getMD5(null), slot)
        if (!File(fileName).exists()) {
            return
        }
        if (!jni.loadState(fileName, slot)) {
            throw EmulatorException(R.string.act_emulator_load_state_failed)
        }
    }

    override fun loadGame(fileName: String, batteryDir: String, batterySaveFullPath: String) {
        if (!jni.loadGame(fileName, batteryDir, batterySaveFullPath)) {
            synchronized(loadLock) {
                loadFailed = true
                loadLock.notifyAll()
            }
            throw EmulatorException(R.string.act_emulator_load_game_failed)
        }
        val gi = GameInfo()
        gi.path = fileName
        gi.md5 = getMD5(fileName)
        synchronized(loadLock) {
            loadFailed = false
            gameInfo = gi
            loadLock.notifyAll()
        }
    }

    override fun setKeyPressed(port: Int, key: Int, isPressed: Boolean) {
        var key = key
        val n = port * 8
        if (key >= 1000) {
            key -= 1000
            setTurboEnabled(port, key, isPressed)
        }
        keys = if (isPressed) {
            keys or (key shl n)
        } else {
            keys and (key shl n).inv()
        }
    }

    override fun setTurboEnabled(port: Int, key: Int, isEnabled: Boolean) {
        val n = port * 8
        var t = turbos.inv()
        t = if (isEnabled) {
            t or (key shl n)
        } else {
            t and (key shl n).inv()
        }
        turbos = t.inv()
    }

    override fun readPalette(result: IntArray) {
        synchronized(loadLock) {
            if (gameInfo == null && !loadFailed) {
                try {
                    loadLock.wait()
                } catch (ignored: InterruptedException) {
                }
            }
        }
        requireNotNull(result)
        if (gameInfo != null) {
            if (!jni.readPalette(result)) {
                throw EmulatorException("error reading palette")
            }
        }
    }

    override fun setViewPortSize(w: Int, h: Int) {
        if (!jni.setViewPortSize(w, h)) {
            throw EmulatorException("set view port size failed")
        }
        synchronized(viewPortLock) {
            viewPortWidth = w
            viewPortHeight = h
        }
    }

    override fun stop() {
        synchronized(readyLock) {
            ready.set(false)
            if (bitmap != null) {
                bitmap!!.recycle()
                NLog.d(TAG, "bitmap recycled")
            }
            if (track != null) {
                track!!.flush()
                track!!.stop()
                track!!.release()
                track = null
            }
            jni.stop()
            gameInfo = null
            bitmap = null
        }
    }

    override val isReady: Boolean
        get() = ready.get()

    override fun fireZapper(x: Float, y: Float) {
        val emuX: Int
        val emuY: Int
        if (x == -1f || y == -1f) {
            emuX = -1
            emuY = -1
        } else {
            emuX = (activeGfxProfile.originalScreenWidth * x).toInt()
            emuY = (activeGfxProfile.originalScreenHeight * y).toInt()
        }
        if (!jni.fireZapper(emuX, emuY)) {
            throw EmulatorException("firezapper failed")
        }
    }

    override fun resetKeys() {
        keys = 0
    }

    override fun emulateFrame(numFramesToSkip: Int) {
        var numFramesToSkip = numFramesToSkip
        if (fastForward && numFramesToSkip > -1) {
            numFramesToSkip = numFastForwardFrames
        }
        if (!jni.emulate(keys, turbos, numFramesToSkip)) {
            throw EmulatorException("emulateframe failed")
        }
    }

    override fun renderGfx() {
        if (!jni.render(bitmap)) {
            createBitmap(viewPortWidth, viewPortHeight)
            if (!jni.render(bitmap)) {
                throw EmulatorException("render failed")
            }
        }
    }

    override fun renderGfxGL() {
        if (!jni.renderGL()) {
            throw EmulatorException("render failed")
        }
    }

    override fun draw(canvas: Canvas, x: Int, y: Int) {
        check(!useOpenGL)
        if (bitmap == null || bitmap!!.isRecycled) {
            return
        }
        canvas.drawBitmap(bitmap!!, x.toFloat(), y.toFloat(), null)
    }

    override fun enableCheat(gg: String) {
        if (!jni.enableCheat(gg, 0)) {
            throw EmulatorException(R.string.act_emulator_invalid_cheat, gg)
        }
    }

    override fun enableRawCheat(addr: Int, `val`: Int, comp: Int) {
        if (!jni.enableRawCheat(addr, `val`, comp)) {
            throw EmulatorException(
                R.string.act_emulator_invalid_cheat, Integer.toHexString(addr)
                        + ":" + Integer.toHexString(`val`)
            )
        }
    }

    override fun readSfxData() {
        val length = jni.readSfxBuffer(sfxBuffer)
        var slen: Int
        var back: Int
        synchronized(testX) {
            back = cur
            slen = lenX[back]
            if (length > 0) {
                if (slen + length < SIZE) {
                    System.arraycopy(sfxBuffer, 0, testX[back], 0, length)
                    lenX[back] = length
                } else {
                    lenX[back] = 0
                }
            }
        }
    }

    override fun onEmulationResumed() {
        synchronized(sfxLock) { resetTrack() }
    }

    override fun onEmulationPaused() {}
    override fun setFastForwardEnabled(enabled: Boolean) {
        fastForward = enabled
    }

    private fun resetTrack() {
        if (track != null) {
            track!!.flush()
            track!!.write(ShortArray(minSize - 2), 0, minSize - 2)
        }
    }

    override fun renderSfx() {
        synchronized(readyLock) {
            if (track == null) {
                return
            }
            var slen: Int
            val cur = cur
            synchronized(testX) {
                slen = lenX[cur]
                if (slen > 0) {
                    lenX[cur] = 0
                    this.cur = if (cur == 0) 1 else 0
                }
            }
            if (slen > 0) {
                track!!.flush()
                track!!.write(testX[cur], 0, slen)
                totalWritten.set(slen)
            }
        }
    }

    private fun createBitmap(w: Int, h: Int) {
        if (bitmap != null) {
            bitmap!!.recycle()
        }
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    }

    private fun initSound(sfx: SfxProfile) {
        val format =
            if (sfx.isStereo) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
        val encoding =
            if (sfx.encoding == SfxProfile.SoundEncoding.PCM8) AudioFormat.ENCODING_PCM_8BIT else AudioFormat.ENCODING_PCM_16BIT
        minSize = AudioTrack.getMinBufferSize(sfx.rate, format, encoding)
        track = AudioTrack(
            AudioManager.STREAM_MUSIC, sfx.rate, format,
            encoding, minSize, AudioTrack.MODE_STREAM
        )
        try {
            track!!.play()
            resetTrack()
        } catch (e: Exception) {
            throw EmulatorException("sound init failed")
        }
        NLog.d(TAG, "sound init OK")
    }

    private fun getMD5(path: String?): String? {
        var path = path
        if (path == null) {
            path = loadedGame?.path
        }
        if (!md5s.containsKey(path)) {
            val md5 = EmuUtils.getMD5Checksum(File(path))
            md5s[path] = md5
        }
        return md5s[path]
    }

    companion object {
        private const val TAG = "JniEmulator"
        private const val SIZE = 32768 * 2
        private val md5s: MutableMap<String?, String> = HashMap()
    }
}