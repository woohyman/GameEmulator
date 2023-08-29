package com.woohyman.keyboard.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.view.Display
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.base.EmulatorUtils
import com.woohyman.keyboard.base.SlotUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.keyboard.data.entity.EmulatorInfo
import com.woohyman.keyboard.di.EmulatorEntryPoint
import com.woohyman.keyboard.di.SingleComponent
import com.woohyman.keyboard.emulator.NesEmulator
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import dagger.hilt.android.EntryPointAccessors
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Collections
import java.util.Locale
import java.util.zip.ZipFile
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

object EmuUtils {
    private const val TAG = "utils.EmuUtils"
    private const val MD5_BYTES_COUNT = 10240
    private val size = Point()

    val emulator =
        EntryPointAccessors.fromApplication(Utils.getApp(), EmulatorEntryPoint::class.java)
            .getEmulator()

    @JvmStatic
    fun stripExtension(str: String?): String? {
        if (str == null) return null
        val pos = str.lastIndexOf(".")
        return if (pos == -1) str else str.substring(0, pos)
    }

    @JvmStatic
    fun getMD5Checksum(file: File?): String {
        var fis: InputStream? = null
        try {
            fis = FileInputStream(file)
            return countMD5(fis)
        } catch (e: IOException) {
            e(TAG, "", e)
        } finally {
            try {
                fis?.close()
            } catch (ignored: IOException) {
            }
        }
        return ""
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getMD5Checksum(zis: InputStream): String {
        return countMD5(zis)
    }

    private fun countMD5(`is`: InputStream): String {
        try {
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(MD5_BYTES_COUNT)
            var readCount = 0
            var totalCount = 0
            var updateCount = 0
            while (`is`.read(buffer).also { readCount = it } != -1) {
                updateCount = readCount
                if (totalCount + readCount > MD5_BYTES_COUNT) {
                    updateCount = MD5_BYTES_COUNT - totalCount
                }
                md.update(buffer, 0, updateCount)
                totalCount += updateCount
                if (totalCount >= MD5_BYTES_COUNT) break
            }
            return if (totalCount >= MD5_BYTES_COUNT) {
                val digest = md.digest()
                var result = ""
                for (aDigest in digest) {
                    result += Integer.toString((aDigest.toInt() and 0xff) + 0x100, 16).substring(1)
                }
                result
            } else {
                "small file"
            }
        } catch (e: NoSuchAlgorithmException) {
            e(TAG, "", e)
        } catch (e: IOException) {
            e(TAG, "", e)
        }
        return ""
    }

    fun getCrc(dir: String?, entry: String?): Long {
        return try {
            val zf = ZipFile(dir)
            val ze = zf.getEntry(entry)
            ze.crc
        } catch (e: Exception) {
            -1
        }
    }

    @JvmStatic
    fun checkGL20Support(context: Context?): Boolean {
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        egl.eglInitialize(display, version)
        val EGL_OPENGL_ES2_BIT = 4
        val configAttribs = intArrayOf(
            EGL10.EGL_RED_SIZE, 4, EGL10.EGL_GREEN_SIZE, 4, EGL10.EGL_BLUE_SIZE, 4,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(10)
        val num_config = IntArray(1)
        egl.eglChooseConfig(display, configAttribs, configs, 10, num_config)
        egl.eglTerminate(display)
        return num_config[0] > 0
    }

    @JvmStatic
    @Throws(IOException::class)
    fun extractFile(zipFile: File, entryName: String, outputFile: File) {
        i(
            TAG, "extract " + entryName + " from " + zipFile.absolutePath + " to "
                    + outputFile.absolutePath
        )
        val zipFile2 = ZipFile(zipFile)
        val ze = zipFile2.getEntry(entryName)
        if (ze != null) {
            val zis = zipFile2.getInputStream(ze)
            val fos = FileOutputStream(outputFile)
            val buffer = ByteArray(20480)
            var count: Int
            while (zis.read(buffer).also { count = it } != -1) {
                fos.write(buffer, 0, count)
            }
            zis.close()
            zipFile2.close()
            fos.close()
        }
    }

    @JvmStatic
    fun removeExt(fileName: String): String {
        val idx = fileName.lastIndexOf('.')
        return if (idx > 0) {
            fileName.substring(0, idx)
        } else {
            fileName
        }
    }

    @JvmStatic
    fun getExt(fileName: String): String {
        val idx = fileName.lastIndexOf('.')
        return if (idx > 0) {
            fileName.substring(idx + 1)
        } else {
            ""
        }
    }

    fun getDeviceType(context: Context): ServerType {
        return if (context.packageManager.hasSystemFeature("android.hardware.telephony")) {
            ServerType.mobile
        } else if (context.packageManager.hasSystemFeature("android.hardware.touchscreen")) {
            ServerType.tablet
        } else {
            ServerType.tv
        }
    }

    @JvmStatic
    fun getDisplayWidth(display: Display): Int {
        display.getSize(size)
        return size.x
    }

    @JvmStatic
    fun getDisplayHeight(display: Display): Int {
        display.getSize(size)
        return size.y
    }

    @JvmStatic
    fun isDebuggable(ctx: Context): Boolean {
        var debuggable = false
        val pm = ctx.packageManager
        try {
            val appinfo = pm.getApplicationInfo(ctx.packageName, 0)
            debuggable = 0 != ApplicationInfo.FLAG_DEBUGGABLE.let {
                appinfo.flags = appinfo.flags and it; appinfo.flags
            }
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return debuggable
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun isWifiAvailable(context: Context): Boolean {
        val manager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = manager.connectionInfo
        return (manager.wifiState == WifiManager.WIFI_STATE_ENABLED) and (wifiInfo.ipAddress != 0)
    }

    fun getBroadcastAddress(context: Context?): InetAddress? {
        val ip = getNetPrefix(context) + ".255"
        return try {
            InetAddress.getByName(ip)
        } catch (e: UnknownHostException) {
            null
        }
    }

    private val iP: IpInfo
        private get() {
            try {
                val result = IpInfo()
                val interfaces: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs: List<InetAddress> = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            var prefixLen = Int.MAX_VALUE
                            for (address in intf.interfaceAddresses) {
                                if (address.networkPrefixLength < prefixLen) {
                                    prefixLen = address.networkPrefixLength.toInt()
                                }
                            }
                            val sAddr = addr.hostAddress.uppercase(Locale.getDefault())
                            val ip = addr.address
                            val iAddr =
                                ip[0].toInt() shl 24 and -0x1000000 or (ip[1].toInt() shl 16 and 0x00FF0000
                                        ) or (ip[2].toInt() shl 8 and 0x0000FF00
                                        ) or (ip[3].toInt() shl 0 and 0x000000FF)
                            val isIPv4 = addr is Inet4Address
                            if (isIPv4) {
                                result.sAddress = sAddr
                                result.address = iAddr
                                result.setPrefixLen(prefixLen)
                                return result
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
            return IpInfo()
        }

    fun getNetPrefix(context: Context?): String {
        val info = iP
        val prefix = info.address and info.netmask
        return (prefix shr 24 and 0xff).toString() + "." + (prefix shr 16 and 0xff) + "." + (prefix shr 8 and 0xff)
    }

    fun getIpAddr(context: Context?): String? {
        return iP.sAddress
    }

    @JvmStatic
    fun createScreenshotBitmap(context: Context?, game: GameDescription): Bitmap {
        val path = SlotUtils.getScreenshotPath(EmulatorUtils.getBaseDir(context), game.checksum, 0)
        val bitmap = BitmapFactory.decodeFile(path)
        val w = bitmap.width
        val h = bitmap.height
        val newW = w * 2
        val newH = h * 2
        val from = Rect(0, 0, w, h)
        val to = Rect(0, 0, newW, newH)
        val largeBitmap = Bitmap.createBitmap(
            bitmap.width * 2,
            bitmap.height * 2, Bitmap.Config.ARGB_8888
        )
        val c = Canvas(largeBitmap)
        val p = Paint()
        p.isDither = false
        p.isFilterBitmap = false
        c.drawBitmap(bitmap, from, to, p)
        bitmap.recycle()
        return largeBitmap
    }

    fun isIntentAvailable(context: Context, action: String?): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(action)
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.size > 0
    }

    enum class ServerType {
        mobile, tablet, tv
    }

    class IpInfo {
        var sAddress: String? = null
        var address = 0
        var netmask = 0
        fun setPrefixLen(len: Int) {
            netmask = 0
            var n = 31
            for (i in 0 until len) {
                netmask = netmask or (1 shl n)
                n--
            }
            e("netmask", len.toString() + "")
            e("netmask", netmask.toString() + "")
        }
    }
}