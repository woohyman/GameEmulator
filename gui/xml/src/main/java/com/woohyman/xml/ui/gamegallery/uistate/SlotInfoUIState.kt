package com.woohyman.xml.ui.gamegallery.uistate

import com.woohyman.keyboard.data.entity.SlotInfo

data class SlotInfoUIState(
    val slotInfo: SlotInfo,
    val idx: Int,
    val labelS: String,
    val messageS: String,
    val dateS: String,
    val timeS: String
)

