package com.antourage.weaverlib.other

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import com.antourage.weaverlib.R
import com.antourage.weaverlib.di.ApplicationComponent
import com.antourage.weaverlib.di.DaggerApplicationComponent
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

internal inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> FragmentTransaction) {
    beginTransaction().func().commit()
}

internal fun AppCompatActivity.replaceFragment(
    fragment: Fragment,
    frameId: Int,
    addToBackStack: Boolean = false
) {
    if (addToBackStack) {
        supportFragmentManager.inTransaction {
            replace(frameId, fragment).addToBackStack(fragment.javaClass.simpleName)
        }
    } else
        supportFragmentManager.inTransaction {
            replace(frameId, fragment)
        }
}

//Did not want to use Application class(problem with merging), decided on extension function
internal fun Application.initDagger(): ApplicationComponent {
    return DaggerApplicationComponent.builder()
        .application(this)
        .build()
}

internal fun Fragment.replaceFragment(fragment: Fragment, frameId: Int, addToBackStack: Boolean = false) {
    (activity as AppCompatActivity).replaceFragment(fragment, frameId, addToBackStack)
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

internal fun <T> LiveData<T>.reObserve(@NonNull owner: LifecycleOwner, @NonNull observer: Observer<T>) {
    this.removeObserver(observer)
    this.observe(owner, observer)
}

internal fun String.parseDate(context: Context): String {
    val secondsInMinute = 60
    val minutesInHour = 60 * secondsInMinute
    val hoursInDay = minutesInHour * 24
    val localUTC = convertUtcToLocal(this)
    if (localUTC != null) {
        var diff = getDateDiff(localUTC, Date())
        when {
            diff > 3 * hoursInDay -> {
                val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                return df.format(localUTC)
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
                if (diff == 0L) {
                    diff = 1
                }
                val amount = context.resources
                    .getQuantityString(R.plurals.ant_seconds, diff.toInt(), diff.toInt())
                return context.getString(R.string.ant_started_ago, amount)
            }
        }
    } else {
        return ""
    }
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

internal fun String.parseToMills(): Long {
    val inputFmt = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
    inputFmt.timeZone = TimeZone.getTimeZone("UTC")
    return inputFmt.parse(this).time
}

internal fun Bitmap.toMultipart(): MultipartBody.Part {
    val bos = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 75, bos)
    val imageByteArray = bos.toByteArray()

    val imageRequestBody =
        RequestBody.create(MediaType.parse("multipart/form-data"), imageByteArray)
    return MultipartBody.Part.createFormData("file", "file.png", imageRequestBody)
}