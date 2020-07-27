package com.antourage.weaverlib.ui.keyboard

import android.app.Activity
import android.os.Build
import android.view.ViewTreeObserver
import java.lang.ref.WeakReference

/**
 * @author Anoop S S
 * anoopvvs@gmail.com
 * on 28/02/2017
 */

class SimpleUnregistrar internal constructor(activity: Activity, globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener) :
    Unregistrar {

    private val mActivityWeakReference: WeakReference<Activity>

    private val mOnGlobalLayoutListenerWeakReference: WeakReference<ViewTreeObserver.OnGlobalLayoutListener>

    init {
        mActivityWeakReference = WeakReference(activity)
        mOnGlobalLayoutListenerWeakReference = WeakReference<ViewTreeObserver.OnGlobalLayoutListener>(globalLayoutListener)
    }

    override fun unregister() {
        val activity = mActivityWeakReference.get()
        val globalLayoutListener = mOnGlobalLayoutListenerWeakReference.get()

        if (null != activity && null != globalLayoutListener) {
            val activityRoot = KeyboardVisibilityEvent.getActivityRoot(activity)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                activityRoot.viewTreeObserver
                        .removeOnGlobalLayoutListener(globalLayoutListener)
            } else {
                activityRoot.viewTreeObserver
                        .removeGlobalOnLayoutListener(globalLayoutListener)
            }
        }

        mActivityWeakReference.clear()
        mOnGlobalLayoutListenerWeakReference.clear()
    }

}