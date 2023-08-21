package com.woohyman.keyboard.emulator

import android.content.Context

class EmulatorException : RuntimeException {
    private var stringResId = -1
    private var formatArg: String? = null

    constructor(msg: String?) : super(msg)
    constructor(stringResId: Int) {
        this.stringResId = stringResId
    }

    constructor(stringResId: Int, t: String?) {
        this.stringResId = stringResId
        formatArg = t
    }

    fun getMessage(context: Context): String {
        if (stringResId != -1) {
            val resource = context.resources.getString(stringResId)
            return if (formatArg != null) {
                String.format(resource, formatArg)
            } else {
                resource
            }
        }
        return message!!
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}