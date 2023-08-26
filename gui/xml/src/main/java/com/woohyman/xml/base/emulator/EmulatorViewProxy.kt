package com.woohyman.xml.base.emulator

import android.content.res.Configuration
import android.view.View
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.Benchmark
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.emulator.EmulatorView
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.keyboard.utils.PreferenceUtil
import com.woohyman.xml.R
import com.woohyman.xml.ui.opengl.OpenGLView
import com.woohyman.xml.ui.widget.UnacceleratedView

class EmulatorViewProxy(
    private val activity: EmulatorActivity,
):EmulatorView {
    val emulatorView: EmulatorView by lazy {
        val paddingLeft = 0
        var paddingTop = 0
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            paddingTop = activity.resources.getDimensionPixelSize(R.dimen.top_panel_touchcontroler_height)
        }
        val shader = activity.fragmentShader
        var openGLView: OpenGLView? = null
        val hasOpenGL20 = EmuUtils.checkGL20Support(Utils.getApp())

        if (hasOpenGL20) {
            openGLView = OpenGLView(activity, activity.emulatorInstance, paddingLeft, paddingTop, shader)
            if (activity.emulatorManagerProxy.needsBenchmark) {
                openGLView.setBenchmark(Benchmark(EmulatorActivity.OPEN_GL_BENCHMARK, 200, benchmarkCallback))
            }
        }
        openGLView ?: UnacceleratedView(activity, activity.emulatorInstance, paddingLeft, paddingTop)
    }

    val benchmarkCallback: Benchmark.BenchmarkCallback = object : Benchmark.BenchmarkCallback {
        private var numTests = 0
        private var numOk = 0
        override fun onBenchmarkReset(benchmark: Benchmark) {}
        override fun onBenchmarkEnded(benchmark: Benchmark, steps: Int, totalTime: Long) {
            val millisPerFrame = totalTime / steps.toFloat()
            numTests++
            if (benchmark.name == EmulatorActivity.OPEN_GL_BENCHMARK) {
                if (millisPerFrame < 17) {
                    numOk++
                }
            }
            if (benchmark.name == EmulatorActivity.EMULATION_BENCHMARK) {
                if (millisPerFrame < 17) {
                    numOk++
                }
            }
            if (numTests == 2) {
                PreferenceUtil.setBenchmarked(activity, true)
                if (numOk == 2) {
                    emulatorView?.setQuality(2)
                    PreferenceUtil.setEmulationQuality(activity, 2)
                }
            }
        }
    }

    init {
        activity.gameControlProxy.group.addView(emulatorView.asView())
    }

    override fun onPause() {
        emulatorView.onPause()
    }

    override fun onResume() {
        emulatorView.onResume()
    }

    override fun setQuality(quality: Int) {
        emulatorView.setQuality(quality)
    }

    override val viewPort: ViewPort?
        get() = emulatorView.viewPort

    override fun asView(): View {
        return emulatorView.asView()
    }

}