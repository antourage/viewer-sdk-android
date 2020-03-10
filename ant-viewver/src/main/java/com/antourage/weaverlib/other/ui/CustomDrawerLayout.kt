package com.antourage.weaverlib.other.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.view.GestureDetectorCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GravityCompat

internal class CustomDrawerLayout : DrawerLayout {

    var touchListener: DrawerTouchListener? = null
    var doubleTapListener: DrawerDoubleTapListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    fun isOpened() = isDrawerOpen(GravityCompat.START)

    private val gestureDetector =
        GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                touchListener?.onDrawerSingleClick()
                return super.onSingleTapConfirmed(e)
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                doubleTapListener?.onDrawerDoubleClick()
                return super.onDoubleTap(e)
            }
        })

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val orientation = context.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            gestureDetector.onTouchEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        val orientation = context.resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            gestureDetector.onTouchEvent(ev)
        return super.onTouchEvent(ev)
    }

    interface DrawerTouchListener {
        fun onDrawerSingleClick()
    }

    interface DrawerDoubleTapListener {
        fun onDrawerDoubleClick()
    }
}