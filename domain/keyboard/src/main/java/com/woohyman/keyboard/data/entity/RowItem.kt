package com.woohyman.keyboard.data.entity

import com.woohyman.keyboard.data.database.GameDescription

data class RowItem(
    var game: GameDescription? = null,
    var firstLetter: Char = 0.toChar()
)