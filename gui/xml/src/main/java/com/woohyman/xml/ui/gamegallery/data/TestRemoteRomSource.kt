package com.woohyman.xml.ui.gamegallery.data

import com.woohyman.keyboard.data.database.GameDescription

object TestRemoteRomSource {
    val remoteRomList = arrayListOf(
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Super%20Mario%20Bros.%203.nes"
            it.name = "超级马里奥兄弟3"
        }
    )
}