package nostalgia.framework.ui.preferences

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import nostalgia.framework.keyboard.KeyboardProfile
import nostalgia.framework.R
import nostalgia.framework.base.EmulatorHolder
import nostalgia.framework.controllers.KeyboardController
import nostalgia.framework.utils.NLog
import java.util.regex.Pattern

class KeyboardSettingsActivity : AppCompatActivity(), AdapterView.OnItemClickListener {
    private var list: ListView? = null
    private var profile: KeyboardProfile? = null
    private val inverseMap = SparseIntArray()
    private var profilesNames: ArrayList<String>? = null
    private var adapter: Adapter? = null
    private var newProfile = false
    private var deleted = false
    @Suppress("deprecation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard_settings)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        profilesNames = KeyboardProfile.getProfilesNames(this)
        list = findViewById(R.id.act_keyboard_settings_list)
        profile = KeyboardProfile.load(this, intent.getStringExtra(EXTRA_PROFILE_NAME))
        inverseMap.clear()
        val keyMap = profile?.keyMap
        for (code in KeyboardProfile.BUTTON_KEY_EVENT_CODES!!) {
            inverseMap.append(code, 0)
        }
        for (i in 0 until keyMap!!.size()) {
            inverseMap.append(keyMap.valueAt(i), keyMap.keyAt(i))
        }
        if (intent.getBooleanExtra(EXTRA_NEW_BOOL, false)) {
            profile?.name = "new profile"
            newProfile = true
            showDialog(0)
        }
        title = String.format(
            getText(R.string.key_profile_pref).toString(),
            profile?.name
        )
        adapter = Adapter()
        list?.setAdapter(adapter)
        list?.setOnItemClickListener(this)
        val plv = findViewById<PlayersLabelView>(R.id.act_keyboard_settings_plv)
        if (EmulatorHolder.info!!.isMultiPlayerSupported) {
            plv.setPlayersOffsets(adapter?.playersOffset)
            list?.setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
                override fun onScroll(
                    view: AbsListView, firstVisibleItem: Int,
                    visibleItemCount: Int, totalItemCount: Int
                ) {
                    val v = list?.getChildAt(0)
                    if (v != null) {
                        var currentY = 0
                        for (i in 0 until list!!.firstVisiblePosition) {
                            currentY += adapter!!.rowHeight
                        }
                        val scrollY = -list?.getChildAt(0)?.top!! + currentY
                        plv.setOffset(scrollY)
                    }
                }
            })
        } else {
            plv.visibility = View.GONE
        }
    }

    @Deprecated("")
    override fun onCreateDialog(id: Int): Dialog {
        val alertDialogBuilder = AlertDialog.Builder(this)
        val editText = EditText(this)
        editText.hint = "Insert profile name"
        editText.setPadding(10, 10, 10, 10)
        alertDialogBuilder.setView(editText)
        alertDialogBuilder
            .setCancelable(false)
            .setPositiveButton("OK") { dialog: DialogInterface?, id1: Int ->
                profile!!.name = editText.text.toString()
                title = String.format(
                    getText(R.string.key_profile_pref).toString(),
                    profile!!.name
                )
                val data = Intent()
                data.putExtra(EXTRA_PROFILE_NAME, profile!!.name)
                setResult(Activity.RESULT_OK, data)
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog: DialogInterface, id12: Int ->
                dialog.cancel()
                setResult(RESULT_NAME_CANCEL)
                finish()
            }
        val alertDialog = alertDialogBuilder.create()
        val pattern = Pattern.compile("[a-zA-Z0-9]")
        editText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val ok = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                val txt = s.toString()
                val m = pattern.matcher(txt)
                ok.isEnabled = !profilesNames!!.contains(txt) && txt != "" && m.replaceAll("")
                    .isEmpty()
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int, count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {}
        })
        alertDialog.setOnShowListener { dialog: DialogInterface? ->
            val ok = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ok.isEnabled = false
        }
        return alertDialog
    }

    override fun onResume() {
        super.onResume()
        deleted = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemClick(arg0: AdapterView<*>?, arg1: View, position: Int, arg3: Long) {
        if (position == KeyboardProfile.BUTTON_NAMES!!.size) {
            if (KeyboardProfile.isDefaultProfile(profile!!.name!!)) {
                KeyboardProfile.restoreDefaultProfile(profile!!.name!!, this)
            } else {
                profile!!.delete(this)
            }
            deleted = true
            finish()
        } else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(
                String.format(
                    resources.getString(R.string.press_key),
                    KeyboardProfile.BUTTON_NAMES!![position]
                )
            )
            builder.setNegativeButton("Cancel", null)
            val view = EditText(this@KeyboardSettingsActivity)
            builder.setView(view)
            val d: Dialog = builder.create()
            view.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    val ch = s[0]
                    proccessKeyEvent(ch.toString() + "", d, ch.code, position)
                }

                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun afterTextChanged(s: Editable) {}
            })
            d.setOnKeyListener { dialog: DialogInterface, keyCode: Int, event: KeyEvent ->
                var keyCode = keyCode
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (event.isAltPressed) {
                        keyCode = KeyboardController.KEY_XPERIA_CIRCLE
                    }
                }
                val txt = getKeyLabel(keyCode)
                if (event.action == KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener proccessKeyEvent(txt, dialog, keyCode, position)
                } else {
                    return@setOnKeyListener false
                }
            }
            d.show()
        }
    }

    private fun proccessKeyEvent(
        txt: String,
        dialog: DialogInterface,
        keyCode: Int,
        position: Int
    ): Boolean {
        NLog.i(TAG, "txt:$txt")
        return if (txt != "" && keyCode != KeyEvent.KEYCODE_BACK) {
            val idx = inverseMap.indexOfValue(keyCode)
            if (idx >= 0) {
                inverseMap.put(inverseMap.keyAt(idx), 0)
            }
            inverseMap.append(KeyboardProfile.BUTTON_KEY_EVENT_CODES!![position], keyCode)
            NLog.i(
                TAG,
                "isert " + KeyboardProfile.BUTTON_NAMES!![position] + " :" + keyCode
            )
            adapter!!.notifyDataSetInvalidated()
            dialog.dismiss()
            true
        } else {
            false
        }
    }

    override fun onPause() {
        super.onPause()
        if (!deleted) {
            profile!!.keyMap.clear()
            for (i in 0 until inverseMap.size()) {
                profile!!.keyMap.append(inverseMap.valueAt(i), inverseMap.keyAt(i))
            }
            profile!!.save(this)
        }
    }

    private inner class Adapter : BaseAdapter() {
        var inflater: LayoutInflater
        private var heightCache = -1

        init {
            inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.row_keyboard_settings, null)
            }
            val name = convertView.findViewById<TextView>(R.id.row_keyboard_name)
            val desc = convertView.findViewById<TextView>(R.id.row_keyboard_desc)
            val keyName = convertView.findViewById<TextView>(R.id.row_keyboard_key_name)
            convertView.isEnabled = true
            if (position < KeyboardProfile.BUTTON_NAMES!!.size) {
                name.text = KeyboardProfile.BUTTON_NAMES!![position]
                val keyCode = inverseMap[KeyboardProfile.BUTTON_KEY_EVENT_CODES!![position]]
                val label = getKeyLabel(keyCode)
                keyName.text = label
                keyName.visibility = View.VISIBLE
            } else {
                name.text =
                    if (KeyboardProfile.isDefaultProfile(profile!!.name!!)) getText(R.string.pref_keyboard_settings_restore_def) else getText(
                        R.string.pref_keyboard_settings_delete_prof
                    )
                desc.visibility = View.GONE
                keyName.visibility = View.GONE
            }
            return convertView
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getItem(position: Int): Any? {
            return null
        }

        override fun getCount(): Int {
            return KeyboardProfile.BUTTON_NAMES!!.size + if (newProfile) 0 else 1
        }

        val rowHeight: Int
            get() {
                if (heightCache < 0) {
                    val convertView = inflater.inflate(R.layout.row_keyboard_settings, null)
                    convertView.measure(
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    heightCache = convertView.measuredHeight
                }
                return heightCache
            }
        val playersOffset: IntArray
            get() {
                val result = ArrayList<Int>()
                var lastDesc = ""
                val h: Int = rowHeight
                for (i in KeyboardProfile.BUTTON_NAMES?.indices!!) {
                    val desc = KeyboardProfile.BUTTON_DESCRIPTIONS?.get(i)
                    if (lastDesc != desc) {
                        result.add(i * h)
                        lastDesc = desc!!
                    }
                }
                val res = IntArray(result.size)
                for (i in result.indices) res[i] = result[i]
                return res
            }
    }

    companion object {
        const val EXTRA_PROFILE_NAME = "EXTRA_PROFILE_NAME"
        const val EXTRA_NEW_BOOL = "EXTRA_NEW_BOOL"
        const val RESULT_NAME_CANCEL = 645943
        private const val TAG = "KeyboardSettingsActivity"
        private val NON_PRINTABLE_KEY_LABELS = SparseArray<String>()

        init {
            initNonPrintMap()
        }

        @SuppressLint("InlinedApi")
        private fun initNonPrintMap() {
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_ENTER, "Enter")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_SPACE, "Space")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_LEFT, "Left")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_RIGHT, "Right")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_UP, "Up")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_DPAD_DOWN, "Down")
            NON_PRINTABLE_KEY_LABELS.put(KeyboardController.KEY_XPERIA_CIRCLE, "Circle")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_A, "A")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_B, "B")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_C, "C")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_X, "X")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_Y, "Y")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_Z, "Z")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_SELECT, "Select")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_START, "Start")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_MODE, "MODE")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_THUMBL, "THUMBL")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_THUMBR, "THUMBR")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_1, "1")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_2, "2")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_3, "3")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_4, "4")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_5, "5")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_6, "6")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_7, "7")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_8, "8")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_9, "9")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_10, "10")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_11, "11")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_12, "12")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_13, "13")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_14, "14")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_15, "15")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_16, "16")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_R1, "R1")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_R2, "R2")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_L1, "L1")
            NON_PRINTABLE_KEY_LABELS.put(KeyEvent.KEYCODE_BUTTON_L2, "L2")
        }

        fun getKeyLabel(keyCode: Int): String {
            if (keyCode == 0) {
                return ""
            }
            val text = NON_PRINTABLE_KEY_LABELS[keyCode]
            return if (text != null) {
                text
            } else {
                val event = KeyEvent(0, keyCode)
                val ch = event.unicodeChar.toChar()
                if (ch.code != 0) {
                    ch.toString() + ""
                } else {
                    "key-$keyCode"
                }
            }
        }
    }
}