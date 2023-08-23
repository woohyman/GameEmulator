package com.woohyman.xml.ui.preferences

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.woohyman.xml.R

/**
 * Created by huzongyao on 17-11-10.
 */
class AboutActivity : AppCompatActivity() {
    private var mTextVersion: TextView? = null
    private var mTextAbout: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mTextVersion = findViewById(R.id.textview_version)
        mTextAbout = findViewById(R.id.textview_about)
        mTextAbout?.setAutoLinkMask(Linkify.ALL)
        mTextAbout?.setMovementMethod(LinkMovementMethod.getInstance())
        packageVersionInfo
    }

    private val packageVersionInfo: Unit
        private get() {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                mTextVersion!!.text = packageInfo.versionName
            } catch (ignored: Exception) {
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}