package com.woohyman.xml.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> constructor(
    private val bindingFactory: (LayoutInflater) -> VB
) : AppCompatActivity() {

    protected val binding:VB by lazy {
        bindingFactory(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}

