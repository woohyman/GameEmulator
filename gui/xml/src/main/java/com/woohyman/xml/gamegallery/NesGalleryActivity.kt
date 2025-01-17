package com.woohyman.xml.gamegallery

import android.content.Intent
import android.os.Bundle
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.keyboard.utils.EmuUtils.checkGL20Support
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.PreferenceUtil.getFragmentShader
import com.woohyman.keyboard.utils.PreferenceUtil.setFragmentShader
import com.woohyman.xml.emulator.EmulatorActivity
import com.woohyman.xml.emulator.NesEmulatorActivity
import com.woohyman.xml.ui.opengl.OpenGLTestActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesGalleryActivity : GalleryActivity() {

    @Inject
    override lateinit var emulatorInstance: NesEmulator

    override val emulatorActivityClass: Class<out EmulatorActivity>
        get() = NesEmulatorActivity::class.java

    override val romExtensions: Set<String>
        get() = kotlin.run {
            val set = HashSet<String>()
            set.add("nes")
            set.add("fds")
            return set
        }

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