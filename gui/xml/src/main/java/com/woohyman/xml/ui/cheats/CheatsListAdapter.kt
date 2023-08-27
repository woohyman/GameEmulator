package com.woohyman.xml.ui.cheats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageButton
import com.woohyman.keyboard.cheats.Cheat
import com.woohyman.xml.R
import com.woohyman.xml.databinding.RowCheatListItemBinding

class CheatsListAdapter(private var cheatsActivity: CheatsActivity, objects: List<Cheat>) :
    ArrayAdapter<Cheat?>(cheatsActivity, 0, objects) {

    private val inflater: LayoutInflater by lazy {
        cheatsActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.row_cheat_list_item, null)
        val binding = RowCheatListItemBinding.bind(view)
        val cheat = getItem(position) ?: return view
        binding.rowCheatChars.text = cheat.chars
        binding.rowCheatDesc.text = cheat.desc
        binding.rowCheatEnable.isChecked = cheat.enable
        binding.rowCheatEdit.setOnClickListener {
            cheatsActivity.editCheat(position)
        }
        binding.rowCheatRemove.setOnClickListener {
            cheatsActivity.removeCheat(position)
        }
        binding.rowCheatEnable.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            cheat.enable = isChecked
            cheatsActivity.saveCheats()
        }
        return view
    }
}