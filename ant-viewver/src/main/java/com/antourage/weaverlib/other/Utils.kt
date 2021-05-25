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
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH)
    return try {
       df.parse(utcTime)
    } catch (e: ParseException) {
        e.printStackTrace()
        null
    }
}

internal fun circularRevealView(viewToReveal: View, anchorView: View) {
    val cx = (anchorView.left + anchorView.right) / 2
    val cy = (anchorView.top + anchorView.bottom) / 2
    val anim: Animator = ViewAnimationUtils.createCircularReveal(
        viewToReveal, cx, cy, 0.0f,
        hypot(
            cx.toDouble(),
            cy.toDouble()
        ).toFloat()
    )
    viewToReveal.visibility = ConstraintLayout.VISIBLE
    anim.duration = 400
    anim.start()
}

internal fun circularHideView(viewToHide: View, anchorView: View) {
    val cx = (anchorView.left + anchorView.right) / 2
    val cy = (anchorView.top + anchorView.bottom) / 2
    val anim: Animator = ViewAnimationUtils.createCircularReveal(
        viewToHide, cx, cy, hypot(
            cx.toDouble(),
            cy.toDouble()
        ).toFloat(), 0.0f
    )
    anim.duration = 400
    anim.addListener(object : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {}
        override fun onAnimationCancel(animation: Animator?) {}
        override fun onAnimationStart(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator?) {
            viewToHide.visibility = ConstraintLayout.GONE
        }
    })
    anim.start()
}

internal fun getSecondsDateDiff(date1: Date, date2: Date): Long {
    val diffInMillies = date2.time - date1.time
    return TimeUnit.SECONDS.convert(diffInMillies, TimeUnit.MILLISECONDS)
}

internal fun isAppInstalledFromGooglePlay(context: Context): Boolean{
    val adb = "adb"
    val installer1 = "com.google.android.packageinstaller"
    val installer2 = "com.android.packageinstaller"

    val installerPackageName = context.packageManager.getInstallerPackageName(context.packageName)

    var result = false
        try {
            // if installer string is not null it might be installed by market
            if (!TextUtils.isEmpty(installerPackageName)) {
                result = true

                // on Android Nougat and up when installing an app through the package installer, the installer will be
                // "com.google.android.packageinstaller" or "com.android.packageinstaller" which is also not to be considered as a market installation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (TextUtils.equals(
                        installerPackageName,
                        installer1
                    ) || TextUtils.equals(installerPackageName, installer2))
                ) {
                    result = false
                }

                // on some devices (Xiaomi) the installer identifier will be "adb", which is not to be considered as a market installation
                if (TextUtils.equals(installerPackageName, adb)) {
                    result = false
                }
            }
        } catch (ignored: Throwable) {
            ignored.printStackTrace()
        }

    return result
}