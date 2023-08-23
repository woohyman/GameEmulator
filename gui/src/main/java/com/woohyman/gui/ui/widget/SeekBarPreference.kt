/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.woohyman.gui.ui.widget

import android.content.Context
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView

open class SeekBarPreference(private val mContext: Context, attrs: AttributeSet) : DialogPreference(
    mContext, attrs
), OnSeekBarChangeListener {
    var hack = false
    private var mSeekBar: SeekBar? = null
    private var mSplashText: TextView? = null
    private var mValueText: TextView? = null
    private val mDialogMessage: String?
    private var mSuffix: String?
    private val mDefault: Int
    var max: Int
    private var mValue = 0
    private val mShowText = true

    init {
        mDialogMessage = attrs.getAttributeValue(ANDROID_NS, "dialogMessage")
        mSuffix = attrs.getAttributeValue(ANDROID_NS, "text")
        if (mSuffix == "[hack]") {
            hack = true
            mSuffix = ""
        }
        mDefault = attrs.getAttributeIntValue(ANDROID_NS, "defaultValue", 0)
        max = attrs.getAttributeIntValue(ANDROID_NS, "max", 100)
    }

    override fun onCreateDialogView(): View {
        val params: LinearLayout.LayoutParams
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(6, 6, 6, 6)
        mSplashText = TextView(mContext)
        if (mDialogMessage != null) mSplashText!!.text = mDialogMessage
        layout.addView(mSplashText)
        mValueText = TextView(mContext)
        mValueText!!.gravity = Gravity.CENTER_HORIZONTAL
        mValueText!!.textSize = 32f
        params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(mValueText, params)
        mSeekBar = SeekBar(mContext)
        mSeekBar!!.setOnSeekBarChangeListener(this)
        layout.addView(
            mSeekBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        if (shouldPersist()) mValue = getPersistedInt(mDefault)
        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue
        return layout
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue
    }

    override fun onSetInitialValue(restore: Boolean, defaultValue: Any) {
        super.onSetInitialValue(restore, defaultValue)
        mValue =
            if (restore) (if (shouldPersist()) getPersistedInt(mDefault) else 0) else (defaultValue as Int)
    }

    override fun onProgressChanged(seek: SeekBar, value: Int, fromTouch: Boolean) {
        val t = (value + if (hack) 1 else 0).toString()
        mValueText!!.text = if (mSuffix == null) t else t + mSuffix
        if (shouldPersist()) persistInt(value)
        callChangeListener(value)
    }

    override fun onStartTrackingTouch(seek: SeekBar) {}
    override fun onStopTrackingTouch(seek: SeekBar) {}
    var progress: Int
        get() = mValue
        set(progress) {
            mValue = progress
            if (mSeekBar != null) mSeekBar!!.progress = progress
        }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}