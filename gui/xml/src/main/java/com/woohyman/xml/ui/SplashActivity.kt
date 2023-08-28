package com.woohyman.xml.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.woohyman.xml.R

/**
 * Created by huzongyao on 2018/6/4.
 */
class SplashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent()
        intent.action = getString(R.string.action_gallery_page)
        startActivity(intent)
        finish()
    }
}