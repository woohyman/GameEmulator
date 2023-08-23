package com.woohyman.xml.ui.cheats

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.woohyman.xml.R
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.cheats.Cheat
import java.util.Locale

class CheatsActivity : AppCompatActivity() {
    var save: Button? = null
    private var list: ListView? = null
    private var adapter: CheatsListAdapter? = null
    private var gameHash: String? = null
    private var cheats: ArrayList<Cheat>? = null
    private var actionBar: ActionBar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cheats)
        actionBar = supportActionBar
        if (actionBar != null) {
            actionBar!!.setHomeButtonEnabled(true)
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        }
        gameHash = intent.getStringExtra(EXTRA_IN_GAME_HASH)
        list = findViewById(R.id.act_cheats_list)
        cheats = Cheat.getAllCheats(this, gameHash)
        adapter = CheatsListAdapter(this, cheats)
        list!!.setAdapter(adapter)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.cheats_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.cheats_menu_add) {
            openCheatDetailDialog(-1)
            return true
        } else if (itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun openCheatDetailDialog(idx: Int) {
        val dialog = Dialog(this, R.style.DialogTheme)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val content = inflater.inflate(R.layout.dialog_new_cheat, null)
        dialog.setContentView(content)
        val chars = content.findViewById<EditText>(R.id.dialog_new_cheat_chars)
        val desc = content.findViewById<EditText>(R.id.dialog_new_cheat_desc)
        save = content.findViewById(R.id.dialog_new_cheat_save)
        if (idx >= 0) {
            val cheat = cheats!![idx]
            chars.setText(cheat.chars)
            desc.setText(cheat.desc)
        }
        if (chars.text.toString() == "") {
            save!!.setEnabled(false)
        }
        chars.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                var s = arg0.toString()
                val locale = Locale.getDefault()
                if (s != s.uppercase(locale)) {
                    s = s.uppercase(locale)
                    chars.setSelection(s.length)
                }
                var newText = s.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                if (newText != s) {
                    chars.setText(newText)
                    chars.setSelection(newText.length)
                }
                s = newText
                newText = s.replace(info!!.cheatInvalidCharsRegex!!.toRegex(), "")
                if (newText != s) {
                    chars.setText(newText)
                    chars.setSelection(newText.length)
                }
                if (newText == "") {
                    save?.setEnabled(false)
                } else {
                    save?.setEnabled(true)
                }
            }
        })
        save?.setOnClickListener(View.OnClickListener { v: View? ->
            if (idx == -1) {
                cheats!!.add(Cheat(chars.text.toString(), desc.text.toString(), true))
            } else {
                val cheat = cheats!![idx]
                cheat.chars = chars.text.toString()
                cheat.desc = desc.text.toString()
            }
            adapter!!.notifyDataSetChanged()
            Cheat.saveCheats(this, gameHash, cheats)
            dialog.cancel()
        })
        dialog.show()
    }

    fun removeCheat(idx: Int) {
        cheats!!.removeAt(idx)
        adapter!!.notifyDataSetChanged()
        Cheat.saveCheats(this, gameHash, cheats)
    }

    fun editCheat(idx: Int) {
        openCheatDetailDialog(idx)
    }

    fun saveCheats() {
        Cheat.saveCheats(this, gameHash, cheats)
    }

    companion object {
        const val EXTRA_IN_GAME_HASH = "EXTRA_IN_GAME_HASH"
    }
}