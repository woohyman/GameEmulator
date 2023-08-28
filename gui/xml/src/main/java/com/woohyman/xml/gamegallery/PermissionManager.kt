package com.woohyman.xml.gamegallery

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.UriUtils
import com.blankj.utilcode.util.Utils
import com.woohyman.xml.R
import com.woohyman.xml.util.PermissionUtil.isStorageAccess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class PermissionManager @Inject constructor(
    activity: Activity,
) : IPermissionManager {
    val compatActivity = activity as AppCompatActivity

    init {
        compatActivity.lifecycle.addObserver(this)
    }

    //获取权限成功
    private val _fetchPermissionFlow = MutableSharedFlow<Boolean>(replay = 0)
    override val fetchPermissionFlow: SharedFlow<Boolean> = _fetchPermissionFlow

    private val requestRomPathLauncher =
        compatActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data?.data ?: return@registerForActivityResult
                grantSuccess(data)
                compatActivity.lifecycleScope.launch {
                    _fetchPermissionFlow.emit(true)
                }
            }
        }

    private var isReadyToRequestRomPermission = false
    private val requestAllFileAccessLaucher =
        compatActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            isReadyToRequestRomPermission = true
        }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        if (isStorageAccess && isReadyToRequestRomPermission) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            requestRomPathLauncher.launch(intent)
            isReadyToRequestRomPermission = false
        }
    }

    private fun grantSuccess(uri: Uri) {
        val dfile = DocumentFile.fromTreeUri(Utils.getApp(), uri)
        val file = UriUtils.uri2File(dfile!!.uri)

        val editor = compatActivity.getSharedPreferences("user", MODE_PRIVATE).edit()
        editor?.putString("path", file.absolutePath)
        editor?.apply()

    }

    override fun fetchStoragePermission() {
        if (!isStorageAccess) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                requestAllFileAccessLaucher.launch(intent)
            } else {
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        startWithPermission()
                    }
                }, 800L)
            }
        } else {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            requestRomPathLauncher.launch(intent)
        }

    }

    fun startWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    val intent = Intent()
                    intent.action = compatActivity.getString(R.string.action_gallery_page)
                    compatActivity.startActivity(intent)
                }

                override fun onDenied() {

                }
            }).request()
    }

}