package com.woohyman.xml.emulator

import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.rom.INesGameDataProvider
import com.woohyman.keyboard.utils.PreferenceUtil
import javax.inject.Inject

class NesGameDataProvider @Inject constructor() : INesGameDataProvider {

    private var shader1 =
        ("precision mediump float;" + "varying vec2 v_texCoord;" + "uniform sampler2D s_texture;" + "uniform sampler2D s_palette; " + "void main()" + "{           " + "		 float a = texture2D(s_texture, v_texCoord).a;" + "	     float c = floor((a * 256.0) / 127.5);" + "      float x = a - c * 0.001953;" + "      vec2 curPt = vec2(x, 0);" + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;" + "}")

    private var shader2 =
        ("precision mediump float;" + "varying vec2 v_texCoord;" + "uniform sampler2D s_texture;" + "uniform sampler2D s_palette; " + "void main()" + "{" + "		 float a = texture2D(s_texture, v_texCoord).a;" + "		 float x = a;	" + "		 vec2 curPt = vec2(x, 0);" + "      gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;" + "}")

    override val fragmentShader: String
        get() = kotlin.run {
            val shaderIdx = PreferenceUtil.getFragmentShader(Utils.getApp())
            return if (shaderIdx == 1) {
                shader2
            } else shader1
        }

    override var game: GameDescription = GameDescription()

}