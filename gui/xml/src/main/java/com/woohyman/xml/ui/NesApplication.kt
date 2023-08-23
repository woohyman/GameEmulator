package com.woohyman.xml.ui

import com.liulishuo.filedownloader.FileDownloader
import com.woohyman.xml.BaseApplication
import com.woohyman.keyboard.base.EmulatorHolder.setEmulatorClass
import com.woohyman.keyboard.emulator.NesEmulator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NesApplication : BaseApplication() {
    @Inject
    lateinit var nesEmulator: NesEmulator

    override fun onCreate() {
        super.onCreate()
        setEmulatorClass(nesEmulator)
        FileDownloader.setupOnApplicationOnCreate(this)
    }

    override fun hasGameMenu(): Boolean {
        return true
    }
}