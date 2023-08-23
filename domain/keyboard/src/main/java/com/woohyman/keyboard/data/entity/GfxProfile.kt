package com.woohyman.keyboard.data.entity

/**
 * 图形效果
 */
abstract class GfxProfile(
    var name: String,
    var originalScreenWidth: Int,
    var originalScreenHeight: Int,
    var fps: Int,
) {
    abstract fun toInt(): Int
}