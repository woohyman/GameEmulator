package com.woohyman.xml.ui.cheats

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import com.woohyman.keyboard.cheats.Cheat
import com.woohyman.keyboard.utils.EmuUtils
import com.woohyman.xml.R
import com.woohyman.xml.base.BaseActivity
import com.woohyman.xml.databinding.ActivityCheatsBinding
import com.woohyman.xml.databinding.DialogNewCheatBinding
import java.util.Locale

class CheatsActivity : BaseActivity<ActivityCheatsBinding>(
    ActivityCheatsBinding::inflate
) {
    private val adapter: CheatsListAdapter by lazy {
        CheatsListAdapter(this, cheats)
    }
    private var gameHash: String? = null
    private var cheats: ArrayList<Cheat> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        gameHash = intent.getStringExtra(EXTRA_IN_GAME_HASH)
        cheats = Cheat.getAllCheats(this, gameHash)
        binding.actCheatsList.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.cheats_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.cheats_menu_add) {
            openCheatDetailDialog(-1)
            return true
        } else if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openCheatDetailDialog(idx: Int) {
        val dialog = Dialog(this, R.style.DialogTheme)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val content = inflater.inflate(R.layout.dialog_new_cheat, null)
        val binding = DialogNewCheatBinding.bind(content)
        dialog.setContentView(content)
        if (idx >= 0) {
            val cheat = cheats[idx]
            binding.dialogNewCheatChars.setText(cheat.chars)
            binding.dialogNewCheatDesc.setText(cheat.desc)
        }
        if (binding.dialogNewCheatChars.text.toString() == "") {
            binding.dialogNewCheatSave.isEnabled = false
        }
        binding.dialogNewCheatChars.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun beforeTextChanged(arg0: CharSequence, arg1: Int, arg2: Int, arg3: Int) {}
            override fun afterTextChanged(arg0: Editable) {
                var s = arg0.toString()
                val locale = Locale.getDefault()
                if (s != s.uppercase(locale)) {
                    s = s.uppercase(locale)
                    binding.dialogNewCheatChars.setSelection(s.length)
                }
                var newText = s.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                if (newText != s) {
                    binding.dialogNewCheatChars.setText(newText)
                    binding.dialogNewCheatChars.setSelection(newText.length)
                }
                s = newText
                newText = s.replace(EmuUtils.emulator.info.cheatInvalidCharsRegex!!.toRegex(), "")
                if (newText != s) {
                    binding.dialogNewCheatChars.setText(newText)
                    binding.dialogNewCheatChars.setSelection(newText.length)
                }
                binding.dialogNewCheatSave.isEnabled = newText != ""
            }
        })
        binding.dialogNewCheatSave.setOnClickListener {
            if (idx == -1) {
                cheats.add(
                    Cheat(
                        binding.dialogNewCheatChars.text.toString(),
                        binding.dialogNewCheatDesc.text.toString(),
                        true
                    )
                )
            } else {
                val cheat = cheats[idx]
                cheat.chars = binding.dialogNewCheatChars.text.toString()
                cheat.desc = binding.dialogNewCheatDesc.text.toString()
            }
            adapter.notifyDataSetChanged()
            Cheat.saveCheats(this, gameHash, cheats)
            dialog.cancel()
        }
        dialog.show()
    }

    fun removeCheat(idx: Int) {
        cheats.removeAt(idx)
        adapter.notifyDataSetChanged()
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