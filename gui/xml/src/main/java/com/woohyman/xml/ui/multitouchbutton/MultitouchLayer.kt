package com.woohyman.xml.ui.multitouchbutton

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Vibrator
import android.util.AttributeSet
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.woohyman.xml.R
import com.woohyman.keyboard.base.EmulatorHolder.info
import com.woohyman.keyboard.base.ViewPort
import com.woohyman.keyboard.base.ViewUtils.computeAllInitViewPorts
import com.woohyman.keyboard.base.ViewUtils.computeInitViewPort
import com.woohyman.keyboard.base.ViewUtils.loadOrComputeAllViewPorts
import com.woohyman.keyboard.base.ViewUtils.loadOrComputeViewPort
import com.woohyman.keyboard.controllers.EmulatorController
import com.woohyman.keyboard.utils.EmuUtils.getDisplayHeight
import com.woohyman.keyboard.utils.EmuUtils.getDisplayWidth
import com.woohyman.keyboard.utils.EmuUtils.isDebuggable
import com.woohyman.keyboard.utils.NLog.d
import com.woohyman.keyboard.utils.NLog.e
import com.woohyman.keyboard.utils.NLog.i
import com.woohyman.keyboard.utils.NLog.wtf
import com.woohyman.keyboard.utils.PreferenceUtil.removeViewPortSave
import com.woohyman.keyboard.utils.PreferenceUtil.setViewPort
import java.util.Timer
import java.util.TimerTask

class MultitouchLayer : RelativeLayout, OnTouchListener {
    var videoModeLabelPaint = Paint()
    var lastW = 0
    var lastH = 0
    var touchLayer: LinearLayout? = null
    var vibrator: Vibrator? = null
    var paint = Paint()
    var bitmapRectPaint = Paint()
    var firstRun = true
    var screenElement: EditElement? = null
    var menuElement: EditElement? = null
    var editPaint = Paint()
    var redPaint = Paint()
    var selectIdx = -1
    var selectW = 0f
    var selectH = 0f
    var startDragX = 0f
    var startDragY = 0f
    var startDragXoffset = 0f
    var startDragYoffset = 0f
    var startTouchX = 0
    var startTouchY = 0
    var startDistance = 0f
    var lastValidBB = RectF()
    var lastTouchX = 0
    var lastTouchY = 0
    var editMode = EDIT_MODE.NONE
        set(value) {
            field = value
            invalidate()
            timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    counter++
                    editElementPaint.pathEffect =
                        DashPathEffect(floatArrayOf(4f, 4f), (counter % 8).toFloat())
                    postInvalidate()
                }
            }, 0, 50)
            if (field == EDIT_MODE.SCREEN) {
                resizeIcon = BitmapFactory.decodeResource(resources, R.drawable.resize_icon_2)
            }
        }

    var counter = 0
    var viewPortsEnvelops = HashMap<String, RectF>()
    var isResizing = false
    var timer = Timer()
    var cacheRotation = -1
    var cacheW = -1
    var cacheH = -1
    private val btns = ArrayList<View>()
    private val pointerMap = SparseIntArray()
    private var touchMapWidth = 0
    private var touchMapHeight = 0
    private val ridToIdxMap = SparseIntArray()
    private val editElementPaint = Paint()
    private var resizeIcon: Bitmap? = null
    private var buttonMinSizePx = 0f
    private val editElements = ArrayList<EditElement>()
    private var maps: Array<ByteArray?> = emptyArray()
    private var boundingBoxs: Array<Rect?> = emptyArray()
    private var buttonsBitmaps: Array<Bitmap?>? = arrayOfNulls(0)
    private var pressedButtonsBitmaps: Array<Bitmap?>? = arrayOfNulls(0)
    private val dpadRIDs = ArrayList<Int>()
    private val btnIdMap = ArrayList<Int?>()
    private var initCounter = 0
    private val optimCounters = IntArray(MAX_POINTERS)
    private var vibrationDuration = 100
    private var lastGameScreenshot: Bitmap? = null
    private var lastGfxProfileName: String? = null
    private var loadingSettings = true
    private var staticDPADEnabled = true
    private var pp: Paint? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun remapOldMTLprefToNew(pref: SharedPreferences, prefMap: Map<String, *>) {
        val oldIdsToNewMap: HashMap<Int, Int> = HashMap()
        val editor = pref.edit()
        val keysToRemove = HashSet<String>()
        var wrongFormat = false
        for ((key, value1) in prefMap) {
            val value = value1 as String
            keysToRemove.add(key)
            val oldBtnId = key.toInt()
            val newBtnId = oldIdsToNewMap[oldBtnId]
            val newKey = btnIdMap.indexOf(newBtnId)
            if (newBtnId == 0 || newKey == -1) {
                e(TAG, "oldBtnId:$oldBtnId newBtnId:$newBtnId newKey:$newKey")
                wrongFormat = true
            } else {
                editor.putString(newKey.toString() + "", value)
            }
        }
        if (wrongFormat) {
            editor.clear()
        } else {
            for (key in keysToRemove) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun setOpacity(alpha: Int) {
        if (!isInEditMode) {
            paint.alpha = alpha
        }
    }

    private fun init(context: Context) {
        dpadRIDs.add(R.id.button_center)
        dpadRIDs.add(R.id.button_down)
        dpadRIDs.add(R.id.button_up)
        dpadRIDs.add(R.id.button_left)
        dpadRIDs.add(R.id.button_right)
        dpadRIDs.add(R.id.button_up_left)
        dpadRIDs.add(R.id.button_up_right)
        dpadRIDs.add(R.id.button_down_left)
        dpadRIDs.add(R.id.button_down_right)
        btnIdMap.add(R.id.button_a)
        btnIdMap.add(R.id.button_a_turbo)
        btnIdMap.add(R.id.button_b)
        btnIdMap.add(R.id.button_b_turbo)
        btnIdMap.add(R.id.button_ab)
        if (info!!.keyMapping[EmulatorController.KEY_SELECT] != -1) {
            btnIdMap.add(R.id.button_select)
        }
        btnIdMap.add(R.id.button_start)
        btnIdMap.add(R.id.button_menu)
        btnIdMap.add(R.id.button_down)
        btnIdMap.add(R.id.button_up)
        btnIdMap.add(R.id.button_left)
        btnIdMap.add(R.id.button_right)
        btnIdMap.add(R.id.button_up_left)
        btnIdMap.add(R.id.button_up_right)
        btnIdMap.add(R.id.button_down_left)
        btnIdMap.add(R.id.button_down_right)
        btnIdMap.add(R.id.button_center)
        btnIdMap.add(R.id.button_fast_forward)
        if (!isInEditMode) {
            initScreenElement(false)
        }
        pp = Paint()
        pp!!.color = 0x5500ff00
        setBackgroundColor(0x01000000)
        paint.isFilterBitmap = true
        editElementPaint.color = getContext().resources.getColor(R.color.main_color)
        editElementPaint.style = Paint.Style.STROKE
        val dashPathEffect = DashPathEffect(floatArrayOf(1f, 4f), 0f)
        editElementPaint.pathEffect = dashPathEffect
        bitmapRectPaint.style = Paint.Style.STROKE
        bitmapRectPaint.color = editElementPaint.color
        resizeIcon = BitmapFactory.decodeResource(resources, R.drawable.resize_icon)
        buttonMinSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, BUTTON_MIN_SIZE_DP.toFloat(),
            resources.displayMetrics
        )
        if (!isInEditMode) {
            val vto = viewTreeObserver
            touchLayer = LinearLayout(getContext())
            vto.addOnGlobalLayoutListener {
                val w = measuredWidth
                val h = measuredHeight
                if (w != lastW || h != lastH) {
                    lastW = w
                    lastH = h
                    initMultiTouchMap()
                }
            }
            vibrator = getContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val videoModeLabelSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            14f, resources.displayMetrics
        )
        videoModeLabelPaint.textSize = videoModeLabelSize
        videoModeLabelPaint.style = Paint.Style.STROKE
    }

    private fun initMultiTouchMap() {
        initCounter++
        for (i in 0..99) pointerMap.put(i, EMPTY_COLOR)
        ridToIdxMap.clear()
        d(TAG, " create touch map width $measuredWidth height:$measuredHeight")
        touchMapWidth = measuredWidth
        touchMapHeight = measuredHeight
        val r = Rect()
        if (btns.size == 0) {
            getAllImageButtons(this, btns)
        }
        val btnsCount = btns.size
        i(TAG, " found $btnsCount multitouch btns")
        maps = arrayOfNulls(btnsCount)
        if (buttonsBitmaps != null) {
            for (bitmap in buttonsBitmaps!!) {
                bitmap?.recycle()
            }
        }
        if (buttonsBitmaps != null) {
            for (bitmap in buttonsBitmaps!!) {
                bitmap?.recycle()
            }
        }
        boundingBoxs = arrayOfNulls(btnsCount)
        buttonsBitmaps = arrayOfNulls(btnsCount)
        pressedButtonsBitmaps = arrayOfNulls(btnsCount)
        var idx = 0
        for (btn in btns) {
            ridToIdxMap.append(btn.id, idx)
            if (btn.visibility != GONE) {
                btn.getLocalVisibleRect(r)
                val btnW = btn.measuredWidth
                val btnH = btn.measuredHeight
                val btnX = getRelativeLeft(btn, this)
                val btnY = getRelativeTop(btn, this)
                boundingBoxs[idx] = Rect(btnX, btnY, btnX + btnW, btnY + btnH)
                r.offsetTo(btnX, btnY)
                if (btnW > 0 && btnH > 0) {
                    val buttonBitmap = Bitmap.createBitmap(btnW, btnH, Bitmap.Config.ARGB_8888)
                    if (buttonBitmap.isRecycled) {
                        wtf(TAG, "co se to kurva deje")
                        throw RuntimeException("netusim")
                    }
                    val buttonCanvas = Canvas(buttonBitmap)
                    btn.draw(buttonCanvas)
                    if (btn !is MultitouchTwoButtonArea) {
                        val pressedButtonBitmap =
                            Bitmap.createBitmap(btnW, btnH, Bitmap.Config.ARGB_8888)
                        val pressedButtonCanvas = Canvas(pressedButtonBitmap)
                        btn.isPressed = true
                        btn.draw(pressedButtonCanvas)
                        btn.isPressed = false
                        pressedButtonsBitmaps!![idx] = pressedButtonBitmap
                        buttonsBitmaps!![idx] = buttonBitmap
                    } else {
                        buttonsBitmaps!![idx] = buttonBitmap
                        pressedButtonsBitmaps!![idx] = null
                    }
                } else {
                    buttonsBitmaps!![idx] = null
                    pressedButtonsBitmaps!![idx] = null
                    postDelayed({ initMultiTouchMap() }, 1000)
                }
            }
            idx++
        }
        if (touchLayer!!.parent != null) {
            val parent = touchLayer!!.parent as ViewGroup
            parent.removeView(touchLayer)
        }
        touchLayer?.setOnTouchListener(this)
        removeAllViews()
        addView(touchLayer, LinearLayout.LayoutParams.MATCH_PARENT, measuredHeight)
        val hasSelect = info!!.keyMapping[EmulatorController.KEY_SELECT] != -1
        if (hasSelect) {
            editElements.add(EditElement(R.id.button_select, true, buttonMinSizePx).saveHistory())
        }
        editElements.add(EditElement(R.id.button_start, true, buttonMinSizePx).saveHistory())
        val dpad = EditElement(R.id.button_center, true, buttonMinSizePx * 5)
        dpad.add(R.id.button_down)
        dpad.add(R.id.button_up)
        dpad.add(R.id.button_left)
        dpad.add(R.id.button_right)
        dpad.add(R.id.button_up_left)
        dpad.add(R.id.button_up_right)
        dpad.add(R.id.button_down_left)
        dpad.add(R.id.button_down_right)
        dpad.saveHistory()
        editElements.add(dpad)
        editElements.add(EditElement(R.id.button_a, true, buttonMinSizePx).saveHistory())
        editElements.add(EditElement(R.id.button_b, true, buttonMinSizePx).saveHistory())
        editElements.add(EditElement(R.id.button_a_turbo, true, buttonMinSizePx).saveHistory())
        editElements.add(EditElement(R.id.button_b_turbo, true, buttonMinSizePx).saveHistory())
        editElements.add(EditElement(R.id.button_ab, true, buttonMinSizePx).saveHistory())
        editElements.add(EditElement(R.id.button_fast_forward, true, buttonMinSizePx).saveHistory())
        val menu = EditElement(R.id.button_menu, false, buttonMinSizePx).saveHistory()
        menu.setOnClickListener(object : OnEditItemClickListener {
            override fun onClick() {
                if (editMode != EDIT_MODE.NONE) {
                    (context as Activity).openOptionsMenu()
                }
            }
        })
        editElements.add(menu)
        menuElement = menu
        reloadTouchProfile()
        setEnableStaticDPAD(staticDPADEnabled)
    }

    fun reloadTouchProfile() {
        if (loadEditElements("") || firstRun || !isTouchMapsValid) {
            firstRun = btns.size == 0
            var idx = 0
            for (btn in btns) {
                if (btn.visibility != GONE) {
                    val bb = boundingBoxs[idx]
                    if (btn.id == R.id.button_fast_forward) {
                        i(TAG, "fast f btn $idx bb $bb")
                    }
                    val btnW = bb!!.width()
                    val btnH = bb.height()
                    val origButtonBitmap = buttonsBitmaps!![idx]
                    val origPressedButtonBitmap = pressedButtonsBitmaps!![idx]
                    if (origPressedButtonBitmap != null) {
                        val pressedBitmap = Bitmap.createScaledBitmap(
                            origPressedButtonBitmap, btnW, btnH, true
                        )
                        origPressedButtonBitmap.recycle()
                        pressedButtonsBitmaps!![idx] = pressedBitmap
                    }
                    if (origButtonBitmap != null) {
                        val buttonBitmap =
                            Bitmap.createScaledBitmap(origButtonBitmap, btnW, btnH, true)
                        origButtonBitmap.recycle()
                        buttonsBitmaps!![idx] = buttonBitmap
                        val buttonPixels = IntArray(btnW * btnH)
                        buttonBitmap.getPixels(buttonPixels, 0, btnW, 0, 0, btnW, btnH)
                        val map = ByteArray(buttonPixels.size)
                        for (i in buttonPixels.indices) {
                            val pixel = buttonPixels[i]
                            map[i] = if (pixel == 0) 0 else (idx + 1).toByte()
                        }
                        maps[idx] = map
                        if (btn is MultitouchTwoButtonArea) {
                            buttonBitmap.recycle()
                            buttonsBitmaps!![idx] = null
                        }
                    }
                }
                idx++
            }
        } else {
            i(TAG, hashCode().toString() + " nic se nezmenilo")
        }
    }

    private val isTouchMapsValid: Boolean
        get() {
            var idx = 0
            for (btn in btns) {
                if (btn.visibility != GONE) {
                    val bb = boundingBoxs[idx]
                    val len = bb!!.width() * bb.height()
                    val map = maps[idx]
                    if (map == null || map.size != len) {
                        return false
                    }
                }
                idx++
            }
            return true
        }

    private fun getAllImageButtons(root: ViewGroup, allButtons: ArrayList<View>) {
        for (i in 0 until root.childCount) {
            val v = root.getChildAt(i)
            if (v is ViewGroup) {
                getAllImageButtons(v, allButtons)
            } else if (v is MultitouchBtnInterface) {
                allButtons.add(v)
            }
        }
    }

    private fun getRelativeLeft(myView: View, rootView: View): Int {
        val parent = myView.parent
        return if (parent == null || parent === rootView) myView.left else myView.left + getRelativeLeft(
            parent as View,
            rootView
        )
    }

    private fun getRelativeTop(myView: View, rootView: View): Int {
        val parent = myView.parent
        return if (parent == null || parent === rootView) myView.top else myView.top + getRelativeTop(
            parent as View,
            rootView
        )
    }

    private fun handleTouchEvent(x: Int, y: Int, pointerId: Int, event: MotionEvent) {
        if (pointerId < MAX_POINTERS && event.actionMasked == MotionEvent.ACTION_MOVE) {
            if (optimCounters[pointerId] < COUNT_SKIP_MOVE_EVENT) {
                optimCounters[pointerId]++
                return
            }
            optimCounters[pointerId] = 0
        }
        if (x < 0 || y < 0 || x >= touchMapWidth || y >= touchMapHeight) {
            return
        }
        var newBtnIdx = EMPTY_COLOR
        for (i in maps.indices.reversed()) {
            val boundingBox = boundingBoxs!![i]
            if (boundingBox != null && boundingBox.contains(x, y)
                && btns[i].isEnabled
            ) {
                val map = maps[i]
                val newx = x - boundingBox.left
                val newy = y - boundingBox.top
                if (map == null) {
                    val debug = isDebuggable(context)
                    if (!debug) {
                        val e = IllegalStateException("button touch map neni nainicializovany")
                        e(TAG, e.toString())
                    }
                    newBtnIdx = i
                    break
                } else {
                    val idx = newx + newy * boundingBox.width()
                    if (idx < map.size) {
                        val btnIdx = map[idx].toInt()
                        if (btnIdx != 0) {
                            newBtnIdx = btnIdx
                            break
                        }
                    }
                }
            }
        }
        val oldBtnIdx = pointerMap[pointerId]
        if (newBtnIdx != 0) {
            if (oldBtnIdx != newBtnIdx) {
                if (oldBtnIdx != EMPTY_COLOR) {
                    onTouchExit(oldBtnIdx - 1, event)
                }
                onTouchEnter(newBtnIdx - 1, event)
                if (vibrationDuration > 0) {
                    vibrator!!.vibrate(vibrationDuration.toLong())
                }
            }
        } else if (oldBtnIdx != EMPTY_COLOR) {
            onTouchExit(oldBtnIdx - 1, event)
        }
        pointerMap.put(pointerId, newBtnIdx)
    }

    fun setVibrationDuration(duration: Int) {
        vibrationDuration = duration
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (editMode == EDIT_MODE.NONE) {
            if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                val pointerCount = event.pointerCount
                for (pointerIdx in 0 until pointerCount) {
                    val id = event.getPointerId(pointerIdx)
                    val x = event.getX(pointerIdx).toInt()
                    val y = event.getY(pointerIdx).toInt()
                    handleTouchEvent(x, y, id, event)
                }
            } else if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                val id = event.getPointerId(event.actionIndex)
                val oldBtnIdx = pointerMap[id]
                if (oldBtnIdx != EMPTY_COLOR) {
                    onTouchExit(oldBtnIdx - 1, event)
                }
                pointerMap.put(id, EMPTY_COLOR)
            } else if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                val pointerId = event.getPointerId(event.actionIndex)
                val pointerIdx = event.findPointerIndex(pointerId)
                if (pointerIdx != -1) {
                    val x = event.getX(pointerIdx).toInt()
                    val y = event.getY(pointerIdx).toInt()
                    handleTouchEvent(x, y, pointerId, event)
                }
            }
        } else {
            onTouchInEditMode(event)
        }
        return true
    }

    fun isPointerHandled(pointerId: Int): Boolean {
        return pointerMap[pointerId] != EMPTY_COLOR
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isInEditMode && editMode == EDIT_MODE.NONE) {
            for (idx in boundingBoxs.indices) {
                val btn = btns[idx] as MultitouchBtnInterface
                btn.removeRequestRepaint()
                if (btn.getVisibility() == VISIBLE) {
                    val b =
                        if (btn.isPressed()) pressedButtonsBitmaps!![idx] else buttonsBitmaps!![idx]
                    if (b != null) {
                        val bb = boundingBoxs[idx]
                        canvas.drawBitmap(b, bb!!.left.toFloat(), bb.top.toFloat(), paint)
                        if (editMode != EDIT_MODE.NONE) {
                            canvas.drawRect(bb, pp!!)
                        }
                    }
                }
            }
        }
        if (editMode != EDIT_MODE.NONE) {
            onDrawInEditMode(canvas)
        }
    }

    private fun onDrawInEditMode(canvas: Canvas) {
        val p = Paint()
        p.color = -0x777778
        p.alpha = 255
        val dstScreenShotRect = RectF()
        if (viewPortsEnvelops.size > 1 && lastGfxProfileName != null) {
            val bb = screenElement!!.boundingbox
            var env: RectF? = null
            var counter = 0
            for ((key, value) in viewPortsEnvelops) {
                if (key == lastGfxProfileName) {
                    env = value
                    break
                }
                counter++
            }
            dstScreenShotRect.left = bb.left + env!!.left * bb.width() + counter * 2 + 2
            dstScreenShotRect.top = bb.top + env.top * bb.height() + counter * 2 + 2
            dstScreenShotRect.right = bb.right - env.right * bb.width() - (counter * 2 + 1)
            dstScreenShotRect.bottom = bb.bottom - env.bottom * bb.height() - (counter * 2 + 1)
        } else {
            dstScreenShotRect.set(screenElement!!.boundingbox)
        }
        if (lastGameScreenshot != null && !lastGameScreenshot!!.isRecycled) {
            val src = Rect(0, 0, lastGameScreenshot!!.width, lastGameScreenshot!!.height)
            canvas.drawBitmap(lastGameScreenshot!!, src, dstScreenShotRect, p)
        } else {
            canvas.drawRect(dstScreenShotRect, p)
        }
        if (editMode == EDIT_MODE.TOUCH) {
            canvas.drawRect(screenElement!!.boundingbox, bitmapRectPaint)
        } else if (editMode == EDIT_MODE.SCREEN && viewPortsEnvelops.size > 1) {
            val rect = RectF()
            val bb = screenElement!!.boundingbox
            var counter = 0
            for ((key, env) in viewPortsEnvelops) {
                rect.left = bb.left + env.left * bb.width() + counter * 2 + 2
                rect.top = bb.top + env.top * bb.height() + counter * 2 + 2
                rect.right = bb.right - env.right * bb.width() - (counter * 2 + 1)
                rect.bottom = bb.bottom - env.bottom * bb.height() - (counter * 2 + 1)
                videoModeLabelPaint.color = VIDEOMODE_COLORS[counter % VIDEOMODE_COLORS.size]
                canvas.drawRect(rect, videoModeLabelPaint)
                videoModeLabelPaint.textAlign =
                    if (counter % 2 == 0) Paint.Align.LEFT else Paint.Align.RIGHT
                canvas.drawText(
                    key,
                    if (counter % 2 == 0) rect.left + videoModeLabelPaint.textSize / 4 else rect.right - videoModeLabelPaint.textSize / 4,
                    rect.bottom - videoModeLabelPaint.textSize / 4,
                    videoModeLabelPaint
                )
                counter++
            }
        }
        for (idx in boundingBoxs.indices) {
            val btn = btns[idx] as MultitouchBtnInterface
            if (btn.getId() == R.id.button_menu) {
                paint.alpha = 255
            } else {
                paint.alpha = if (editMode == EDIT_MODE.SCREEN) 64 else 255
            }
            btn.removeRequestRepaint()
            val b = if (btn.isPressed()) pressedButtonsBitmaps!![idx] else buttonsBitmaps!![idx]
            if (b != null) {
                val bb = boundingBoxs[idx]
                val bRect = Rect(0, 0, b.width, b.height)
                canvas.drawBitmap(b, bRect, bb!!, paint)
            }
        }
        editPaint.color = 0x55ff0000
        if (editMode == EDIT_MODE.TOUCH) {
            for (e in editElements) {
                if (e.movable) {
                    if (!e.validPosition) {
                        canvas.drawRect(e.boundingbox, editPaint)
                    }
                    canvas.drawRect(e.boundingbox, editElementPaint)
                    val r = e.getResizingBox()
                    canvas.drawBitmap(resizeIcon!!, r.left, r.top, editElementPaint)
                }
            }
        } else {
            val e = screenElement
            if (e!!.movable) {
                if (!e.validPosition) {
                    canvas.drawRect(e.boundingbox, editPaint)
                }
                canvas.drawRect(e.boundingbox, editElementPaint)
                val r = e.getResizingBox()
                canvas.drawBitmap(resizeIcon!!, r.left, r.top, editElementPaint)
            }
        }
    }

    private fun onTouchInEditMode(event: MotionEvent) {
        if (!isResizing) {
            onTouchInEditModeMove(event)
        } else {
            onTouchInEditModeResize(event)
        }
    }

    private fun onTouchCheck(e: EditElement?, idx: Int, x: Int, y: Int): Boolean {
        val boundingBox = e!!.boundingbox
        val resizingAnchor = e.getResizingBox()
        if (e.listener != null && boundingBox.contains(x.toFloat(), y.toFloat())) {
            e.listener!!.onClick()
        } else if ((resizingAnchor.contains(
                x.toFloat(),
                y.toFloat()
            ) || boundingBox.contains(x.toFloat(), y.toFloat())) && e.movable
        ) {
            lastValidBB.set(e.boundingbox)
            isResizing = resizingAnchor.contains(x.toFloat(), y.toFloat())
            selectIdx = idx
            selectW = boundingBox.width()
            selectH = boundingBox.height()
            startDragX = boundingBox.left
            startDragY = boundingBox.top
            startTouchX = x
            startTouchY = y
            startDragXoffset = boundingBox.right - x
            startDragYoffset = boundingBox.bottom - y
            if (isResizing) {
                e.resizeRects.clear()
                for (i in e.ids.indices) {
                    val id = e.ids[i]
                    e.resizeRects.add(RectF(boundingBoxs[id]))
                }
            }
            val invalR = Rect()
            boundingBox.round(invalR)
            invalidate(invalR)
            return true
        }
        return false
    }

    private fun onTouchInEditModeMove(event: MotionEvent) {
        val action = event.action
        val x = (event.x + 0.5f).toInt()
        val y = (event.y + 0.5f).toInt()
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                var idx = 0
                if (editMode == EDIT_MODE.TOUCH) {
                    for (e in editElements) {
                        if (onTouchCheck(e, idx, x, y)) {
                            break
                        }
                        idx++
                    }
                } else {
                    onTouchCheck(screenElement, 0, x, y)
                    onTouchCheck(menuElement, 0, x, y)
                }
            }

            MotionEvent.ACTION_MOVE -> if (selectIdx != -1) {
                val element: EditElement? = if (editMode == EDIT_MODE.TOUCH) {
                    editElements[selectIdx]
                } else {
                    screenElement
                }
                val elementBb = element!!.boundingbox
                val vx = x - startTouchX
                val vy = y - startTouchY
                val r = RectF(elementBb)
                val left = startDragX + vx
                val top = startDragY + vy
                r[left - 2, top - 2, left + selectW + 2] = top + selectH + 2
                element.validPosition = isRectValid(r, element)
                if (element.validPosition) {
                    lastValidBB[left, top, left + selectW] = top + selectH
                }
                r[left - 10, top - 10, left + selectW + 10] = top + selectH + 10
                val tempRect = Rect()
                r.round(tempRect)
                invalidate(tempRect)
                element.boundingbox[r.left + 10, r.top + 10, r.right - 10] = r.bottom - 10
                if (editMode == EDIT_MODE.TOUCH) recomputeBtn(element)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                endMovementCheck()
            }
        }
    }

    private fun onTouchInEditModeResize(event: MotionEvent) {
        val action = event.action
        val x = (event.x + 0.5f).toInt()
        lastTouchX = x
        lastTouchY = Math.round(event.y)
        when (action) {
            MotionEvent.ACTION_MOVE -> if (selectIdx != -1) {
                val element =
                    if (editMode == EDIT_MODE.TOUCH) editElements[selectIdx] else screenElement!!
                val elementBb = element.boundingbox
                val newW = x - startDragX + startDragXoffset
                val scaleFactorW = newW / selectW
                elementBb[startDragX, startDragY, x + startDragXoffset] =
                    startDragY + selectH * scaleFactorW
                if (editMode == EDIT_MODE.TOUCH) recomputeBtn(element)
                element.validPosition = isRectValid(elementBb, element)
                if (element.validPosition) {
                    lastValidBB.set(element.boundingbox)
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                isResizing = false
                endMovementCheck()
            }
        }
    }

    private fun recomputeBtn(element: EditElement?) {
        val scaleFactor = element!!.boundingbox.width() / element.boundingboxHistory.width()
        for (i in element.ids.indices) {
            val id = element.ids[i]
            val offset = RectF(element.offsetshistory[i])
            val bb = RectF(element.boundingboxsHistory[i])
            val elemBB = element.boundingboxHistory
            bb.offset(-elemBB.left, -elemBB.top)
            bb.left *= scaleFactor
            bb.top *= scaleFactor
            bb.right *= scaleFactor
            bb.bottom *= scaleFactor
            offset.left *= scaleFactor
            offset.top *= scaleFactor
            element.offsets[i].set(offset)
            bb.offset(element.boundingbox.left, element.boundingbox.top)
            bb.round(boundingBoxs[id]!!)
        }
    }

    private fun endMovementCheck() {
        if (selectIdx != -1) {
            val element =
                if (editMode == EDIT_MODE.TOUCH) editElements[selectIdx] else screenElement!!
            if (!element.validPosition) {
                element.boundingbox.set(lastValidBB)
            }
            if (editMode == EDIT_MODE.TOUCH) recomputeBtn(element)
            element.validPosition = true
            selectIdx = -1
        }
        invalidate()
    }

    private fun isRectValid(r: RectF, element: EditElement?): Boolean {
        var isvalid = true
        val globalBox = RectF(0f, 0f, touchMapWidth.toFloat(), touchMapHeight.toFloat())
        if (globalBox.contains(r)) {
            if (editMode == EDIT_MODE.TOUCH) {
                for (el in editElements) {
                    if (el !== element && RectF.intersects(r, el.boundingbox)) {
                        isvalid = false
                        break
                    }
                }
            }
        } else {
            isvalid = false
        }
        if (element!!.boundingbox.width() < element.minimalSize ||
            element.boundingbox.height() < element.minimalSize
        ) {
            isvalid = false
        }
        return isvalid
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        for (b in buttonsBitmaps!!) {
            b?.recycle()
        }
        for (b in pressedButtonsBitmaps!!) {
            b?.recycle()
        }
        buttonsBitmaps = null
        pressedButtonsBitmaps = null
        i(TAG, "on detach")
    }

    private fun onTouchEnter(btnIdx: Int, event: MotionEvent) {
        val btn = btns[btnIdx] as MultitouchBtnInterface
        btn.onTouchEnter(event)
        btn.requestRepaint()
        invalidate(boundingBoxs!![btnIdx])
        if (btn is MultitouchTwoButtonArea) {
            val mtba = btn
            val idx1 = ridToIdxMap[mtba.firstBtnRID]
            val idx2 = ridToIdxMap[mtba.secondBtnRID]
            invalidate(boundingBoxs!![idx1])
            invalidate(boundingBoxs!![idx2])
        } else if (btn is MultitouchTwoButton) {
            val mtba = btn
            val idx1 = ridToIdxMap[mtba.firstBtnRID]
            val idx2 = ridToIdxMap[mtba.secondBtnRID]
            invalidate(boundingBoxs!![idx1])
            invalidate(boundingBoxs!![idx2])
        }
    }

    private fun onTouchExit(btnIdx: Int, event: MotionEvent) {
        val btn = btns[btnIdx] as MultitouchBtnInterface
        btn.onTouchExit(event)
        invalidate(boundingBoxs!![btnIdx])
        btn.requestRepaint()
        if (btn is MultitouchTwoButtonArea) {
            val mtba = btn
            val idx1 = ridToIdxMap[mtba.firstBtnRID]
            val idx2 = ridToIdxMap[mtba.secondBtnRID]
            invalidate(boundingBoxs!![idx1])
            invalidate(boundingBoxs!![idx2])
        } else if (btn is MultitouchTwoButton) {
            val mtba = btn
            val idx1 = ridToIdxMap[mtba.firstBtnRID]
            val idx2 = ridToIdxMap[mtba.secondBtnRID]
            invalidate(boundingBoxs!![idx1])
            invalidate(boundingBoxs!![idx2])
        }
    }

    fun setLastgameScreenshot(bitmap: Bitmap?, gfxProfileName: String) {
        i(TAG, "set last profile:$gfxProfileName")
        lastGameScreenshot = bitmap
        lastGfxProfileName = gfxProfileName
        initScreenElement(false)
        invalidate()
    }

    private fun initScreenElement(reset: Boolean) {
        val topPadding = resources.getDimensionPixelSize(R.dimen.top_panel_touch_controller_height)
        var viewPorts: HashMap<String, ViewPort>? = null
        val vport:ViewPort = if (reset) {

            viewPorts = computeAllInitViewPorts(
                context,
                cacheW,
                cacheH,
                0,
                if (cacheRotation == 0) topPadding else 0
            )
            computeInitViewPort(
                context,
                cacheW,
                cacheH,
                0,
                if (cacheRotation == 0) topPadding else 0
            )
        } else {
            viewPorts = loadOrComputeAllViewPorts(
                context,
                cacheW,
                cacheH,
                0,
                if (cacheRotation == 0) topPadding else 0
            )
            loadOrComputeViewPort(
                context,
                null,
                cacheW,
                cacheH,
                0,
                if (cacheRotation == 0) topPadding else 0,
                true
            )?:return
        }
        val viewPort =
            Rect(vport.x + 1, vport.y, vport.x + vport.width - 1, vport.y + vport.height - 1)
        if (editMode != EDIT_MODE.NONE) {
            if (editMode == EDIT_MODE.SCREEN) {
                for (port in viewPorts.values) {
                    viewPort.left = if (port.x < viewPort.left) port.x else viewPort.left
                    viewPort.top = if (port.y < viewPort.top) port.y else viewPort.top
                    val right = port.x + port.width
                    viewPort.right = if (right > viewPort.right) right else viewPort.right
                    val bottom = port.y + port.height
                    viewPort.bottom = if (bottom > viewPort.bottom) bottom else viewPort.bottom
                }
            } else if (lastGfxProfileName != null) {
                val port = viewPorts[lastGfxProfileName]
                if (port != null) {
                    viewPort.left = port.x
                    viewPort.top = port.y
                    viewPort.right = port.x + port.width
                    viewPort.bottom = port.y + port.height
                }
            }
            viewPortsEnvelops = HashMap(viewPorts.size)
            for ((key, port) in viewPorts) {
                val w = viewPort.width().toFloat()
                val h = viewPort.height().toFloat()
                val relativeLeft = (-viewPort.left + port.x) / w
                val relativeTop = (-viewPort.top + port.y) / h
                val relativeRight = (viewPort.right - (port.x + port.width)) / w
                val relativeBottom = (viewPort.bottom - (port.y + port.height)) / h
                val envelop = RectF(relativeLeft, relativeTop, relativeRight, relativeBottom)
                viewPortsEnvelops[key] = envelop
            }
        }
        val topOffset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            20f, resources.displayMetrics
        ).toInt()
        viewPort.top -= if (cacheRotation == 0) topOffset else 0
        viewPort.bottom -= if (cacheRotation == 0) topOffset else 0
        i(
            TAG, "init screenlayout " + info!!.defaultGfxProfile.name
                    + " vp:" + viewPort.left + "," + viewPort.top + "," + viewPort.width()
                    + "," + viewPort.height()
        )
        screenElement = EditElement(viewPort)
    }

    fun stopEditMode() {
        timer.cancel()
    }

    fun resetEditElement(gameHash: String?) {
        for (element in editElements) {
            element.boundingbox.set(element.boundingboxHistory)
            for (i in element.ids.indices) {
                val bb = boundingBoxs!![element.ids[i]]
                bb!!.set(element.boundingboxsHistory[i])
                element.offsets[i].set(element.offsetshistory[i])
            }
        }
        invalidate()
        val edit = pref.edit()
        edit.clear()
        edit.apply()
    }

    fun resetScreenElement() {
        initScreenElement(true)
        removeViewPortSave(context)
    }

    fun disableLoadSettings() {
        loadingSettings = false
        for (element in editElements) {
            element.boundingbox.set(element.boundingboxHistory)
            for (i in element.ids.indices) {
                val bb = boundingBoxs!![element.ids[i]]
                bb!!.set(element.boundingboxsHistory[i])
                element.offsets[i].set(element.offsetshistory[i])
            }
        }
        invalidate()
    }

    private val pref: SharedPreferences
        get() {
            if (cacheRotation == -1) {
                val mWindowManager = context
                    .getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val mDisplay = mWindowManager.defaultDisplay
                cacheRotation = mDisplay.rotation % 2
                cacheW = getDisplayWidth(mDisplay)
                cacheH = getDisplayHeight(mDisplay)
            }
            return context.getSharedPreferences(getPrefName(cacheRotation), Context.MODE_PRIVATE)
        }

    fun saveScreenElement() {
        endMovementCheck()
        val bb = screenElement!!.boundingbox
        val env = viewPortsEnvelops[info!!.defaultGfxProfile.name]
        val rect = Rect()
        rect.left = Math.round(bb.left + env!!.left * bb.width())
        rect.top = Math.round(bb.top + env.top * bb.height())
        rect.right = Math.round(bb.right - env.right * bb.width())
        rect.bottom = Math.round(bb.bottom - env.bottom * bb.height())
        val topOffset = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f,
            resources.displayMetrics
        ).toInt()
        val vp = ViewPort()
        vp.x = rect.left
        vp.y = rect.top + if (cacheRotation == 0) topOffset else 0
        vp.width = rect.width()
        vp.height = rect.height()
        i(
            TAG, "save screenlayout " + info!!.defaultGfxProfile.name
                    + " vp:" + vp.x + "," + vp.y + "," + vp.width + "," + vp.height
        )
        setViewPort(context, vp, cacheW, cacheH)
    }

    fun saveEditElements() {
        endMovementCheck()
        val pref = pref
        val editor = pref.edit()
        for (i in btns.indices) {
            val btn = btns[i]
            val offset = boundingBoxs[i]
            val s =
                offset!!.left.toString() + "-" + offset.top + "-" + offset.right + "-" + offset.bottom
            val id = btnIdMap.indexOf(btn.id)
            editor.putString(id.toString() + "", s)
        }
        editor.apply()
    }

    private fun loadEditElements(unused: String): Boolean {
        return if (!loadingSettings) {
            false
        } else {
            var pref = pref
            val prefMap = pref.all
            for (key in prefMap.keys) {
                try {
                    val id = key.toInt()
                    if (id > 100) {
                        i(TAG, "Detect old MTL format($id)!\nTrying repaire it")
                        remapOldMTLprefToNew(pref, prefMap)
                        break
                    }
                } catch (e: NumberFormatException) {
                    val editor = pref.edit()
                    editor.clear()
                    editor.apply()
                    break
                }
            }
            pref = this.pref
            if (pref.all.isEmpty()) {
                i(TAG, "neni ulozene nastaveni")
                for (elem in editElements) {
                    elem.computeBoundingBox()
                    elem.computeOffsets()
                }
                false
            } else {
                var isNew = false
                for (i in btns.indices) {
                    val btn = btns[i]
                    val id = btnIdMap.indexOf(btn.id)
                    val s = pref.getString("" + id, "")
                    if (s != "") {
                        val sa =
                            s!!.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val bb = boundingBoxs!![ridToIdxMap[btn.id]]
                        val left = sa[0].toInt()
                        val top = sa[1].toInt()
                        val right = sa[2].toInt()
                        val bottom = sa[3].toInt()
                        if (bb!!.left != left || bb.top != top || bb.right != right || bb.bottom != bottom) {
                            bb[left, top, right] = bottom
                            i(TAG, hashCode().toString() + " detect change layout")
                            isNew = true
                        }
                    }
                }
                for (elem in editElements) {
                    elem.computeBoundingBox()
                    elem.computeOffsets()
                }
                //NLog.i(TAG, hashCode() + " isNew:" + isNew + " " + btns.size() + " " + Arrays.toString(boundingBoxs));
                checkFastForwardButton()
                isNew
            }
        }
    }

    private fun checkFastForwardButton() {
        val idx = ridToIdxMap[R.id.button_fast_forward]
        val ff_bb = boundingBoxs[idx]
        //NLog.i(TAG, "fast forward btn " + idx + " rect " + ff_bb);
        for (bb2 in boundingBoxs) {
            if (ff_bb !== bb2 && Rect.intersects(ff_bb!!, bb2!!)) {
                //NLog.i(TAG, "colision with " + bb2);
                val w = measuredWidth
                val h = measuredHeight
                var wrongPosition = false
                for (i in 0..299) {
                    wrongPosition = false
                    ff_bb.offset(10, 0)
                    if (ff_bb.right >= w) {
                        ff_bb.offsetTo(0, ff_bb.top + 10)
                        if (ff_bb.bottom >= h) {
                            break
                        }
                    }
                    //NLog.i(TAG, i + " new rect " + ff_bb);
                    for (bb3 in boundingBoxs) {
                        if (ff_bb !== bb3 && Rect.intersects(ff_bb, bb3!!)) {
                            //NLog.i(TAG, "colision with " + bb3);
                            wrongPosition = true
                            break
                        }
                    }
                    if (!wrongPosition) {
                        break
                    }
                }
                if (wrongPosition) {
                    i(TAG, "Nepodarilo se najit vhodnou pozici")
                    resetEditElement("")
                } else {
                    i(
                        TAG, "Podarilo se najit vhodnou pozici " + ff_bb + " "
                                + boundingBoxs[btnIdMap.indexOf(R.id.button_fast_forward)]
                    )
                    for (elem in editElements) {
                        elem.computeBoundingBox()
                        elem.computeOffsets()
                    }
                }
            }
        }
    }

    fun setEnableStaticDPAD(isEnable: Boolean) {
        staticDPADEnabled = isEnable
        for (btn in btns) {
            if (dpadRIDs.contains(btn.id)) {
                btn.visibility = if (isEnable) VISIBLE else INVISIBLE
                btn.isEnabled = isEnable
            }
        }
        invalidate()
    }

    enum class EDIT_MODE {
        NONE, TOUCH, SCREEN
    }

    interface OnEditItemClickListener {
        fun onClick()
    }

    inner class EditElement {
        var boundingbox = RectF()
        var ids = ArrayList<Int>()
        var offsets = ArrayList<RectF>()
        var resizeRects = ArrayList<RectF>()
        var movable = true
        var boundingboxHistory = RectF()
        var boundingboxsHistory = ArrayList<Rect>()
        var offsetshistory = ArrayList<RectF>()
        var validPosition = true
        var minimalSize: Float
        var isScreenElement = false
        private val resizingBox = RectF()
        var listener: OnEditItemClickListener? = null

        constructor(rid: Int, movable: Boolean, minimalSize: Float) {
            val idx = ridToIdxMap[rid]
            if (idx != -1) {
                ids.add(idx)
                boundingbox.set(boundingBoxs!![idx]!!)
                boundingboxHistory.set(boundingbox)
            }
            computeOffsets()
            this.movable = movable
            this.minimalSize = minimalSize
        }

        constructor(viewPort: Rect?) {
            isScreenElement = true
            boundingbox.set(viewPort!!)
            boundingboxHistory.set(viewPort)
            computeOffsets()
            movable = true
            minimalSize = 200f
        }

        fun getResizingBox(): RectF {
            val K = resizeIcon!!.height / if (isScreenElement) 1 else 2
            resizingBox[boundingbox.right - K, boundingbox.bottom - K, boundingbox.right + K] =
                boundingbox.bottom + K
            return resizingBox
        }

        fun add(rid: Int) {
            val idx = ridToIdxMap[rid]
            ids.add(idx)
            val tmp = RectF()
            tmp.set(boundingBoxs!![idx]!!)
            boundingbox.union(tmp)
            boundingboxHistory.set(boundingbox)
            computeOffsets()
        }

        fun computeOffsets() {
            offsets.clear()
            if (isScreenElement) {
                val offset = RectF(boundingbox.left, boundingbox.top, 0f, 0f)
                offsets.add(offset)
            } else {
                for (id in ids) {
                    val r = boundingBoxs!![id]
                    val offset = RectF(r!!.left - boundingbox.left, r.top - boundingbox.top, 0f, 0f)
                    offsets.add(offset)
                }
            }
        }

        fun computeBoundingBox() {
            if (!isScreenElement) {
                boundingbox.set(boundingBoxs!![ids[0]]!!)
                for (id in ids) {
                    val r = boundingBoxs!![id]
                    val tmp = RectF()
                    tmp.set(r!!)
                    boundingbox.union(tmp)
                }
            }
        }

        fun saveHistory(): EditElement {
            boundingboxsHistory.clear()
            offsetshistory.clear()
            if (isScreenElement) {
            } else {
                for (i in offsets.indices) {
                    val id = ids[i]
                    boundingboxsHistory.add(Rect(boundingBoxs!![id]))
                    offsetshistory.add(RectF(offsets[i]))
                }
            }
            return this
        }

        fun setOnClickListener(listener: OnEditItemClickListener?) {
            this.listener = listener
        }
    }

    companion object {
        private const val TAG = "MultitouchLayer"
        private const val EMPTY_COLOR = 0x00
        private const val BUTTON_MIN_SIZE_DP = 20
        private val VIDEOMODE_COLORS = intArrayOf(-0x7800, -0x663400)
        private const val MAX_POINTERS = 6
        private const val COUNT_SKIP_MOVE_EVENT = 3
        fun getPrefName(rot: Int): String {
            return "-mtl-" + Integer.toString(rot) + ".settings"
        }
    }
}