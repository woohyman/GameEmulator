package nostalgia.framework.utils

import android.app.Dialog

object DialogUtils {
    @JvmStatic
    fun show(dialog: Dialog, cancelable: Boolean) {
        dialog.setCanceledOnTouchOutside(cancelable)
        dialog.show()
    }
}