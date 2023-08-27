package com.woohyman.xml.ui.timetravel

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.woohyman.keyboard.base.Manager
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.emulator.EmulatorException
import com.woohyman.xml.R
import com.woohyman.xml.databinding.DialogTimeTravelBinding
import java.util.Locale

class TimeTravelDialog(
    context: Context,
    private val manager: Manager,
    private val game: GameDescription
) : Dialog(context, android.R.style.Theme_Translucent_NoTitleBar), OnSeekBarChangeListener {

    private val bitmap: Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    private var max = 0
    private val binding by lazy {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val content = inflater.inflate(R.layout.dialog_time_travel, null)
        DialogTimeTravelBinding.bind(content)
    }

    init {
        setContentView(binding.root)
        (binding.dialogTimeSeek as SeekBar).setOnSeekBarChangeListener(this)
        binding.dialogTimeBtnCancel.setOnClickListener { cancel() }
        binding.dialogTimeBtnCancel.isFocusable = true
        max = manager.historyItemCount - 1
        (binding.dialogTimeSeek as SeekBar).max = max
        (binding.dialogTimeSeek as SeekBar).progress = max
        binding.dialogTimeWheelBtnOk.setOnClickListener {
            manager.startGame(game)
            manager.loadHistoryState(max - (binding.dialogTimeSeek as SeekBar).progress)
            try {
                manager.enableCheats(context, game)
            } catch (ignored: EmulatorException) {
            }
            dismiss()
        }
        binding.dialogTimeWheelBtnOk.isFocusable = true
        manager.pauseEmulation()
        manager.renderHistoryScreenshot(bitmap, 0)
        binding.dialogTimeImg.setImageBitmap(bitmap)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        binding.dialogTimeLabel.text = String.format(Locale.getDefault(), "-%02.2fs", (max - progress) / 4f)
        manager.renderHistoryScreenshot(bitmap, max - progress)
        binding.dialogTimeImg.setImageBitmap(bitmap)
        binding.dialogTimeImg.invalidate()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}
}