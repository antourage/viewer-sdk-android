package com.antourage.weaverlib.other

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.DisplayMetrics
import android.util.TypedValue
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal fun dp2px(context: Context, dipValue: Float): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
}

internal fun px2dp(context: Context, px: Float): Float {
    return px / (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}

internal fun calculatePlayerHeight(activity: Activity): Float {
    val width = getScreenWidth(activity)
    return ((width * 9.0f) / 16.0f)
}

internal fun getScreenWidth(activity: Activity): Int {
    val display = activity.windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x
}

internal fun convertUtcToLocal(utcTime: String): Date? {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)
    df.timeZone = TimeZone.getTimeZone("UTC")
    val date: Date
    return try {
        date = df.parse(utcTime)
        df.timeZone = TimeZone.getDefault()
        df.parse(df.format(date))
    } catch (e: ParseException) {
        e.printStackTrace()
        null
    }
}

internal fun getSecondsDateDiff(date1: Date, date2: Date): Long {
    val diffInMillies = date2.time - date1.time
    return TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS)
}