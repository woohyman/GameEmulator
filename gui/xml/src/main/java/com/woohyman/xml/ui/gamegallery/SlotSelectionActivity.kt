package com.woohyman.xml.ui.gamegallery

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.format.DateFormat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.xml.R
import com.woohyman.xml.ui.widget.MenuItem
import com.woohyman.xml.ui.widget.PopupMenu
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.SlotInfo
import com.woohyman.keyboard.utils.NLog.i
import java.util.Calendar
import java.util.Date

class SlotSelectionActivity : AppCompatActivity() {
    var slots = arrayOfNulls<View>(8)
    var clearIcon: Drawable? = null
    var sendIcon: Drawable? = null
    var game: GameDescription? = null
    var type = 0
    var loadFocusIdx = 0
    var saveFocusIdx = 0
    private var actionBar: ActionBar? = null
    private fun initSlot(
        slotInfo: SlotInfo, idx: Int, labelS: String,
        messageS: String, dateS: String, timeS: String
    ) {
        val slotView = slots[idx]
        val isUsed = slotInfo.isUsed
        val screenshotBitmap = slotInfo.screenShot
        val label = slotView!!.findViewById<TextView>(R.id.row_slot_label)
        val message = slotView.findViewById<TextView>(R.id.row_slot_message)
        val date = slotView.findViewById<TextView>(R.id.row_slot_date)
        val time = slotView.findViewById<TextView>(R.id.row_slot_time)
        val screenshot = slotView.findViewById<ImageView>(R.id.row_slot_screenshot)
        label.text = labelS
        message.text = messageS
        date.text = dateS
        time.text = timeS
        slotView.setOnClickListener { v: View? -> onSelected(game, idx + 1, isUsed) }
        if (isUsed) {
            slotView.setOnLongClickListener { v: View? ->
                val menu = PopupMenu(this@SlotSelectionActivity)
                menu.setHeaderTitle(labelS)
                menu.setOnItemSelectedListener(object : PopupMenu.OnItemSelectedListener {
                    override fun onItemSelected(item: MenuItem?) {
                        if (item?.itemId == SEND_SLOT) {
                        }

                    }
                })
                menu.add(SEND_SLOT, R.string.act_slot_popup_menu_send).icon = sendIcon
                menu.show(slotView)
                true
            }
        }
        if (screenshotBitmap != null) {
            screenshot.setImageBitmap(screenshotBitmap)
            message.visibility = View.INVISIBLE
        }
    }

    private fun onSelected(game: GameDescription?, slot: Int, isUsed: Boolean) {
        if (type == DIALOAG_TYPE_LOAD && !isUsed) {
            return
        }
        val data = Intent()
        data.putExtra(EXTRA_GAME, game)
        data.putExtra(EXTRA_SLOT, slot)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clearIcon = resources.getDrawable(R.drawable.ic_clear_slot)
        sendIcon = resources.getDrawable(R.drawable.ic_send_slot)
        game = intent.getSerializableExtra(EXTRA_GAME) as GameDescription?
        val baseDir = intent.getStringExtra(EXTRA_BASE_DIRECTORY)
        val slotInfos = SlotUtils.getSlots(baseDir, game!!.checksum)
        type = intent.getIntExtra(EXTRA_DIALOG_TYPE_INT, DIALOAG_TYPE_LOAD)
        setContentView(R.layout.activity_slot_selection)
        actionBar = supportActionBar
        if (actionBar != null) {
            actionBar!!.setHomeButtonEnabled(true)
            actionBar!!.setDisplayHomeAsUpEnabled(true)
            actionBar!!.setTitle(if (type == DIALOAG_TYPE_LOAD) R.string.game_menu_load else R.string.game_menu_save)
        }
        slots[0] = findViewById(R.id.slot_0)
        slots[1] = findViewById(R.id.slot_1)
        slots[2] = findViewById(R.id.slot_2)
        slots[3] = findViewById(R.id.slot_3)
        slots[4] = findViewById(R.id.slot_4)
        slots[5] = findViewById(R.id.slot_5)
        slots[6] = findViewById(R.id.slot_6)
        slots[7] = findViewById(R.id.slot_7)
        val dateFormat = DateFormat.getDateFormat(this)
        val timeFormat = DateFormat.getTimeFormat(this)
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
            initSlot(slotInfo, i, label, message, dateString, timeString)
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

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @SuppressLint("HandlerLeak")
    override fun onResume() {
        super.onResume()
        val h: Handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                slots[msg.what]!!.requestFocusFromTouch()
                i(TAG, "focus item:$loadFocusIdx")
                val pref = getSharedPreferences("slot-pref", MODE_PRIVATE)
                if (!pref.contains("show")) {
                    val editor = pref.edit()
                    editor.putBoolean("show", true)
                    editor.apply()
                }
            }
        }
        h.sendEmptyMessageDelayed(
            if (type == DIALOAG_TYPE_LOAD) loadFocusIdx else saveFocusIdx,
            500
        )
    }

    companion object {
        const val EXTRA_GAME = "EXTRA_GAME"
        const val EXTRA_BASE_DIRECTORY = "EXTRA_BASE_DIR"
        const val EXTRA_SLOT = "EXTRA_SLOT"
        const val EXTRA_DIALOG_TYPE_INT = "EXTRA_DIALOG_TYPE_INT"
        const val DIALOAG_TYPE_LOAD = 1
        const val DIALOAG_TYPE_SAVE = 2
        private const val TAG = "SlotSelectionActivity"
        private const val SEND_SLOT = 0
        private const val REMOVE_SLOT = 1
    }
}