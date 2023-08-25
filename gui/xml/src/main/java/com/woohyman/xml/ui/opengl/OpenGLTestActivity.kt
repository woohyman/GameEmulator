package com.woohyman.xml.ui.opengl

import android.app.Activity
import android.os.Bundle

class OpenGLTestActivity : Activity(), OpenGLTestView.Callback {
    var view: OpenGLTestView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = OpenGLTestView(this, this)
        setContentView(view)
    }

    override fun onDetected(i: Int) {
        runOnUiThread {
            setResult(i)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (view != null) {
            view!!.onPause()
        }
    }
}