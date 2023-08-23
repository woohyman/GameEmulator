package com.woohyman.xml.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.PermissionUtils
import com.woohyman.xml.R
import java.util.Timer
import java.util.TimerTask

/**
 * Created by huzongyao on 2018/6/4.
 */
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val intent = Intent()
        intent.action = getString(R.string.action_gallery_page)
        startActivity(intent)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (Environment.isExternalStorageManager()) {
//                val intent = Intent()
//                intent.action = getString(R.string.action_gallery_page)
//                startActivity(intent)
//                finish()
//            } else {
//                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                startActivityForResult(intent, GET_ALL_FILE_ACCESS)
//            }
//        } else {
//            val timer = Timer()
//            timer.schedule(object : TimerTask() {
//                override fun run() {
//                    startWithPermission()
//                }
//            }, 800L)
//        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            val intent = Intent()
            intent.action = getString(R.string.action_gallery_page)
            startActivity(intent)
            finish()
        }
    }

    private fun startWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    val intent = Intent()
                    intent.action = getString(R.string.action_gallery_page)
                    startActivity(intent)
                    finish()
                }

                override fun onDenied() {
                    finish()
                }
            }).request()
    }

    companion object {
        private const val GET_ALL_FILE_ACCESS = 673
    }
}