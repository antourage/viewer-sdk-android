package com.antourage.weaverlib.other

import android.os.SystemClock
import android.view.View

internal abstract class OnSingleClickListener : View.OnClickListener {

    private var mLastClickTime: Long = 0

    abstract fun onSingleClick(v: View)

    override fun onClick(v: View) {
        val currentClickTime = SystemClock.uptimeMillis()
        val elapsedTime = currentClickTime - mLastClickTime

        if (elapsedTime <= MIN_CLICK_INTERVAL) {
            return
        }
        mLastClickTime = currentClickTime

        onSingleClick(v)
    }

    companion object {
        /**
         * class created to prevent bugs connected with very
         * quick taps on item
         */
        private val MIN_CLICK_INTERVAL: Long = 1000
    }
}