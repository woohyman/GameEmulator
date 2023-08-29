/* The following code was written by Matthew Wiggins 
 * and is released under the APACHE 2.0 license 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.woohyman.xml.ui.widget

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
    private var hack = false
    private var mSeekBar: SeekBar? = null
    private var mSplashText: TextView? = null
    private var mValueText: TextView? = null
    private val mDialogMessage: String?
    private var mSuffix: String?
    private val mDefault: Int
    private var max: Int
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
        val layout = LinearLayout(mContext)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(6, 6, 6, 6)

        mSplashText = TextView(mContext).also {
            if (mDialogMessage != null) it.text = mDialogMessage
        }

        layout.addView(mSplashText)
        mValueText = TextView(mContext).also {
            it.gravity = Gravity.CENTER_HORIZONTAL
            it.textSize = 32f
        }

        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layout.addView(mValueText, params)

        mSeekBar = SeekBar(mContext).also {
            it.setOnSeekBarChangeListener(this)
            layout.addView(
                it, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
            if (shouldPersist()) mValue = getPersistedInt(mDefault)
            it.max = max
            it.progress = mValue
        }

        return layout
    }

    override fun onBindDialogView(v: View) {
        super.onBindDialogView(v)
        mSeekBar!!.max = max
        mSeekBar!!.progress = mValue
    }

    override fun onSetInitialValue(restore: Boolean, defaultValue: Any?) {
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