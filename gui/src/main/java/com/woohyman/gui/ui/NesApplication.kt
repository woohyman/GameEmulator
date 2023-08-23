package com.woohyman.gui.ui

import com.liulishuo.filedownloader.FileDownloader
import com.woohyman.gui.BaseApplication
import com.woohyman.keyboard.base.EmulatorHolder.setEmulatorClass
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.nes.NesEmulatorImpl
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NesApplication : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        setEmulatorClass(NesEmulatorImpl::class.java)
        FileDownloader.setupOnApplicationOnCreate(this)
    }

    override fun hasGameMenu(): Boolean {
        return true
    }
}