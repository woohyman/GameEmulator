package com.woohyman.keyboard.rom

import com.woohyman.keyboard.data.database.GameDescription

interface INesGameDataProvider {

    val fragmentShader: String

    var game: GameDescription
}