package com.woohyman.xml.ui

import android.content.Intent
import android.os.Bundle
import com.woohyman.xml.base.EmulatorActivity
import com.woohyman.xml.base.OpenGLTestActivity
import com.woohyman.xml.ui.gamegallery.GalleryActivity
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.keyboard.utils.EmuUtils.checkGL20Support
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.PreferenceUtil.getFragmentShader
import com.woohyman.keyboard.utils.PreferenceUtil.setFragmentShader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesGalleryActivity : GalleryActivity() {

    @Inject
    lateinit var nesEmulator: NesEmulator

    override val emulatorActivityClass: Class<out EmulatorActivity?>?
        get() = NesEmulatorActivity::class.java

    override val romExtensions: Set<String>?
        get() = kotlin.run {
            val set = HashSet<String>()
            set.add("nes")
            set.add("fds")
            return set
        }

    override val emulatorInstance: Emulator?
        get() = nesEmulator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getFragmentShader(this) == -1
            && checkGL20Support(this)
        ) {
            val intent = Intent(this, OpenGLTestActivity::class.java)
            startActivityForResult(intent, REQUEST_CHECK_OPENGL)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_OPENGL) {
            e("opengl", "opengl: $resultCode")
            setFragmentShader(this, resultCode)
        }
    }

    companion object {
        private const val REQUEST_CHECK_OPENGL = 200
    }
}