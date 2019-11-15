package com.antourage.weaverlib.other.ui

import android.content.Context
import android.content.res.Configuration
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.widget.DrawerLayout
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent

/**
 * had to create custom, crush thrown in onMeasure method with default
 */

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY
        )
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY
        )
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

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