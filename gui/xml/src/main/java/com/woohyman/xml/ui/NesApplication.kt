package com.woohyman.xml.ui

import com.liulishuo.filedownloader.FileDownloader
import com.woohyman.xml.BaseApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NesApplication : BaseApplication() {

    override fun onCreate() {
        super.onCreate()
        FileDownloader.setupOnApplicationOnCreate(this)
    }

    override fun hasGameMenu(): Boolean {
        return true
    }
}