package com.woohyman.xml.ui.preferences

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.MenuItem
import com.woohyman.xml.R
import com.woohyman.xml.base.BaseActivity
import com.woohyman.xml.databinding.ActivityAboutBinding

class AboutActivity : BaseActivity<ActivityAboutBinding>(ActivityAboutBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        setSupportActionBar(binding.toolbar)
        binding.textviewAbout.autoLinkMask = Linkify.ALL
        binding.textviewAbout.movementMethod = LinkMovementMethod.getInstance()
        packageVersionInfo
    }

    private val packageVersionInfo: Unit
        get() {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                binding.textviewVersion.text = packageInfo.versionName
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