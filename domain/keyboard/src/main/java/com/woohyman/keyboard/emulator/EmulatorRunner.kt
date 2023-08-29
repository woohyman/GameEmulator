package com.woohyman.keyboard.emulator

import android.content.Context
import android.graphics.Bitmap
import android.os.Process
import com.woohyman.keyboard.base.BatterySaveUtils
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.SfxProfile
import com.woohyman.keyboard.utils.EmuUtils.emulator
import com.woohyman.keyboard.utils.FileUtils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import java.io.File
import java.io.FilenameFilter
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/*管理模拟器运行*/
open class EmulatorRunner(context: Context) {
    protected val lock = Any()

    @JvmField
    protected var context: Context
    private val pauseLock = Any()
    private var audioEnabled = false
    private var benchmark: Benchmark? = null
    private val sfxReadyLock = Any()
    private var sfxReady = false
    private var audioPlayer: AudioPlayer? = null
    private val isPaused = AtomicBoolean()
    private var updater: EmulatorThread? = null
    private var notRespondingListener: OnNotRespondingListener? = null

    init {
        emulator.setBaseDir(EmulatorUtils.getBaseDir(context))
        this.context = context
        fixBatterySaveBug()
    }

    private fun fixBatterySaveBug() {
        if (PreferenceUtil.isBatterySaveBugFixed(context)) {
            return
        }
        val dir = context.externalCacheDir ?: return
        val filter = FilenameFilter { dir1: File?, filename: String ->
            filename.lowercase(Locale.getDefault()).endsWith(".sav")
        }
        val cacheDir = context.externalCacheDir!!.absolutePath
        val baseDir = EmulatorUtils.getBaseDir(context)
        val fileNames = dir.list(filter)
        for (filename in fileNames) {
            val source = File(cacheDir, filename)
            val dest = File(baseDir, filename)
            try {
                FileUtils.copyFile(source, dest)
                source.delete()
                NLog.d("SAV", "copying: $source $dest")
            } catch (ignored: Exception) {
            }
        }
        PreferenceUtil.setBatterySaveBugFixed(context)
    }

    fun destroy() {
        if (audioPlayer != null) {
            audioPlayer!!.destroy()
        }
        if (updater != null) {
            updater!!.destroy()
        }
    }

    fun setOnNotRespondingListener(listener: OnNotRespondingListener?) {
        notRespondingListener = listener
    }

    fun pauseEmulation() {
        synchronized(pauseLock) {
            if (!isPaused.get()) {
                NLog.i(TAG, "--PAUSE EMULATION--")
                isPaused.set(true)
                emulator.onEmulationPaused()
                updater!!.pause()
                saveAutoState()
            }
        }
    }

    fun resumeEmulation() {
        synchronized(pauseLock) {
            if (isPaused.get()) {
                NLog.i(TAG, "--UNPAUSE EMULATION--")
                emulator.onEmulationResumed()
                updater!!.unpause()
                isPaused.set(false)
            }
        }
    }

    fun stopGame() {
        if (audioPlayer != null) {
            audioPlayer!!.destroy()
        }
        if (updater != null) {
            updater!!.destroy()
        }
        saveAutoState()
        synchronized(lock) { emulator.stop() }
    }

    fun resetEmulator() {
        synchronized(lock) { emulator.reset() }
    }

    fun startGame(game: GameDescription) {
        isPaused.set(false)
        if (updater != null) {
            updater!!.destroy()
        }
        if (audioPlayer != null) {
            audioPlayer!!.destroy()
        }
        synchronized(lock) {
            val gfx = PreferenceUtil.getVideoProfile(context, emulator, game)
            PreferenceUtil.setLastGfxProfile(context, gfx!!)
            val settings = EmulatorSettings()
            settings.zapperEnabled = PreferenceUtil.isZapperEnabled(context, game.checksum)
            settings.historyEnabled = PreferenceUtil.isTimeshiftEnabled(context)
            settings.loadSavFiles = PreferenceUtil.isLoadSavFiles(context)
            settings.saveSavFiles = PreferenceUtil.isSaveSavFiles(context)
            val profiles = emulator.info!!.availableSfxProfiles
            var sfx: SfxProfile?
            var desiredQuality = PreferenceUtil.getEmulationQuality(context)
            settings.quality = desiredQuality
            desiredQuality = Math.min(profiles!!.size - 1, desiredQuality)
            sfx = profiles[desiredQuality]
            if (!PreferenceUtil.isSoundEnabled(context)) {
                sfx = null
            }
            audioEnabled = sfx != null
            emulator.start(gfx, sfx!!, settings)
            val battery = context.externalCacheDir!!.absolutePath
            NLog.e("bat", battery)
            BatterySaveUtils.createSavFileCopyIfNeeded(context, game.path)
            val batteryDir = BatterySaveUtils.getBatterySaveDir(context, game.path)
            val possibleBatteryFileFullPath = (batteryDir + "/"
                    + FileUtils.getFileNameWithoutExt(File(game.path))
                    + ".sav")
            emulator.loadGame(
                game.path, batteryDir,
                possibleBatteryFileFullPath
            )
            emulator.emulateFrame(0)
        }
        updater = EmulatorThread()
        updater!!.setFps(emulator.activeGfxProfile!!.fps)
        updater!!.start()
        if (audioEnabled) {
            audioPlayer = AudioPlayer()
            audioPlayer!!.start()
        }
    }

    fun enableCheat(gg: String?) {
        checkGameLoaded()
        synchronized(lock) { emulator.enableCheat(gg!!) }
    }

    fun enableRawCheat(addr: Int, `val`: Int, comp: Int) {
        checkGameLoaded()
        synchronized(lock) { emulator.enableRawCheat(addr, `val`, comp) }
    }

    open fun saveState(slot: Int) {
        if (emulator.isGameLoaded) {
            synchronized(lock) { emulator.saveState(slot) }
        }
    }

    val historyItemCount: Int
        get() {
            synchronized(lock) { return emulator.historyItemCount }
        }

    fun loadHistoryState(pos: Int) {
        synchronized(lock) {
            emulator.emulateFrame(-1)
            emulator.loadHistoryState(pos)
        }
    }

    fun renderHistoryScreenshot(bmp: Bitmap?, pos: Int) {
        synchronized(lock) { emulator.renderHistoryScreenshot(bmp!!, pos) }
    }

    fun loadState(slot: Int?) {
        if (slot == null) {
            return
        }
        checkGameLoaded()
        synchronized(lock) {
            emulator.emulateFrame(-1)
            emulator.loadState(slot)
        }
    }

    private fun checkGameLoaded() {
        if (!emulator.isGameLoaded) {
            throw EmulatorException("unexpected")
        }
    }

    private fun saveAutoState() {
        saveState(AUTO_SAVE_SLOT)
    }

    fun setBenchmark(benchmark: Benchmark?) {
        this.benchmark = benchmark
    }

    interface OnNotRespondingListener {
        fun onNotResponding()
    }

    private inner class EmulatorThread : Thread() {
        private var totalSkipped = 0
        private var expectedTimeE1: Long = 0
        private var exactDelayPerFrameE1 = 0
        private var currentFrame: Long = 0
        private var startTime: Long = 0
        private var isPaused = true
        private val isRunning = AtomicBoolean(true)
        private val pauseLock = Any()
        private var delayPerFrame = 0
        fun setFps(fps: Int) {
            exactDelayPerFrameE1 = (1000 / fps.toFloat() * 10).toInt()
            delayPerFrame = (exactDelayPerFrameE1 / 10f + 0.5).toInt()
        }

        override fun run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY)
            name = "emudroid:gameLoop #" + (Math.random() * 1000).toInt()
            NLog.i(TAG, "$name started")
            var skippedTime: Long = 0
            totalSkipped = 0
            unpause()
            expectedTimeE1 = 0
            var cnt = 0
            var afterSkip = 0
            while (isRunning.get()) {
                if (benchmark != null) {
                    benchmark!!.notifyFrameEnd()
                }
                var time1 = System.currentTimeMillis()
                synchronized(pauseLock) {
                    while (isPaused) {
                        try {
                            pauseLock.wait()
                        } catch (ignored: InterruptedException) {
                        }
                        if (benchmark != null) {
                            benchmark!!.reset()
                        }
                        time1 = System.currentTimeMillis()
                    }
                }
                var numFramesToSkip = 0
                val realTime = time1 - startTime
                val diff = expectedTimeE1 / 10 - realTime
                val delay = +diff
                if (delay > 0) {
                    try {
                        sleep(delay)
                    } catch (ignored: Exception) {
                    }
                } else {
                    try {
                        sleep(1)
                    } catch (ignored: Exception) {
                    }
                }
                skippedTime = -diff
                if (afterSkip > 0) {
                    afterSkip--
                }
                if (skippedTime >= delayPerFrame * 3 && afterSkip == 0) {
                    numFramesToSkip = (skippedTime / delayPerFrame).toInt() - 1
                    val originalSkipped = numFramesToSkip
                    numFramesToSkip = Math.min(originalSkipped, 8)
                    expectedTimeE1 += (numFramesToSkip * exactDelayPerFrameE1).toLong()
                    totalSkipped += numFramesToSkip
                }
                if (benchmark != null) {
                    benchmark!!.notifyFrameStart()
                }
                synchronized(lock) {
                    if (emulator.isReady) {
                        emulator.emulateFrame(numFramesToSkip)
                        cnt += 1 + numFramesToSkip
                        if (audioEnabled && cnt >= 3) {
                            emulator.readSfxData()
                            synchronized(sfxReadyLock) {
                                sfxReady = true
                                sfxReadyLock.notifyAll()
                            }
                            cnt = 0
                        }
                    }
                }
                currentFrame += (1 + numFramesToSkip).toLong()
                expectedTimeE1 += exactDelayPerFrameE1.toLong()
            }
            NLog.i(TAG, "$name finished")
        }

        fun unpause() {
            synchronized(pauseLock) {
                startTime = System.currentTimeMillis()
                currentFrame = 0
                expectedTimeE1 = 0
                isPaused = false
                pauseLock.notifyAll()
            }
        }

        fun pause() {
            synchronized(pauseLock) { isPaused = true }
        }

        override fun destroy() {
            isRunning.set(false)
            unpause()
        }
    }

    private inner class AudioPlayer : Thread() {
        protected var isRunning = AtomicBoolean()
        override fun run() {
            isRunning.set(true)
            name = "emudroid:audioReader"
            while (isRunning.get()) {
                synchronized(sfxReadyLock) {
                    while (!sfxReady) {
                        try {
                            sfxReadyLock.wait()
                        } catch (e: Exception) {
                            return
                        }
                    }
                    sfxReady = false
                }
                if (emulator.isReady) {
                    emulator.renderSfx()
                }
            }
        }

        override fun destroy() {
            isRunning.set(false)
            interrupt()
        }
    }

    companion object {
        private const val TAG = "EmulatorRunner"
        private const val AUTO_SAVE_SLOT = 0
    }
}