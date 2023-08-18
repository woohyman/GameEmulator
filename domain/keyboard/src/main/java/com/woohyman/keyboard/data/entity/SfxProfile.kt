package nostalgia.framework.data.entity

/**
 * 音效
 */
abstract class SfxProfile {
    var name: String? = null
    var isStereo = false
    var rate = 0
    var bufferSize = 0
    var encoding: SoundEncoding? = null
    var quality = 0
    abstract fun toInt(): Int
    enum class SoundEncoding {
        PCM8, PCM16
    }
}