package com.woohyman.xml.ui.cheats

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.TextView
import com.woohyman.xml.R
import com.woohyman.keyboard.cheats.Cheat

class CheatsListAdapter(var cheatsActivity: CheatsActivity, objects: List<Cheat>?) :
    ArrayAdapter<Cheat?>(
        cheatsActivity, 0, objects!!
    ) {
    var inflater: LayoutInflater

    init {
        inflater =
            cheatsActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val chars: TextView
        val desc: TextView
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_cheat_list_item, null)
            chars = convertView.findViewById(R.id.row_cheat_chars)
            desc = convertView.findViewById(R.id.row_cheat_desc)
        } else {
            chars = convertView.findViewById(R.id.row_cheat_chars)
            desc = convertView.findViewById(R.id.row_cheat_desc)
        }
        val cheat = getItem(position)
        val enable = convertView!!.findViewById<CheckBox>(R.id.row_cheat_enable)
        val edit = convertView.findViewById<ImageButton>(R.id.row_cheat_edit)
        val remove = convertView.findViewById<ImageButton>(R.id.row_cheat_remove)
        chars.text = cheat!!.chars
        desc.text = cheat.desc
        enable.isChecked = cheat.enable
        edit.setOnClickListener { v: View? -> cheatsActivity.editCheat(position) }
        remove.setOnClickListener { v: View? -> cheatsActivity.removeCheat(position) }
        enable.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            val cheat1 = getItem(position)
            cheat1!!.enable = isChecked
            cheatsActivity.saveCheats()
        }
        return convertView
    }
}