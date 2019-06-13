package com.antourage.weaverlib.other

import android.content.Context
import android.util.TypedValue
import android.R.attr.x
import android.app.Activity
import android.graphics.Point
import android.view.Display
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


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