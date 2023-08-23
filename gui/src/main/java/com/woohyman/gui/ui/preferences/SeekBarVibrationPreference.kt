package com.woohyman.gui.ui.preferences

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.util.AttributeSet
import android.widget.SeekBar
import com.woohyman.gui.ui.widget.SeekBarPreference

class SeekBarVibrationPreference(context: Context?, attrs: AttributeSet?) : SeekBarPreference(
    context!!, attrs!!
) {
    private val vibrator: Vibrator

    init {
        vibrator = getContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        negativeButtonText = ""
    }

    override fun onStopTrackingTouch(seek: SeekBar) {
        super.onStopTrackingTouch(seek)
        vibrator.vibrate((seek.progress * 10).toLong())
    }

    override fun showDialog(state: Bundle) {
        super.showDialog(state)
    }
}