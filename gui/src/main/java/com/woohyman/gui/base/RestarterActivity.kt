package com.woohyman.gui.base

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.widget.TextView
import java.util.concurrent.atomic.AtomicBoolean

class RestarterActivity : Activity() {
    var thread: RestarterThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Loading..."
        setContentView(tv)
    }

    override fun onResume() {
        super.onResume()
        val pid = intent.extras!!.getInt(EXTRA_PID)
        val className = intent.extras!!.getString(EXTRA_CLASS)
        var clazz: Class<*>? = null
        try {
            clazz = Class.forName(className)
        } catch (ignored: Exception) {
        }
        var restartIntent: Intent? = null
        if (clazz != null) {
            restartIntent = Intent(this, clazz)
            restartIntent.putExtras(intent)
        }
        thread = RestarterThread(pid, restartIntent)
        thread!!.start()
    }

    override fun onBackPressed() {}
    override fun onPause() {
        super.onPause()
        if (thread != null) {
            thread!!.cancel()
        }
        finish()
    }

    inner class RestarterThread(var pid: Int, var intent: Intent?) : Thread() {
        private val cancelled = AtomicBoolean(false)
        override fun run() {
            try {
                sleep(500)
            } catch (ignored: Exception) {
            }
            Process.killProcess(pid)
            try {
                sleep(300)
            } catch (ignored: Exception) {
            }
            val activityManager = applicationContext
                .getSystemService(ACTIVITY_SERVICE) as ActivityManager
            var killed = false
            while (!killed) {
                var appProcesses: List<RunningAppProcessInfo>
                if (activityManager != null) {
                    appProcesses = activityManager.runningAppProcesses
                    killed = true
                    for (info in appProcesses) {
                        if (info.pid == pid) {
                            killed = false
                            break
                        }
                    }
                    if (!killed) {
                        try {
                            sleep(30)
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
            if (!cancelled.get()) {
                if (intent != null) {
                    intent!!.putExtra(EXTRA_AFTER_RESTART, true)
                    startActivity(intent)
                } else {
                    finish()
                }
            }
        }

        fun cancel() {
            cancelled.set(true)
        }
    }

    companion object {
        const val EXTRA_PID = "pid"
        const val EXTRA_CLASS = "class"
        const val EXTRA_AFTER_RESTART = "isAfterRestart"
    }
}