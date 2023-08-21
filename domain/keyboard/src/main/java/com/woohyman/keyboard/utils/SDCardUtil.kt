/**
 * Based on post in http://stackoverflow.com/questions/5694933/find-an-external-sd-card-location
 * author: http://stackoverflow.com/users/565319/richard
 */
package com.woohyman.keyboard.utils

import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.Locale
import java.util.Scanner

object SDCardUtil {
    const val SD_CARD = "sdCard"
    const val EXTERNAL_SD_CARD = "externalSdCard"
    private const val TAG = "utils.SDCardUtil"
    val isAvailable: Boolean
        /**
         * @return True if the external storage is available. False otherwise.
         */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }
    val sdCardPath: String
        get() = Environment.getExternalStorageDirectory().path + "/"
    val isWritable: Boolean
        /**
         * @return True if the external storage is writable. False otherwise.
         */
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }
    @JvmStatic
    val allStorageLocations: HashSet<File>
        /**
         * @return A map of all storage locations available
         */
        get() {
            val sdcards = HashSet<String>(3)
            sdcards.add("/mnt/sdcard")
            try {
                val mountFile = File("/proc/mounts")
                if (mountFile.exists()) {
                    val scanner = Scanner(mountFile)
                    while (scanner.hasNext()) {
                        val line = scanner.nextLine()
                        val lineLower = line.lowercase(Locale.getDefault()) // neukladat lower
                        // primo do
                        // line, protoze
                        // zbytek line je
                        // case sensitive
                        if (lineLower.contains("vfat") || lineLower.contains("exfat") ||
                            lineLower.contains("fuse") || lineLower.contains("sdcardfs")
                        ) {
                            val lineElements =
                                line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            val path = lineElements[1]
                            sdcards.add(path)
                        }
                    }
                }
            } catch (e: Exception) {
                NLog.e(TAG, "", e)
            }
            getSDcardsPath(sdcards)
            val result = HashSet<File>(sdcards.size)
            for (mount in sdcards) {
                val root = File(mount)
                if (root.exists() && root.isDirectory && root.canRead()) {
                    result.add(root)
                }
            }
            return result
        }

    /**
     * Copy from
     * http://www.javacodegeeks.com/2012/10/android-finding-sd-card-path.html
     *
     * @return
     */
    private fun getSDcardsPath(set: HashSet<String>) {
        val file = File("/system/etc/vold.fstab")
        var fr: FileReader? = null
        var br: BufferedReader? = null
        try {
            fr = FileReader(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        try {
            if (fr != null) {
                val defaultExternalStorage = Environment.getExternalStorageDirectory().absolutePath
                br = BufferedReader(fr)
                var s = br.readLine()
                while (s != null) {
                    if (s.startsWith("dev_mount")) {
                        val tokens =
                            s.split("\\s".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val path = tokens[2] // mount_point
                        if (defaultExternalStorage != path) {
                            set.add(path)
                            break
                        }
                    }
                    s = br.readLine()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fr?.close()
                br?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * http://svn.apache.org/viewvc/commons/proper/io/trunk/src/main/java/org/
     * apache/commons/io/FileUtils.java?view=markup
     *
     * @param file
     * @return
     * @throws IOException
     */
    @Throws(IOException::class)
    fun isSymlink(file: File?): Boolean {
        if (file == null) throw NullPointerException("File must not be null")
        val canon: File
        canon = if (file.parent == null) {
            file
        } else {
            val canonDir = file.parentFile.canonicalFile
            File(canonDir, file.name)
        }
        return canon.canonicalFile != canon.absoluteFile
    }
}