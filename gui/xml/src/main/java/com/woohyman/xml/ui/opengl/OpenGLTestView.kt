package com.woohyman.xml.ui.opengl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.woohyman.xml.ui.opengl.OpenGLView.Renderer.Companion.loadShader
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.utils.NLog.i
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class OpenGLTestView(context: Context?, callback: Callback) : GLSurfaceView(context) {
    private val renderer: Renderer

    init {
        setEGLContextClientVersion(2)
        renderer = Renderer(callback)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    interface Callback {
        fun onDetected(i: Int)
    }

    private class Renderer(private val callback: Callback) : GLSurfaceView.Renderer {
        private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
        var textureIds = IntArray(2)
        var paletteTextureId = 0
        var mainTextureId = 0
        private var detected = false
        private var screenWidth = 0
        private var screenHeight = 0
        private var positionHandle = 0
        private var texCoordHandle = 0
        private var textureHandle = 0
        private var paletteHandle = 0
        private var mvpMatrixHandle = 0
        private var program = 0
        private val projMatrix = FloatArray(16)
        var viewPort: ViewPort? = null
            private set
        private var textureBuffer: FloatBuffer? = null
        private var testBuffer: ByteBuffer? = null
        private var vertexBuffer: FloatBuffer? = null
        private var drawListBuffer: ShortBuffer? = null
        private lateinit var quadCoords: FloatArray
        private lateinit var textureCoords: FloatArray
        override fun onDrawFrame(gl: GL10) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            GLES20.glVertexAttribPointer(
                positionHandle,
                3,
                GLES20.GL_FLOAT,
                false,
                3 * 4,
                vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                texCoordHandle,
                2,
                GLES20.GL_FLOAT,
                false,
                2 * 4,
                textureBuffer
            )
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projMatrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mainTextureId)
            GLES20.glUniform1i(textureHandle, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, paletteTextureId)
            GLES20.glUniform1i(paletteHandle, 1)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D, 0, 0, 0, 256, 256,
                GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, testBuffer
            )
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer
            )
            val pixels = ByteBuffer.allocate(4).order(ByteOrder.nativeOrder())
            GLES20.glReadPixels(
                screenHeight / 2, screenWidth / 2, 1, 1,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels
            )
            if (!detected) {
                val intBuf = pixels.asIntBuffer()
                val array = IntArray(1)
                intBuf[array]
                val value = intBuf[0] and 0x000000ff
                i("pix", "pix: " + Integer.toHexString(value))
                detected = true
                if (value == 0) {
                    i("pix", "on detect: 0")
                    callback.onDetected(0)
                } else if (value == 0xff) {
                    callback.onDetected(1)
                    i("pix", "on detect: 1")
                } else {
                    callback.onDetected(2)
                    i("pix", "on detect: 2")
                }
            }
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
        }

        override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
            Matrix.orthoM(
                projMatrix, 0, -width / 2f, +width / 2f,
                -height / 2f, +height / 2f, -2f, 2f
            )
            screenWidth = width
            screenHeight = height
            GLES20.glViewport(0, 0, width, height)
            initQuadCoordinates(width, height)
            GLES20.glUseProgram(program)
            initQuadCoordinates(width, height)
            GLES20.glUseProgram(program)
            positionHandle = GLES20.glGetAttribLocation(program, "a_position")
            textureHandle = GLES20.glGetUniformLocation(program, "s_texture")
            paletteHandle = GLES20.glGetUniformLocation(program, "s_palette")
            texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord")
        }

        override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            initTextures()
        }

        private fun initQuadCoordinates(width: Int, height: Int) {
            viewPort = ViewPort()
            viewPort!!.height = height
            viewPort!!.width = width
            quadCoords = floatArrayOf(
                -width / 2f, -height / 2f, 0f,
                -width / 2f, height / 2f, 0f,
                width / 2f, height / 2f, 0f,
                width / 2f, -height / 2f, 0f
            )
            textureCoords = floatArrayOf(
                0f, 1f,
                0f, 0f,
                1f, 0f,
                1f, 1f
            )
            val bb0 = ByteBuffer.allocateDirect(256 * 256)
            bb0.order(ByteOrder.nativeOrder())
            val pixels = ByteArray(256 * 256)
            for (i in 0 until 256 * 256) {
                pixels[i] = 132.toByte()
            }
            testBuffer = bb0
            testBuffer!!.put(pixels)
            testBuffer!!.position(0)
            val bb1 = ByteBuffer.allocateDirect(quadCoords.size * 4)
            bb1.order(ByteOrder.nativeOrder())
            vertexBuffer = bb1.asFloatBuffer()
            vertexBuffer?.put(quadCoords)
            vertexBuffer?.position(0)
            val bb2 = ByteBuffer.allocateDirect(textureCoords.size * 4)
            bb2.order(ByteOrder.nativeOrder())
            textureBuffer = bb2.asFloatBuffer()
            textureBuffer?.put(textureCoords)
            textureBuffer?.position(0)
            val dlb = ByteBuffer.allocateDirect(drawOrder.size * 2)
            dlb.order(ByteOrder.nativeOrder())
            drawListBuffer = dlb.asShortBuffer()
            drawListBuffer?.put(drawOrder)
            drawListBuffer?.position(0)
        }

        private fun initTextures() {
            val paletteSize = 256
            GLES20.glGenTextures(2, textureIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, 256,
                256, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1])
            val palette = IntArray(paletteSize)
            for (i in 0 until paletteSize) {
                val c = if (i % 2 == 0) 0x0 else 0xff
                palette[i] = -0x1000000 or (c shl 16) or (c shl 8) or c
            }
            GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            val paletteBmp = Bitmap.createBitmap(paletteSize, paletteSize, Bitmap.Config.ARGB_8888)
            paletteBmp.setPixels(palette, 0, paletteSize, 0, 0, paletteSize, 1)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, paletteBmp, 0)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE
            )
            mainTextureId = textureIds[0]
            paletteTextureId = textureIds[1]
        }

        companion object {
            private const val vertexShaderCode = ("attribute vec4 a_position; "
                    + "attribute vec2 a_texCoord;  								 "
                    + "uniform mat4 uMVPMatrix;   								 "
                    + "varying lowp vec2 v_texCoord;   						     "
                    + "void main()                  							 "
                    + "{                            							 "
                    + "   gl_Position =  uMVPMatrix  * a_position; 				 "
                    + "   v_texCoord = a_texCoord;  							 "
                    + "}                            							 ")
            private const val fragmentShaderCode =
                ("precision mediump float;                                  "
                        + "uniform sampler2D s_texture;                              "
                        + "uniform sampler2D s_palette;                              "
                        + "void main()                     							 "
                        + "{                            							 "
                        + " float a = texture2D(s_texture, vec2(0, 0)).a;        "
                        + " float c = floor((a * 256.0) / 127.5);                    "
                        + " float x = a - c * 0.001953;                               "
                        + " vec2 curPt = vec2(x, 0);                                  "
                        + " gl_FragColor.rgb = texture2D(s_palette, curPt).rgb;"
                        + "}                            							 ")
        }
    }
}