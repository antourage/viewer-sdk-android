package com.antourage.weaverlib.other

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.NonNull
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewTreeObserver
import com.antourage.weaverlib.R
import java.text.SimpleDateFormat
import java.util.*

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
    val MINUTE = 60
    val HOUR = 60*MINUTE
    val DAY = HOUR * 24
    val localUTC = convertUtcToLocal(this)
    if (localUTC != null) {
        var diff = getDateDiff(localUTC, Date())
        if (diff > 3 * DAY) {
            val df = SimpleDateFormat("dd.MM.yyyy")
            return  df.format(localUTC)
        } else if (diff > DAY) {
            val days = (diff / DAY).toInt()
            val amount = context.resources.getQuantityString(R.plurals.days, days, days)
            return context.getString(R.string.started_ago, amount)
        } else if (diff > HOUR) {
            val hours = (diff / HOUR).toInt()
            val amount = context.resources.getQuantityString(R.plurals.hours, hours, hours)
            return context.getString(R.string.started_ago, amount)
        }else if(diff>MINUTE){
            val minute = (diff / MINUTE).toInt()
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