package com.woohyman.xml.emulator

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import com.blankj.utilcode.util.ActivityUtils
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.utils.PreferenceUtil.getFragmentShader
import com.woohyman.xml.gamegallery.NesGalleryActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesEmulatorActivity : EmulatorActivity() {

    override fun onBackPressed() {
        if (checkLastStack()) {
            val intent = Intent(this@NesEmulatorActivity, NesGalleryActivity::class.java)
            startActivity(intent)
        }else{
            super.onBackPressed()
        }
    }

    private fun checkLastStack(): Boolean {
        return ActivityUtils.getActivityList().size == 1 && ActivityUtils.getTopActivity().componentName.className == this.javaClass.name
    }
}