package com.antourage.weaverlib.other

import android.animation.Animator
import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewAnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.hypot


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
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH)
    return try {
       df.parse(utcTime)
    } catch (e: ParseException) {
        e.printStackTrace()
        null
    }
}

internal fun circularRevealOverlay(overlay: View, fab: View) {
    val cx = (fab.x + 3*(fab.width/4))
    val cy = (fab.y + 2*fab.height/3)
    val anim: Animator = ViewAnimationUtils.createCircularReveal(
        overlay, cx.toInt(), cy.toInt(), 0.0f,
        hypot(
            cx.toDouble(),
            cy.toDouble()
        ).toFloat()
    )
    overlay.visibility = ConstraintLayout.VISIBLE
    anim.duration = 500
    anim.start()
}

internal fun circularHideOverlay(overlay: View, fab: View) {
    val cx = (fab.x + 3*(fab.width/4))
    val cy = (fab.y + 2*fab.height/3)
    val anim: Animator = ViewAnimationUtils.createCircularReveal(
        overlay, cx.toInt(), cy.toInt(), hypot(
            cx.toDouble(),
            cy.toDouble()
        ).toFloat(), 0.0f
    )
    anim.duration = 500
    anim.addListener(object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            overlay.visibility = ConstraintLayout.GONE
        }
    })
    anim.start()
}

internal fun getSecondsDateDiff(date1: Date, date2: Date): Long {
    val diffInMillies = date2.time - date1.time
    return TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS)
}