package com.woohyman.xml.permission

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.woohyman.keyboard.utils.NLog
import com.woohyman.xml.R
import com.woohyman.xml.gamegallery.IPermissionManager
import com.woohyman.xml.gamegallery.PermissionManager
import com.woohyman.xml.util.PermissionUtil.isStorageAccess
import com.woohyman.xml.util.PermissionUtil.romPathFile
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FileAccessPermissionDialogFragment private constructor(): DialogFragment() {

    @Inject
    lateinit var permissionManager: IPermissionManager

    companion object {
        fun showFragment(
            manager: FragmentManager,
            tag: String,
            permissionSuccess: () -> Unit
        ) {
            if (isStorageAccess && romPathFile != null) {
                permissionSuccess.invoke()
            } else {
                FileAccessPermissionDialogFragment().show(manager, tag)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("授权文件访问权限")
            .setMessage("搜索本地ROM需要授权文件访问权限,并指定ROM文件所在目录")
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                permissionManager.fetchStoragePermission()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                NLog.e("test999", "isStorageAccess ===> " + getString(R.string.cancel))
                dismiss()
            }
            .create()
    }

}