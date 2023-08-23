package com.woohyman.xml.ui.gamegallery

import java.io.File
import java.io.FilenameFilter
import java.util.Locale

class FilenameExtFilter : FilenameFilter {
    var exts: Set<String>
    var showDir = false
    var showHiden = false

    constructor(exts: Array<String>, showDirs: Boolean, showHiden: Boolean) {
        var tmp: Set<String> = HashSet()
        tmp = tmp.plus(exts)
        showDir = showDirs
        this.showHiden = showHiden
        this.exts = addDots(tmp)
    }

    constructor(
        exts: Set<String>?, showDirs: Boolean,
        showHiden: Boolean
    ) {
        showDir = showDirs
        this.showHiden = showHiden
        this.exts = addDots(exts)
    }

    private fun addDots(exts: Set<String>?): Set<String> {
        val temp: MutableSet<String> = HashSet()
        for (ext in exts!!) {
            temp.add(".$ext")
        }
        return temp
    }

    override fun accept(dir: File, filename: String): Boolean {
        if (!showHiden && filename[0] == '.') return false
        if (showDir) {
            val f = File(dir, filename)
            if (f.isDirectory) return true
        }
        val fnLower = filename.lowercase(Locale.getDefault())
        for (ext in exts) {
            if (fnLower.endsWith(ext)) {
                return true
            }
        }
        return false
    }
}