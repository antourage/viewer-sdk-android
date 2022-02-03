package com.antourage.weaverlib.other

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.Constants.secInDay
import com.antourage.weaverlib.other.Constants.secInHour
import com.antourage.weaverlib.other.Constants.secInMin
import com.antourage.weaverlib.other.Constants.secInMonth
import com.antourage.weaverlib.other.Constants.secInWeek
import com.antourage.weaverlib.other.Constants.secInYear
import com.antourage.weaverlib.other.models.StreamResponseType
import java.text.SimpleDateFormat
import java.util.*

internal inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

internal fun AppCompatActivity.replaceFragment(
    fragment: Fragment,
    frameId: Int,
    addToBackStack: Boolean = false,
    slideFromBottom: Boolean = false
) {
    if (addToBackStack) {
        supportFragmentManager.inTransaction {
            if (slideFromBottom) {
                setCustomAnimations(
                    R.anim.antourage_slide_up,
                    R.anim.antourage_replace_fade_out,
                    0,
                    R.anim.antourage_slide_down
                )
            }
            replace(frameId, fragment).addToBackStack(fragment.javaClass.simpleName)
        }
    } else
        supportFragmentManager.inTransaction {
            if (slideFromBottom) {
                setCustomAnimations(
                    R.anim.antourage_slide_up,
                    R.anim.antourage_replace_fade_out,
                    0,
                    R.anim.antourage_slide_down
                )
            }
            replace(frameId, fragment)
        }
}

internal fun Fragment.replaceFragment(
    fragment: Fragment,
    frameId: Int,
    addToBackStack: Boolean = false,
    slideFromBottom: Boolean = false
) {
    (activity as AppCompatActivity).replaceFragment(
        fragment,
        frameId,
        addToBackStack,
        slideFromBottom
    )
}

internal fun Fragment.replaceChildFragment(
    fragment: Fragment,
    frameId: Int,
    addToBackStack: Boolean = false
) {
    if (addToBackStack) {
        childFragmentManager.inTransaction {
            replace(
                frameId,
                fragment
            ).addToBackStack(fragment.javaClass.simpleName)
        }
    } else
        childFragmentManager.inTransaction { replace(frameId, fragment) }
}

internal fun <T> LiveData<T>.reObserve(
    @NonNull owner: LifecycleOwner,
    @NonNull observer: Observer<T>
) {
    this.removeObserver(observer)
    this.observe(owner, observer)
}

internal fun <T> LiveData<T>.reObserveForever(@NonNull observer: Observer<T>) {
    this.removeObserver(observer)
    this.observeForever(observer)
}

internal fun String.parseDate(context: Context): String {
    val localUTC = convertUtcToLocal(this)
    return localUTC?.parseToDisplayAgoTime(context) ?: ""
}

internal fun String.parseDateLong(context: Context): String {
    val localUTC = convertUtcToLocal(this)
    return localUTC?.parseToDisplayAgoTimeLong(context) ?: ""
}

internal fun Date.parseToDisplayMessagesAgoTimeLong(context: Context): String {
    val diff = getSecondsDateDiff(this, Date())
    val timeAgo: String
    timeAgo = when {
        diff > secInHour -> {
            val hours = (diff / secInHour).toInt()
            context.resources.getQuantityString(R.plurals.ant_hours_long, hours, hours)
        }
        diff > secInMin -> {
            val minute = (diff / secInMin).toInt()
            context.resources.getQuantityString(R.plurals.ant_minutes_long, minute, minute)
        }

        diff > 10 -> {
            return context.getString(R.string.ant_recent)
        }

        else -> {
            return context.getString(R.string.ant_started_now)
        }
    }
    return context.getString(R.string.ant_started_ago, timeAgo)
}

internal fun Date.parseToDisplayAgoTime(context: Context): String {
    val secondsInMinute = 60
    val minutesInHour = 60 * secondsInMinute
    val hoursInDay = minutesInHour * 24
    val diff = getSecondsDateDiff(this, Date())
    when {
        diff > 7 * hoursInDay -> {
            val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return df.format(this)
        }
        diff > hoursInDay -> {
            val days = (diff / hoursInDay).toInt()
            val amount = context.resources.getQuantityString(R.plurals.ant_days, days, days)
            return context.getString(R.string.ant_started_ago, amount)
        }
        diff > minutesInHour -> {
            val hours = (diff / minutesInHour).toInt()
            val amount = context.resources.getQuantityString(R.plurals.ant_hours, hours, hours)
            return context.getString(R.string.ant_started_ago, amount)
        }
        diff > secondsInMinute -> {
            val minute = (diff / secondsInMinute).toInt()
            val amount =
                context.resources.getQuantityString(R.plurals.ant_minutes, minute, minute)
            return context.getString(R.string.ant_started_ago, amount)

        }
        else -> {
            if (diff <= 0L) {
                return "now"
            }
            val amount = context.resources
                .getQuantityString(R.plurals.ant_seconds, diff.toInt(), diff.toInt())
            return context.getString(R.string.ant_started_ago, amount)
        }
    }
}

internal fun Date.parseToDisplayAgoTimeLong(
    context: Context,
    type: StreamResponseType? = null
): String {
    val diff = getSecondsDateDiff(this, Date())
    val prefix = when (type) {
        StreamResponseType.POST -> context.resources.getString(R.string.ant_prefix_posted)
        StreamResponseType.VOD -> context.resources.getString(R.string.ant_prefix_live)
        StreamResponseType.UPLOADED_VIDEO -> context.resources.getString(R.string.ant_prefix_posted)
        null -> null
    }
    val timeAgo: String = when {
        diff > secInYear -> {
            val days = (diff / secInYear).toInt()
            context.resources.getQuantityString(R.plurals.ant_years_long, days, days)
        }
        diff > secInMonth -> {
            val days = (diff / secInMonth).toInt()
            context.resources.getQuantityString(R.plurals.ant_months_long, days, days)
        }
        diff > secInWeek -> {
            val days = (diff / secInWeek).toInt()
            context.resources.getQuantityString(R.plurals.ant_weeks_long, days, days)
        }
        diff > secInDay -> {
            val days = (diff / secInDay).toInt()
            context.resources.getQuantityString(R.plurals.ant_days_long, days, days)
        }
        diff > secInHour -> {
            val hours = (diff / secInHour).toInt()
            context.resources.getQuantityString(R.plurals.ant_hours_long, hours, hours)
        }
        diff > secInMin -> {
            val minute = (diff / secInMin).toInt()
            context.resources.getQuantityString(R.plurals.ant_minutes_long, minute, minute)
        }
        else -> {
            return if (prefix == null) {
                context.getString(R.string.ant_started_now).lowercase(Locale.getDefault())
            } else "$prefix${
                context.getString(R.string.ant_started_now)
                    .lowercase(Locale.getDefault())
            }"
        }
    }
    val resultString = if (prefix == null) {
        timeAgo
    } else "$prefix$timeAgo"
    return context.getString(R.string.ant_started_ago, resultString)
}

internal fun View.visible(visible: Boolean) {
    this.visibility = if (visible) VISIBLE else View.INVISIBLE
}

internal fun View.gone(gone: Boolean) {
    this.visibility = if (gone) View.GONE else VISIBLE
}

internal fun String.parseToDate(): Date? {
    return convertUtcToLocal(this)
}

internal fun Fragment.orientation() = resources.configuration.orientation

internal fun Long.getUtcTime(): String {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ", Locale.ENGLISH).format(this)
}

internal inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

internal fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
internal fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

internal fun TextView.hideBadge(runnable: Runnable) {
    this.animate()
        .scaleY(0.0f)
        .scaleX(0.0f)
        .alpha(0.0f)
        .setDuration(300)
        .withEndAction {
            this.text = ""
        }
        .start()
    this.post(runnable)
}

internal fun TextView.showBadge(runnable: Runnable) {
    this.scaleX = 0.5f
    this.scaleY = 0.5f
    this.alpha = 0.0f
    this.animate()
        .scaleY(1.0f)
        .scaleX(1.0f)
        .alpha(1.0f)
        .setDuration(300)
        .start()
    this.post(runnable)
}
