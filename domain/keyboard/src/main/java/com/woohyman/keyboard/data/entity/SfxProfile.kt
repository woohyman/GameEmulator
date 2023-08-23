package com.woohyman.keyboard.data.entity

/**
 * 音效
 */
abstract class SfxProfile(
    var name: String,
    var isStereo: Boolean,
    var rate: Int,
    var bufferSize: Int,
    var encoding: SoundEncoding,
    var quality: Int,
) {
    abstract fun toInt(): Int
    enum class SoundEncoding {
        PCM8, PCM16
    }
}