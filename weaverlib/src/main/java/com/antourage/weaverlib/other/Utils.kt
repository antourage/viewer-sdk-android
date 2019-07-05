package com.antourage.weaverlib.other

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.util.TypedValue
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


fun dp2px(context: Context, dipValue: Float): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics)
}


fun calculatePlayerHeight(activity: Activity): Float {
    val width = getScreenWidth(activity)
    return ((width * 9.0f) / 16.0f)
}

fun getScreenWidth(activity: Activity): Int {
    val display = activity.windowManager.defaultDisplay
    val size = Point()
    display.getSize(size)
    return size.x
}

fun convertUtcToLocal(utcTime: String): Date? {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.ENGLISH)
    df.timeZone = TimeZone.getTimeZone("UTC")
    val date: Date
    try {
        date = df.parse(utcTime)
        df.timeZone = TimeZone.getDefault()
        return df.parse(df.format(date))
    } catch (e: ParseException) {
        e.printStackTrace()
        return null
    }
}

fun getDateDiff(date1: Date, date2: Date): Long {
    val diffInMillies = date2.time - date1.time
    return TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS)
}

fun generateRandomViewerNumber(): Int {
    val min = 20
    val max = 1000
    return Random().nextInt(max - min + 1) + min
}