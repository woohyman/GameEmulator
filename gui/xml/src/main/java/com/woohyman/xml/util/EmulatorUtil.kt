package com.woohyman.xml.util

import com.blankj.utilcode.util.Utils
import com.woohyman.xml.di.FetchProxyEntryPoint
import dagger.hilt.android.EntryPointAccessors

object EmulatorUtil {
    val fetchProxy = EntryPointAccessors.fromApplication(
        Utils.getApp(),
        FetchProxyEntryPoint::class.java
    ).getFetchProxy()
}