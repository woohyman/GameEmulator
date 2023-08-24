package com.woohyman.xml.ui.gamegallery.data

import com.woohyman.keyboard.data.database.GameDescription

object TestRemoteRomSource {
    val remoteRomList = arrayListOf(
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Super%20Mario%20Bros.%203.nes"
            it.name = "超级马里奥兄弟3"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Boy%20and%20His%20Blob,%20A%20-%20Trouble%20on%20Blobolonia.nes"
            it.name = "Boy and His Blob, A - Trouble on Blobolonia.nes"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Blaster%20Master.nes"
            it.name = "Blaster Master"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Blades%20of%20Steel.nes"
            it.name = "Blades of Steel"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Bionic%20Commando.nes"
            it.name = "Bionic Commando"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Baseball%20Stars.nes"
            it.name = "Baseball Stars"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Astyanax.nes"
            it.name = "Astyanax"
        },
        GameDescription().also {
            it.url = "https://gitee.com/popvan/nes-repo/raw/master/roms/Adventures%20of%20Lolo%203.nes"
            it.name = "Adventures of Lolo 3"
        },
    )
}