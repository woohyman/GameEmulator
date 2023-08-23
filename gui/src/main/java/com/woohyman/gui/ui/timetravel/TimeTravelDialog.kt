package com.woohyman.gui.ui.timetravel

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.woohyman.gui.R
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.EmulatorException
import java.util.Locale

class TimeTravelDialog(
    context: Context, private val manager: Manager,
    private val game: GameDescription
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar), OnSeekBarChangeListener {
    private val img: ImageView
    private val label: TextView
    private val bitmap: Bitmap
    private var max = 0

    init {
        bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val inflater = context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val content = inflater.inflate(R.layout.dialog_time_travel, null)
        setContentView(content)
        val seekBar = content.findViewById<SeekBar>(R.id.dialog_time_seek)
        seekBar.setOnSeekBarChangeListener(this)
        val cancel = content.findViewById<Button>(R.id.dialog_time_btn_cancel)
        cancel.setOnClickListener { v: View? -> cancel() }
        cancel.isFocusable = true
        img = content.findViewById(R.id.dialog_time_img)
        label = content.findViewById(R.id.dialog_time_label)
        max = manager.historyItemCount - 1
        seekBar.max = max
        seekBar.progress = max
        val ok = content.findViewById<Button>(R.id.dialog_time_wheel_btn_ok)
        ok.setOnClickListener { v: View? ->
            manager
                .startGame(game)
            manager.loadHistoryState(
                max
                        - seekBar.progress
            )
            try {
                manager.enableCheats(
                    context,
                    game
                )
            } catch (ignored: EmulatorException) {
            }
            dismiss()
        }
        ok.isFocusable = true
        manager.pauseEmulation()
        manager.renderHistoryScreenshot(bitmap, 0)
        img.setImageBitmap(bitmap)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        label.text = String.format(
            Locale.getDefault(),
            "-%02.2fs", (max - progress) / 4f
        )
        manager.renderHistoryScreenshot(bitmap, max - progress)
        img.setImageBitmap(bitmap)
        img.invalidate()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}