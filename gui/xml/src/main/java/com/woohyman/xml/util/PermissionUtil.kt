package com.woohyman.xml.util

import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.Utils
import java.io.File

object PermissionUtil {

    //romPathFile文件夹路径是否已经选定
    val romPathFile: File?
        get() {
            val preferences =
                Utils.getApp().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)

            val path = preferences.getString("path", "")

            if (StringUtils.isEmpty(path) || path == null) {
                return null
            }
            val file = File(path)

            return if (file.exists()) {
                file
            } else null
        }

    //文件权限是否已经授予
    val isStorageAccess
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            PermissionUtils.isGranted(PermissionConstants.STORAGE)
        }
}