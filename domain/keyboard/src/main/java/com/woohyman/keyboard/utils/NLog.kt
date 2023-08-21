package com.woohyman.keyboard.utils

import android.util.Log

object NLog {
    private var WTF = true
    private var E = true
    private var W = true
    private var D = false
    private var I = false
    private var V = false
    fun setDebugMode(debug: Boolean) {
        if (debug) {
            WTF = true
            E = true
            W = true
            D = true
            I = true
            V = true
        }
    }

    @JvmStatic
    fun e(tag: String?, msg: String?) {
        if (E) Log.e(tag, msg!!)
    }

    @JvmStatic
    fun e(tag: String?, msg: String?, e: Throwable?) {
        if (E) Log.e(tag, msg, e)
    }

    @JvmStatic
    fun d(tag: String?, msg: String?) {
        if (D) Log.d(tag, msg!!)
    }

    @JvmStatic
    fun w(tag: String?, msg: String?) {
        if (W) Log.w(tag, msg!!)
    }

    @JvmStatic
    fun i(tag: String?, msg: String?) {
        if (I) Log.i(tag, msg!!)
    }

    fun v(tag: String?, msg: String?) {
        if (V) Log.i(tag, msg!!)
    }

    @JvmStatic
    fun wtf(tag: String?, msg: String?) {
        if (WTF) Log.wtf(tag, msg)
    }

    fun wtf(tag: String?, msg: String?, th: Throwable?) {
        if (WTF) Log.wtf(tag, msg, th)
    }
}