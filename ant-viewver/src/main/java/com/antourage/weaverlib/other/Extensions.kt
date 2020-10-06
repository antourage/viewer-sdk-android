package com.antourage.weaverlib.other

import android.content.Context
import android.graphics.Bitmap
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.Constants.secInDay
import com.antourage.weaverlib.other.Constants.secInHour
import com.antourage.weaverlib.other.Constants.secInMin
import com.antourage.weaverlib.other.Constants.secInMonth
import com.antourage.weaverlib.other.Constants.secInWeek
import com.antourage.weaverlib.other.Constants.secInYear
import com.antourage.weaverlib.other.Constants.suffixes
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.list.rv.VideoPlayerRecyclerView
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.MAX_HORIZONTAL_MARGIN
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.MAX_VERTICAL_MARGIN
import com.google.android.exoplayer2.C
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
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
    when {
        diff > secInHour -> {
            val hours = (diff / secInHour).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_hours_long, hours, hours)
        }
        diff > secInMin -> {
            val minute = (diff / secInMin).toInt()
            timeAgo =
                context.resources.getQuantityString(R.plurals.ant_minutes_long, minute, minute)
        }

        diff > 10 -> { return context.getString(R.string.ant_recent) }

        else -> { return context.getString(R.string.ant_started_now) }
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

internal fun Date.parseToDisplayAgoTimeLong(context: Context): String {
    val diff = getSecondsDateDiff(this, Date())
    val timeAgo: String
    when {
        diff > secInYear -> {
            val days = (diff / secInYear).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_years_long, days, days)
        }
        diff > secInMonth -> {
            val days = (diff / secInMonth).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_months_long, days, days)
        }
        diff > secInWeek -> {
            val days = (diff / secInWeek).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_weeks_long, days, days)
        }
        diff > secInDay -> {
            val days = (diff / secInDay).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_days_long, days, days)
        }
        diff > secInHour -> {
            val hours = (diff / secInHour).toInt()
            timeAgo = context.resources.getQuantityString(R.plurals.ant_hours_long, hours, hours)
        }
        diff > secInMin -> {
            val minute = (diff / secInMin).toInt()
            timeAgo =
                context.resources.getQuantityString(R.plurals.ant_minutes_long, minute, minute)
        }
        else -> {
            return context.getString(R.string.ant_started_now)
        }
    }
    return context.getString(R.string.ant_started_ago, timeAgo)
}

internal fun <T : View> T.trueWidth(function: (Int) -> Unit) {
    if (width == 0)
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                function(width)
            }
        })
    else function(width)
}

internal fun <T : View> T.setMargins(left: Int, top: Int, right: Int, bottom: Int) {
    if (this.layoutParams is ViewGroup.MarginLayoutParams) {
        val p = this.layoutParams as ViewGroup.MarginLayoutParams
        p.setMargins(left, top, right, bottom)
        this.requestLayout()
    }
}

internal fun View.visible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

internal fun View.gone(gone: Boolean) {
    this.visibility = if (gone) View.GONE else View.VISIBLE
}

internal fun <T> LiveData<Resource<T>>.observeOnce(observer: Observer<Resource<T>>) {
    observeForever(object : Observer<Resource<T>> {
        override fun onChanged(resource: Resource<T>?) {
            observer.onChanged(resource)
            when (resource?.status) {
                is Status.Success, is Status.Failure -> removeObserver(this)
            }
        }
    })
}

internal fun String.parseToDate(): Date? {
    return convertUtcToLocal(this)
}

internal fun Fragment.orientation() = resources.configuration.orientation

internal fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

internal fun String.isEmptyTrimmed(): Boolean = this.trim().isEmpty()
internal fun CharSequence.isEmptyTrimmed(): Boolean = this.trim().isEmpty()

internal fun Long.formatDuration(): String {
    val outputFmt = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    outputFmt.timeZone = TimeZone.getTimeZone("UTC")
    return outputFmt.format(this)
}

internal fun Long.millisToTime(): String {

    val h = (this / 1000 / 3600)
    val m = (this / 1000 / 60 % 60)
    val s = (this / 1000 % 60)

    val formattedH = if (h > 0) "$h:" else ""
    val formattedM = if (m in 0..9 && h != 0L) "0$m" else m.toString()
    val formattedS = if (s in 0..9) "0$s" else s.toString()

    return "$formattedH$formattedM:$formattedS"
}


fun Long.formatTimeMillisToTimer(): String {
    var time = this
    if (this == C.TIME_UNSET) {
        time = 0
    }
    val formatter = Formatter(StringBuilder(), Locale.getDefault())
    val totalSeconds: Long = (time + 500) / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) formatter.format("%d:%02d:%02d", hours, minutes, seconds)
        .toString() else formatter.format("%d:%02d", minutes, seconds).toString()
}

internal fun String.parseToMills(): Long {
    val inputFmt = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    inputFmt.timeZone = TimeZone.getTimeZone("UTC")
    return inputFmt.parse(this).time
}

internal fun String.parseTimerToMills(): Long {
    val stringToParse = if (this.length > 8) this.substring(0, 8) else this
    val inputFmt = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    inputFmt.timeZone = TimeZone.getTimeZone("UTC")
    return inputFmt.parse(stringToParse)?.time ?: 0L
}

internal fun Long.getUtcTime(): String? {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH).format(this)
}

internal fun Bitmap.toMultipart(): MultipartBody.Part {
    val bos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 75, bos)
    val imageByteArray = bos.toByteArray()

    val imageRequestBody =
        imageByteArray.toRequestBody(
            "multipart/form-data".toMediaTypeOrNull(),
            0,
            imageByteArray.size
        )
    return MultipartBody.Part.createFormData("file", "file.png", imageRequestBody)
}

internal fun View.marginDp(
    left: Float? = 0f,
    top: Float? = 0f,
    right: Float? = 0f,
    bottom: Float? = 0f
) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.run { leftMargin = dpToPx(this) }
        top?.run { topMargin = dpToPx(this) }
        right?.run { rightMargin = dpToPx(this) }
        bottom?.run { bottomMargin = dpToPx(this) }
    }
}

internal inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}

internal fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
internal fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

internal fun TextView.hideBadge() {
    this.animate()
        .scaleY(0.0f)
        .scaleX(0.0f)
        .alpha(0.0f)
        .setDuration(300)
        .withEndAction { this.text = "" }
        .start()
}

internal fun TextView.showBadge() {
    this.scaleX = 0.5f
    this.scaleY = 0.5f
    this.alpha = 0.0f
    this.animate()
        .scaleY(1.0f)
        .scaleX(1.0f)
        .alpha(1.0f)
        .setDuration(300)
        .start();
}

internal fun RecyclerView.betterSmoothScrollToPosition(targetItem: Int) {
    layoutManager?.apply {
        val maxScroll = 5
        when (this) {
            is LinearLayoutManager -> {
                val topItem = findFirstVisibleItemPosition()
                val distance = topItem - targetItem
                val anchorItem = when {
                    distance > maxScroll -> targetItem + maxScroll
                    distance < -maxScroll -> targetItem - maxScroll
                    else -> topItem
                }
                if (anchorItem != topItem) scrollToPosition(anchorItem)
                post {
                    smoothScrollToPosition(targetItem)
                }
            }
            else -> smoothScrollToPosition(targetItem)
        }
    }
}

fun View.animateShowHideDown(isShow: Boolean) {
    if (isShow && visibility != View.VISIBLE) {
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_up_fade_in))
        visibility = View.VISIBLE
    } else if (!isShow && visibility == View.VISIBLE) {
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_down_fade_out))
        visibility = View.INVISIBLE
    }
}

internal fun View.revealWithAnimation() {
    alpha = 0f
    visibility = View.VISIBLE
    animate().alpha(1f).setDuration(300).start()
}

internal fun View.hideWithAnimation() {
    animate().alpha(0f).setDuration(300).withEndAction { this.visibility = View.INVISIBLE }.start()
}

internal inline fun VideoPlayerRecyclerView.afterMeasured(crossinline f: VideoPlayerRecyclerView.() -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (measuredWidth > 0 && measuredHeight > 0) {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                f()
            }
        }
    })
}

internal fun View.removeConstraints(parent: ConstraintLayout) {
    val set = ConstraintSet()
    set.clone(parent)
    set.clear(this.id, ConstraintSet.TOP)
    set.clear(this.id, ConstraintSet.RIGHT)
    set.clear(this.id, ConstraintSet.BOTTOM)
    set.clear(this.id, ConstraintSet.LEFT)
    set.applyTo(parent)
}

internal fun Long.formatQuantity(): String {
    if (this < 1000) return this.toString() //deal with easy case

    val e = suffixes.floorEntry(this)!!
    val divideBy: Long = e.key
    val suffix: String = e.value

    val truncated: Long = this / (divideBy / 10) //the number part of the output times 10

    val hasDecimal = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal) {
        (truncated / 10.0).toString().replace(".", ",") + suffix
    } else {
        (truncated / 10).toString() + suffix
    }
}

/** checks if values are in range and converts it to pixels*/
internal fun Int.validateHorizontalMarginForFab(context: Context): Int {
    return when {
        this<0 -> 0
        this>MAX_HORIZONTAL_MARGIN -> dp2px(context, MAX_HORIZONTAL_MARGIN.toFloat()).toInt()
        else -> dp2px(context, this.toFloat()).toInt()
    }
}

/** checks if values are in range and converts it to pixels*/
internal fun Int.validateVerticalMarginForFab(context: Context): Int {
    return when {
        this<0 -> 0
        this>MAX_VERTICAL_MARGIN -> dp2px(context, MAX_VERTICAL_MARGIN.toFloat()).toInt()
        else -> dp2px(context, this.toFloat()).toInt()
    }
}