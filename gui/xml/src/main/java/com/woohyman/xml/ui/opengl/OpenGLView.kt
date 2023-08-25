package com.woohyman.xml.ui.opengl

import android.annotation.SuppressLint
import android.app.Application
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.view.View
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.base.ViewUtils.loadOrComputeViewPort
import com.woohyman.keyboard.emulator.Emulator
import com.woohyman.keyboard.utils.NLog
import com.woohyman.keyboard.emulator.EmulatorView
import com.woohyman.xml.base.emulator.EmulatorActivity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
internal class OpenGLView(
    context: EmulatorActivity, emulator: Emulator, paddingLeft: Int,
    paddingTop: Int, shader: String
) : GLSurfaceView(context), EmulatorView {
    private val renderer: Renderer

    init {
        setEGLContextClientVersion(2)
        renderer = Renderer(context, emulator, paddingLeft, paddingTop, shader)
        renderer.textureSize = context.gLTextureSize
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun asView(): View {
        return this
    }

    fun setBenchmark(benchmark: com.woohyman.keyboard.base.Benchmark?) {
        renderer.benchmark = benchmark
    }

    override fun onResume() {
        super.onResume()
        if (renderer.benchmark != null) {
            renderer.benchmark!!.reset()
        }
    }

    override fun setQuality(quality: Int) {
        renderer.setQuality(quality)
    }

    override val viewPort: ViewPort?
        get() = renderer.viewPort!!

    internal class Renderer(
        context: EmulatorActivity, private val emulator: Emulator, private val paddingLeft: Int,
        private val paddingTop: Int, shader: String
    ) : GLSurfaceView.Renderer {
        val VERTEX_STRIDE = COORDS_PER_VERTEX * 4
        val TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4
        private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
        var benchmark: com.woohyman.keyboard.base.Benchmark? = null
        private val hasPalette: Boolean
        private val context: Application
        private val textureBounds: IntArray?
        var textureSize = 256
            set
        var viewPort: com.woohyman.keyboard.base.ViewPort? = null
            private set
        private var textureHandle = 0
        private var texCoordHandle = 0
        private var paletteHandle = 0
        private var positionHandle = 0
        private var mvpMatrixHandle = 0
        private var mainTextureId = 0
        private var paletteTextureId = 0
        private var startTime: Long = 0
        private var program = 0
        private var quadCoords: FloatArray = FloatArray(0)
        private var textureCoords: FloatArray = FloatArray(0)
        private val projMatrix = FloatArray(16)
        private var vertexBuffer: FloatBuffer? = null
        private var textureBuffer: FloatBuffer? = null
        private var drawListBuffer: ShortBuffer? = null
        private var delayPerFrame = 40

        init {
            hasPalette = context.hasGLPalette()
            this.context = context.application
            textureBounds = context.getTextureBounds(emulator)
            NLog.i("SHADER", "shader: $shader")
            fragmentShaderCode = shader
        }

        fun setQuality(quality: Int) {
            delayPerFrame = if (quality == 2) 17 else 40
        }

        override fun onDrawFrame(unused: GL10) {
            if (benchmark != null) {
                benchmark!!.notifyFrameEnd()
            }
            val endTime = System.currentTimeMillis()
            val delay = delayPerFrame - (endTime - startTime)
            if (delay > 0) {
                try {
                    Thread.sleep(delay)
                } catch (ignored: InterruptedException) {
                }
            }
            if (benchmark != null) {
                benchmark!!.notifyFrameStart()
            }
            startTime = System.currentTimeMillis()
            render()
        }

        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
            val vp = loadOrComputeViewPort(
                context, emulator, width, height,
                paddingLeft, paddingTop, false
            )
            viewPort = vp
            Matrix.orthoM(
                projMatrix, 0, -vp!!.width / 2f, +vp.width / 2f, -vp.height / 2f,
                +vp.height / 2f, -2f, 2f
            )
            val nvpy = height - vp.y - vp.height
            GLES20.glViewport(vp.x, nvpy, vp.width, vp.height)
            initQuadCoordinates(emulator, vp.width, vp.height)
            GLES20.glUseProgram(program)
            positionHandle = GLES20.glGetAttribLocation(program, "a_position")
            textureHandle = GLES20.glGetUniformLocation(program, "s_texture")
            if (hasPalette) {
                paletteHandle = GLES20.glGetUniformLocation(program, "s_palette")
            }
            texCoordHandle = GLES20.glGetAttribLocation(program, "a_texCoord")
            startTime = System.currentTimeMillis()
        }

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
            program = GLES20.glCreateProgram()
            GLES20.glAttachShader(program, vertexShader)
            GLES20.glAttachShader(program, fragmentShader)
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] != GLES20.GL_TRUE) {
                val log = GLES20.glGetProgramInfoLog(program)
                throw RuntimeException("glLinkProgram failed. $log#")
            }
            initTextures()
        }

        private fun render() {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(texCoordHandle)
            checkGlError("handles")
            GLES20.glVertexAttribPointer(
                positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, VERTEX_STRIDE, vertexBuffer
            )
            GLES20.glVertexAttribPointer(
                texCoordHandle, COORDS_PER_TEXTURE, GLES20.GL_FLOAT,
                false, TEXTURE_STRIDE, textureBuffer
            )
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, projMatrix, 0)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mainTextureId)
            GLES20.glUniform1i(textureHandle, 0)
            if (hasPalette) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, paletteTextureId)
                GLES20.glUniform1i(paletteHandle, 1)
            }
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            checkGlError("uniforms")
            emulator.renderGfxGL()
            checkGlError("emu render")
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer
            )
            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(texCoordHandle)
            checkGlError("disable vertex arrays")
        }

        private fun initQuadCoordinates(emulator: Emulator, width: Int, height: Int) {
            val maxTexX: Int
            val maxTexY: Int
            if (textureBounds == null) {
                val gfx = emulator.activeGfxProfile
                maxTexX = gfx!!.originalScreenWidth
                maxTexY = gfx.originalScreenHeight
            } else {
                maxTexX = textureBounds[0]
                maxTexY = textureBounds[1]
            }
            val textureSize = textureSize
            quadCoords = floatArrayOf(
                -width / 2f, -height / 2f, 0f,
                -width / 2f, height / 2f, 0f,
                width / 2f, height / 2f, 0f,
                width / 2f, -height / 2f, 0f
            )
            textureCoords = floatArrayOf(
                0f,
                maxTexY / textureSize.toFloat(),
                0f,
                0f,
                maxTexX / textureSize.toFloat(),
                0f,
                maxTexX / textureSize.toFloat(),
                maxTexY / textureSize.toFloat()
            )
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
            val numTextures = if (hasPalette) 2 else 1
            val textureIds = IntArray(numTextures)
            val textureWidth = textureSize
            val textureHeight = textureSize
            val paletteSize = 256
            GLES20.glGenTextures(numTextures, textureIds, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_ALPHA, textureWidth,
                textureHeight, 0, GLES20.GL_ALPHA, GLES20.GL_UNSIGNED_BYTE, null
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
            if (hasPalette) {
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[1])
                val palette = IntArray(paletteSize)
                emulator.readPalette(palette)
                for (i in 0 until paletteSize) {
                    val dd = palette[i]
                    val b = dd and 0x00FF0000 shr 16
                    val g = dd and 0x0000FF00 shr 8
                    val r = dd and 0x000000FF shr 0
                    palette[i] = -0x1000000 or (r shl 16) or (g shl 8) or b
                }
                GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1)
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
                val paletteBmp =
                    Bitmap.createBitmap(paletteSize, paletteSize, Bitmap.Config.ARGB_8888)
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
                paletteTextureId = textureIds[1]
            }
            mainTextureId = textureIds[0]
            checkGlError("textures")
        }

        companion object {
            const val COORDS_PER_VERTEX = 3
            const val COORDS_PER_TEXTURE = 2
            private const val vertexShaderCode = ("attribute vec4 a_position; "
                    + "attribute vec2 a_texCoord;  								 "
                    + "uniform mat4 uMVPMatrix;   								 "
                    + "varying highp vec2 v_texCoord;   						 "
                    + "void main()                  							 "
                    + "{                            							 "
                    + "   gl_Position =  uMVPMatrix  * a_position; 				 "
                    + "   v_texCoord = a_texCoord;  							 "
                    + "}                            							 ")
            private var fragmentShaderCode: String = ""
            private fun checkGlError(glOperation: String) {
                var error: Int
                while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                    NLog.e(TAG, "$glOperation: glError $error")
                    throw RuntimeException("$glOperation: glError $error")
                }
            }

            @JvmStatic
            fun loadShader(type: Int, shaderCode: String?): Int {
                val shader = GLES20.glCreateShader(type)
                GLES20.glShaderSource(shader, shaderCode)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    val log = GLES20.glGetShaderInfoLog(shader)
                    throw RuntimeException("glCompileShader failed. t: $type $log#")
                }
                return shader
            }
        }
    }

    companion object {
        private const val TAG = "base.OpenGLView"
    }
}