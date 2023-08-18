package nostalgia.framework.data.entity

/**
 * 图形效果
 */
abstract class GfxProfile {
    var name: String? = null
    var originalScreenWidth = 0
    var originalScreenHeight = 0
    var fps = 0
    abstract fun toInt(): Int
}