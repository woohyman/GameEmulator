package com.woohyman.xml.emulator.business

import com.blankj.utilcode.util.ActivityUtils
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.xml.emulator.EmulatorActivity
import javax.inject.Inject

class FetchProxy @Inject constructor() : IFetchProxy {

    private val emulatorActivity: EmulatorActivity =
        ActivityUtils.getTopActivity() as EmulatorActivity
    override val fragmentShader: String get() = emulatorActivity.fragmentShader
    override val game: GameDescription
        get() = emulatorActivity.intent.getSerializableExtra(
            EmulatorActivity.EXTRA_GAME
        ) as GameDescription

}