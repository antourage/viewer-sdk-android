package com.antourage.weaverlib.other.ui

import android.content.Context
import android.content.res.Configuration
import android.support.v4.widget.DrawerLayout
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP

/**
 * had to create custom, crush thrown in onMeasure method with default
 */

class CustomDrawerLayout : DrawerLayout {

    var touchListener: DrawerTouchListener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

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
        if (ev.action == ACTION_UP)
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                touchListener?.onDrawerTouch()
            }
        return super.onInterceptTouchEvent(ev)
    }

    interface DrawerTouchListener {
        fun onDrawerTouch()
    }

}