package com.antourage.weaverlib.other

import android.app.Application
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.NonNull
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import com.antourage.weaverlib.R
import java.text.SimpleDateFormat
import java.util.*
import android.view.ViewGroup
import com.antourage.weaverlib.di.ApplicationComponent
import com.antourage.weaverlib.di.DaggerApplicationComponent


inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

fun AppCompatActivity.replaceFragment(fragment: Fragment, frameId: Int, addToBackStack: Boolean = false) {
    if (addToBackStack) {
        supportFragmentManager.inTransaction {
            replace(frameId, fragment).addToBackStack(fragment.javaClass.simpleName)
        }
    } else
        supportFragmentManager.inTransaction {
            replace(frameId, fragment)
        }
}

//Did not wanted to use Application class(problem with merging), decided on extension function
fun Application.initDagger(): ApplicationComponent {
    return DaggerApplicationComponent.builder()
        .application(this)
        .build()
}

fun Fragment.replaceFragment(fragment: Fragment, frameId: Int, addToBackStack: Boolean = false) {
    (activity as AppCompatActivity).replaceFragment(fragment, frameId, addToBackStack)
}

fun Fragment.replaceChildFragment(fragment: Fragment, frameId: Int) {
    childFragmentManager.inTransaction { replace(frameId, fragment) }
}

fun Fragment.replaceChildFragment(fragment: Fragment, frameId: Int, addToBackStack: Boolean = false) {
    if (addToBackStack) {
        childFragmentManager.inTransaction { replace(frameId, fragment).addToBackStack(fragment.javaClass.simpleName) }
    } else
        replaceChildFragment(fragment, frameId)
}

fun <T> LiveData<T>.reobserve(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<T>) {
    this.removeObserver(observer)
    this.observe(owner, observer)
}

fun String.parseDate(context: Context): String {
    val secondsInMinute = 60
    val minutesInHour = 60*secondsInMinute
    val hoursInDay = minutesInHour * 24
    val localUTC = convertUtcToLocal(this)
    if (localUTC != null) {
        var diff = getDateDiff(localUTC, Date())
        if (diff > 3 * hoursInDay) {
            val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return  df.format(localUTC)
        } else if (diff > hoursInDay) {
            val days = (diff / hoursInDay).toInt()
            val amount = context.resources.getQuantityString(R.plurals.days, days, days)
            return context.getString(R.string.started_ago, amount)
        } else if (diff > minutesInHour) {
            val hours = (diff / minutesInHour).toInt()
            val amount = context.resources.getQuantityString(R.plurals.hours, hours, hours)
            return context.getString(R.string.started_ago, amount)
        }else if(diff>secondsInMinute){
            val minute = (diff / secondsInMinute).toInt()
            val amount = context.resources.getQuantityString(R.plurals.minutes,minute,minute)
            return context.getString(R.string.started_ago, amount)

        } else {
            if (diff == 0L) {
                diff = 1
            }
            val amount = context.resources
                .getQuantityString(R.plurals.seconds, diff.toInt(), diff.toInt())
            return context.getString(R.string.started_ago, amount)
        }
    } else {
        return ""
    }
}

fun <T : View> T.trueWidth(function: (Int) -> Unit) {
    if (width == 0)
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                function(width)
            }
        })
    else function(width)
}

fun <T: View> T.setMargins(left:Int, top:Int, right:Int, bottom:Int){
    if (this.layoutParams is ViewGroup.MarginLayoutParams) {
        val p = this.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(left, top, right, bottom)
        this.requestLayout()
    }
}