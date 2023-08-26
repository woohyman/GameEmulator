package com.woohyman.xml.ui

import android.app.ActivityManager
import android.content.Intent
import android.os.Bundle
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.keyboard.utils.PreferenceUtil.getFragmentShader
import com.woohyman.xml.base.emulator.EmulatorActivity
import com.woohyman.xml.base.emulator.EmulatorMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NesEmulatorActivity : EmulatorActivity() {

    @Inject
    override lateinit var emulatorInstance: NesEmulator

    private var isLastOfStack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLastOfStack = checkLastStack()
    }

    var shader1 = ("precision mediump float;"
            + "varying vec2 v_texCoord;"
            + "uniform sampler2D s_texture;"
            + "uniform sampler2D s_palette; "
            + "void main()"
            + "{           "
            + "		 float a = texture2D(s_texture, v_texCoord).a;"
            + "	     float c = floor((a * 256.0) / 127.5);"
            + "      float x = a - c * 0.001953;"
            + "      vec2 curPt = vec2(x, 0);"
            + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
            + "}")
    var shader2 = ("precision mediump float;"
            + "varying vec2 v_texCoord;"
            + "uniform sampler2D s_texture;"
            + "uniform sampler2D s_palette; "
            + "void main()"
            + "{"
            + "		 float a = texture2D(s_texture, v_texCoord).a;"
            + "		 float x = a;	"
            + "		 vec2 curPt = vec2(x, 0);"
            + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
            + "}")

    override val fragmentShader: String
        get() = kotlin.run {
            val shaderIdx = getFragmentShader(this)
            return if (shaderIdx == 1) {
                shader2
            } else shader1
        }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isLastOfStack) {
            val intent = Intent(this, NesGalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkLastStack(): Boolean {
        val mngr = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val taskList = mngr.getRunningTasks(10)
        return taskList[0].numActivities == 1 && taskList[0].topActivity!!.className == this.javaClass.name
    }
}