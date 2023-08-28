package com.woohyman.xml.gamegallery.viewmodel

import android.content.Intent
import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.xml.gamegallery.Constants.DIALOAG_TYPE_LOAD
import com.woohyman.xml.gamegallery.Constants.EXTRA_BASE_DIRECTORY
import com.woohyman.xml.gamegallery.Constants.EXTRA_DIALOG_TYPE_INT
import com.woohyman.xml.gamegallery.Constants.EXTRA_GAME
import com.woohyman.xml.gamegallery.uistate.SlotInfoUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SlotSelectionViewModel @Inject constructor(

) : ViewModel() {
    var loadFocusIdx = 0
    var saveFocusIdx = 0

    //存档信息
    private val _curShareFlow = MutableSharedFlow<SlotInfoUIState>(replay = 1)
    val curShareFlow: SharedFlow<SlotInfoUIState> = _curShareFlow

    var baseDir: String? = null
    var game: GameDescription? = null
    var type: Int = 0

    fun onCreate(intent: Intent) {
        game = intent.getSerializableExtra(EXTRA_GAME) as GameDescription?
        baseDir = intent.getStringExtra(EXTRA_BASE_DIRECTORY)
        type = intent.getIntExtra(EXTRA_DIALOG_TYPE_INT, DIALOAG_TYPE_LOAD)

        val slotInfos = SlotUtils.getSlots(baseDir, game?.checksum)
        val dateFormat = DateFormat.getDateFormat(Utils.getApp())
        val timeFormat = DateFormat.getTimeFormat(Utils.getApp())
        val dd = Calendar.getInstance()
        dd[1970, 10] = 10
        var emptyDate = dateFormat.format(dd.time)
        emptyDate = emptyDate.replace("1970", "----")
        emptyDate = emptyDate.replace('0', '-')
        emptyDate = emptyDate.replace('1', '-')
        var focusTime: Long = 0
        saveFocusIdx = -1
        for (i in 0 until SlotUtils.NUM_SLOTS) {
            var message = "EMPTY"
            val slotInfo = slotInfos[i]
            if (slotInfo.isUsed) {
                message = "USED"
            }
            val label = "SLOT  " + (i + 1)
            val time = Date(slotInfo.lastModified)
            val dateString =
                if (slotInfo.lastModified == -1L) emptyDate else dateFormat.format(time)
            val timeString = if (slotInfo.lastModified == -1L) "--:--" else timeFormat.format(time)
            viewModelScope.launch {
                _curShareFlow.emit(
                    SlotInfoUIState(
                        slotInfo,
                        i,
                        label,
                        message,
                        dateString,
                        timeString
                    )
                )
            }
            if (focusTime < slotInfo.lastModified) {
                loadFocusIdx = i
                focusTime = slotInfo.lastModified
            }
            if (!slotInfo.isUsed && saveFocusIdx == -1) {
                saveFocusIdx = i
            }
        }
        if (loadFocusIdx < 0) loadFocusIdx = 0
        if (saveFocusIdx < 0) saveFocusIdx = 0
    }

    val curSlotIndex
        get() = if (type == DIALOAG_TYPE_LOAD) loadFocusIdx else saveFocusIdx

    fun onResume() {
        val pref =
            Utils.getApp().getSharedPreferences("slot-pref", AppCompatActivity.MODE_PRIVATE)
        if (!pref.contains("show")) {
            val editor = pref.edit()
            editor.putBoolean("show", true)
            editor.apply()
        }
    }
}