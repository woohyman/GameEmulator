package com.woohyman.xml.permission

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.StringUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.R
import java.io.File
import java.util.Timer
import java.util.TimerTask

class FileAccessPermissionDialogFragment private constructor(private val permissionSuccess: () -> Unit) :
    DialogFragment() {

    companion object {
        const val GET_ALL_FILE_ACCESS = 673
        const val GET_ROM_PATH_ACCESS = 674

        fun showFragment(
            manager: FragmentManager,
            tag: String,
            permissionSuccess: () -> Unit
        ) {
            if (isStorageAccess && romPathFile != null) {
                permissionSuccess.invoke()
            } else {
                FileAccessPermissionDialogFragment(permissionSuccess).show(manager, tag)
            }
        }

        val romPathFile: File?
            get() {
                val preferences =
                    Utils.getApp().getSharedPreferences("user", AppCompatActivity.MODE_PRIVATE)

                val path = preferences.getString("path", "")
                NLog.e("test999", "path file??? ===> " + path)

                if (StringUtils.isEmpty(path) || path == null) {
                    return null
                }
                val file = File(path)
                NLog.e("test999", "file??? ===> " + file.exists())
                return if (file.exists()) {
                    file
                } else null
            }

        val isStorageAccess
            get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                PermissionUtils.isGranted(PermissionConstants.STORAGE)
            }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("授权文件访问权限")
            .setMessage("搜索本地ROM需要授权文件访问权限,并指定ROM文件所在目录")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                fetchStoragePermission()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                NLog.e("test999", "isStorageAccess ===> " + getString(R.string.cancel))
                dismiss()
            }
            .create()
    }

    private fun fetchStoragePermission() {
        NLog.e("test999", "isStorageAccess ===> " + isStorageAccess)
        NLog.e("test999", "romPathFile ===> " + romPathFile)
        if (!isStorageAccess) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity?.startActivityForResult(intent, GET_ALL_FILE_ACCESS)
            } else {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        startWithPermission()
                    }
                }, 800L)
            }
        } else {
            NLog.e("test999", "startActivityForResult sssssssssssssssssssssssssssssss")
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            activity?.startActivityForResult(intent, GET_ROM_PATH_ACCESS)
        }

    }

    private fun startWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    val intent = Intent()
                    intent.action = getString(R.string.action_gallery_page)
                    startActivity(intent)
                    dismiss()
                }

                override fun onDenied() {
                    dismiss()
                }
            }).request()
    }
}