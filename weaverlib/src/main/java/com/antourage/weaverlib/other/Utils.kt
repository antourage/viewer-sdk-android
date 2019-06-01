package com.antourage.weaverlib.other

import android.content.Context
import android.util.TypedValue
import android.R.attr.x
import android.app.Activity
import android.graphics.Point
import android.view.Display



fun dp2px(context: Context, dipValue: Float): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
}

fun calculatePlayerHeight(activity: Activity):Float{
    val display = activity.windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    val width = size.x
    return ((width*10.0f) /16.0f)
}