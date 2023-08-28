package com.woohyman.xml.gamegallery

import androidx.lifecycle.DefaultLifecycleObserver
import kotlinx.coroutines.flow.SharedFlow

interface IPermissionManager : DefaultLifecycleObserver {

    //权限获取结果
    val fetchPermissionFlow: SharedFlow<Boolean>

    //获取存储权限
    fun fetchStoragePermission()

}