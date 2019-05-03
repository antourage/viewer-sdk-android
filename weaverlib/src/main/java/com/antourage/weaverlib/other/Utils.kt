package com.antourage.weaverlib.other.networking

import android.content.Context
import android.util.TypedValue

fun dp2px(context: Context, dipValue: Float): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
}