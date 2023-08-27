package com.woohyman.xml.ui.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import com.woohyman.xml.R
import com.woohyman.xml.databinding.PopupMenuBinding

class PopupMenu @SuppressLint("ClickableViewAccessibility") constructor(
    private val mContext: Context
) {
    var font: Typeface? = null
    private val mInflater: LayoutInflater = mContext
        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mWindowManager: WindowManager = mContext
        .getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val mPopupWindow by lazy {
        PopupWindow(mContext).also {
            it.setTouchInterceptor(OnTouchListener { v: View, event: MotionEvent ->
                v.performClick()
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    it.dismiss()
                    return@OnTouchListener true
                }
                false
            })
        }
    }

    private var mListener: OnItemSelectedListener? = null
    private val mItems: MutableList<MenuItem> = ArrayList()
    private var mWidth = 240
    private val mScale: Float

    private var binding:PopupMenuBinding

    init {
        val metrics = DisplayMetrics()
        mWindowManager.defaultDisplay.getMetrics(metrics)
        mScale = metrics.scaledDensity
        binding = PopupMenuBinding.bind(mInflater.inflate(R.layout.popup_menu, null))
        setContentView(binding.root)
    }

    /**
     * Sets the popup's content.
     *
     * @param contentView
     */
    private fun setContentView(contentView: View) {
        mPopupWindow.contentView = contentView
    }

    fun add(itemId: Int, titleRes: Int): MenuItem {
        val item = MenuItem()
        item.itemId = itemId
        item.title = mContext.getString(titleRes)
        mItems.add(item)
        return item
    }

    /**
     * Show popup menu.
     *
     * @param anchor
     */
    /**
     * Show popup menu.
     */
    @JvmOverloads
    fun show(anchor: View? = null) {
        check(mItems.size != 0) { "PopupMenu#add was not called with a menu item to display." }
        preShow()
        val adapter = MenuItemAdapter(mContext, mItems)
        binding.items.adapter = adapter
        binding.items.onItemClickListener =
            AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                mListener?.onItemSelected(mItems[position])
                mPopupWindow.dismiss()
            }
        if (anchor == null) {
            val parent = (mContext as Activity).window.decorView
            mPopupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0)
            return
        }
        val xPos: Int
        val yPos: Int
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorRect = Rect(
            location[0], location[1], location[0]
                    + anchor.width, location[1] + anchor.height
        )
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.root.measure(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val rootHeight = binding.root.measuredHeight
        val screenHeight = mWindowManager.defaultDisplay.height

        // Set x-coordinate to display the popup menu
        xPos = anchorRect.centerX() - mPopupWindow.width / 2
        val dyTop = anchorRect.top
        val dyBottom = screenHeight + rootHeight
        val onTop = dyTop > dyBottom

        // Set y-coordinate to display the popup menu
        yPos = if (onTop) {
            anchorRect.top - rootHeight
        } else {
            if (anchorRect.bottom > dyTop) {
                anchorRect.bottom - 20
            } else {
                anchorRect.top - anchorRect.bottom + 20
            }
        }
        val parent = (mContext as Activity).window.decorView
        mPopupWindow.showAtLocation(parent, Gravity.NO_GRAVITY, xPos, yPos)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun preShow() {
        val width = (mWidth * mScale).toInt()
        mPopupWindow.width = width
        mPopupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        mPopupWindow.isTouchable = true
        mPopupWindow.isFocusable = true
        mPopupWindow.isOutsideTouchable = true
        mPopupWindow.animationStyle = android.R.style.Animation_Dialog
        mPopupWindow.setBackgroundDrawable(
            mContext.resources.getDrawable(
                R.drawable.panel_background
            )
        )
    }

    /**
     * Dismiss the popup menu.
     */
    fun dismiss() {
        if (mPopupWindow.isShowing) {
            mPopupWindow.dismiss()
        }
    }

    /**
     * Sets the popup menu header's title.
     *
     * @param title
     */
    fun setHeaderTitle(title: CharSequence?) {
        binding.headerTitle.text = title
        binding.headerTitle.visibility = View.VISIBLE
        binding.headerTitle.requestFocus()
        binding.headerTitle.typeface = font
    }

    /**
     * Change the popup's width.
     *
     * @param width
     */
    fun setWidth(width: Int) {
        mWidth = width
    }

    /**
     * Register a callback to be invoked when an item in this PopupMenu has been
     * selected.
     *
     * @param listener
     */
    fun setOnItemSelectedListener(listener: OnItemSelectedListener?) {
        mListener = listener
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * PopupMenu has been selected.
     */
    interface OnItemSelectedListener {
        fun onItemSelected(item: MenuItem?)
    }

    internal class ViewHolder {
        var icon: ImageView? = null
        var title: TextView? = null
    }

    private inner class MenuItemAdapter(context: Context?, objects: List<MenuItem>?) :
        ArrayAdapter<MenuItem?>(
            context!!, 0, objects!!
        ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView
            val holder: ViewHolder
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.menu_list_item, null)
                holder = ViewHolder()
                holder.icon = convertView.findViewById(R.id.icon)
                holder.title = convertView.findViewById(R.id.title)
                holder.run { title?.setTypeface(font) }
                convertView.tag = holder
            } else {
                holder = convertView.tag as ViewHolder
            }
            val item = getItem(position)
            if (item!!.icon != null) {
                holder.icon!!.setImageDrawable(item.icon)
                holder.icon!!.visibility = View.VISIBLE
            } else {
                holder.icon!!.visibility = View.GONE
            }
            holder.title!!.text = item.title
            return convertView!!
        }
    }
}