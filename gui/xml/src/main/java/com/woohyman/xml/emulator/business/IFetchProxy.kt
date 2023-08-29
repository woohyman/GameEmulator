package com.woohyman.xml.emulator.business

import com.woohyman.keyboard.data.database.GameDescription

interface IFetchProxy {

    val fragmentShader: String

    val game: GameDescription
}