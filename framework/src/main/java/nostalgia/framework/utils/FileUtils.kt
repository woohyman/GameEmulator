package nostalgia.framework.utils

import android.content.Context
import android.os.Environment
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

object FileUtils {
    @Throws(IOException::class)
    fun readAsset(context: Context, asset: String?): String {
        val `is` = context.assets.open(asset!!)
        val reader = BufferedReader(InputStreamReader(`is`))
        return try {
            var line: String?
            val buffer = StringBuilder()
            while (reader.readLine().also { line = it } != null) {
                buffer.append(line)
            }
            buffer.toString()
        } finally {
            reader.close()
        }
    }

    fun getFileNameWithoutExt(file: File): String {
        val name = file.name
        val lastIdx = name.lastIndexOf(".")
        return if (lastIdx == -1) {
            name
        } else name.substring(0, lastIdx)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(from: File?, to: File?) {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(from)
            copyFile(fis, to)
        } finally {
            fis?.close()
        }
    }

    @Throws(IOException::class)
    fun copyFile(`is`: InputStream, to: File?) {
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(to)
            var count: Int
            val buffer = ByteArray(1024)
            while (`is`.read(buffer).also { count = it } != -1) {
                fos.write(buffer, 0, count)
            }
        } finally {
            fos?.close()
        }
    }

    @Throws(IOException::class)
    fun cleanDirectory(directory: File?) {
        if (directory != null) {
            val files = directory.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    cleanDirectory(file)
                }
                file.delete()
            }
        }
    }

    @Throws(IOException::class)
    fun saveStringToFile(text: String, file: File?) {
        val fos = FileOutputStream(file)
        fos.write(text.toByteArray())
        fos.close()
    }

    fun loadFileToString(file: File?): String {
        return try {
            val reader = BufferedReader(FileReader(file))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            sb.toString()
        } catch (e: IOException) {
            ""
        }
    }

    @JvmStatic
    val isSDCardRWMounted: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state
        }
}