package com.woohyman.xml.ui.gamegallery

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.woohyman.keyboard.data.database.GameDescription
import com.woohyman.xml.R
import com.woohyman.xml.base.BaseActivity
import com.woohyman.xml.databinding.ActivitySlotSelectionBinding
import com.woohyman.xml.databinding.RowSlotItemBinding
import com.woohyman.xml.ui.gamegallery.Constants.DIALOAG_TYPE_LOAD
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_GAME
import com.woohyman.xml.ui.gamegallery.Constants.EXTRA_SLOT
import com.woohyman.xml.ui.gamegallery.Constants.SEND_SLOT
import com.woohyman.xml.ui.gamegallery.uistate.SlotInfoUIState
import com.woohyman.xml.ui.gamegallery.viewmodel.SlotSelectionViewModel
import com.woohyman.xml.ui.widget.PopupMenu
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SlotSelectionActivity : BaseActivity<ActivitySlotSelectionBinding>
    (ActivitySlotSelectionBinding::inflate) {

    private val slots by lazy {
        listOf(
            binding.slot0 as RowSlotItemBinding,
            binding.slot1 as RowSlotItemBinding,
            binding.slot2 as RowSlotItemBinding,
            binding.slot3 as RowSlotItemBinding,
            binding.slot4 as RowSlotItemBinding,
            binding.slot5 as RowSlotItemBinding,
            binding.slot6 as RowSlotItemBinding,
            binding.slot7 as RowSlotItemBinding
        )
    }

    private var clearIcon: Drawable? = null
    private var sendIcon: Drawable? = null

    private val viewModel: SlotSelectionViewModel by viewModels()

    private fun initSlot(slotInfoUIState: SlotInfoUIState) {
        slotInfoUIState.apply {
            val slotView = slots[idx]
            val isUsed = slotInfo.isUsed
            val screenshotBitmap = slotInfo.screenShot

            slotView.rowSlotLabel.text = labelS
            slotView.rowSlotMessage.text = messageS
            slotView.rowSlotDate.text = dateS
            slotView.rowSlotTime.text = timeS
            slotView.root.setOnClickListener { onSelected(viewModel.game, idx + 1, isUsed) }
            if (isUsed) {
                slotView.root.setOnLongClickListener {
                    val menu = PopupMenu(this@SlotSelectionActivity)
                    menu.setHeaderTitle(labelS)
                    menu.add(SEND_SLOT, R.string.act_slot_popup_menu_send).icon = sendIcon
                    menu.show(slotView.root)
                    true
                }
            }
            if (screenshotBitmap != null) {
                slotView.rowSlotScreenshot.setImageBitmap(screenshotBitmap)
                slotView.rowSlotMessage.visibility = View.INVISIBLE
            }
        }
    }

    private fun onSelected(game: GameDescription?, slot: Int, isUsed: Boolean) {
        if (viewModel.type == DIALOAG_TYPE_LOAD && !isUsed) {
            return
        }
        val data = Intent()
        data.putExtra(EXTRA_GAME, game)
        data.putExtra(EXTRA_SLOT, slot)
        setResult(RESULT_OK, data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        clearIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_clear_slot, null)
        sendIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_send_slot, null)

        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(if (viewModel.type == DIALOAG_TYPE_LOAD) R.string.game_menu_load else R.string.game_menu_save)
        viewModel.onCreate(intent)

        viewModel.curShareFlow.onEach {
            initSlot(it)
        }.launchIn(lifecycleScope)

    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        slots[viewModel.curSlotIndex].root.requestFocusFromTouch()
        viewModel.onResume()
    }
}

object Constants{
    val EXTRA_GAME = "EXTRA_GAME"
    val EXTRA_BASE_DIRECTORY = "EXTRA_BASE_DIR"
    val EXTRA_SLOT = "EXTRA_SLOT"
    val EXTRA_DIALOG_TYPE_INT = "EXTRA_DIALOG_TYPE_INT"
    val DIALOAG_TYPE_LOAD = 1
    val DIALOAG_TYPE_SAVE = 2
    const val TAG = "SlotSelectionActivity"
    const val SEND_SLOT = 0
    const val REMOVE_SLOT = 1
}