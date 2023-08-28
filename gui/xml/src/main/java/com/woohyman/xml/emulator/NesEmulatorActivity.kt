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

    @Inject
    override lateinit var emulatorInstance: NesEmulator

    private var shader1 =
        ("precision mediump float;" + "varying vec2 v_texCoord;" + "uniform sampler2D s_texture;" + "uniform sampler2D s_palette; " + "void main()" + "{           " + "		 float a = texture2D(s_texture, v_texCoord).a;" + "	     float c = floor((a * 256.0) / 127.5);" + "      float x = a - c * 0.001953;" + "      vec2 curPt = vec2(x, 0);" + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;" + "}")

    private var shader2 =
        ("precision mediump float;" + "varying vec2 v_texCoord;" + "uniform sampler2D s_texture;" + "uniform sampler2D s_palette; " + "void main()" + "{" + "		 float a = texture2D(s_texture, v_texCoord).a;" + "		 float x = a;	" + "		 vec2 curPt = vec2(x, 0);" + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;" + "}")

    override val fragmentShader: String
        get() = kotlin.run {
            val shaderIdx = getFragmentShader(this)
            return if (shaderIdx == 1) {
                shader2
            } else shader1
        }

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