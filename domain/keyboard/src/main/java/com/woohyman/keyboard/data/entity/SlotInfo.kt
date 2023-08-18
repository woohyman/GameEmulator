package nostalgia.framework.data.entity

import android.graphics.Bitmap

data class SlotInfo(
    var id: Int = 0,
    var isUsed: Boolean = false,
    var path: String? = null,
    var screenShot: Bitmap? = null,
    var lastModified: Long = -1
)