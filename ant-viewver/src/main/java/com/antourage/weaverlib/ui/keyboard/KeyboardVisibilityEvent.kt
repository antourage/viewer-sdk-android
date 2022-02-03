package com.antourage.weaverlib.ui.keyboard

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver

import android.view.WindowManager

/**
 * Created by yshrsmz on 15/03/17.
 */
object KeyboardVisibilityEvent {

    private const val KEYBOARD_MIN_HEIGHT_RATIO = 0.15

    /**
     * Variable for storing keyboard height (so it is known even when keyborad is not shown anymore)
     */
    var keyboardHeight = 0
        private set

    /**
     * Variable for storing heights of system views like statusbar, navbar, etc
     */
    private var latestSystemViewsHeight = 0

    /**
     * Set keyboard visibility change event listener.
     * This automatically remove registered event listener when the Activity is destroyed
     *
     * @param activity Activity
     * @param listener KeyboardVisibilityEventListener
     */
    fun setEventListener(activity: Activity,
                         listener: (Boolean, Int) -> Unit) {

        val unregistrar = registerEventListener(activity, listener)
        activity.application
            .registerActivityLifecycleCallbacks(object : AutoActivityLifecycleCallback(activity) {
                override fun onTargetActivityDestroyed() {
                    unregistrar.unregister()
                }

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                }
            })
    }

    /**
     * Set keyboard visibility change event listener.
     *
     * @param activity Activity
     * @param listener KeyboardVisibilityEventListener
     * @return Unregistrar
     */
    fun registerEventListener(activity: Activity?,
                              listener: (Boolean, Int) -> Unit): Unregistrar {

        if (activity == null) {
            throw NullPointerException("Parameter:activity must not be null")
        }

        val softInputAdjust = activity.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST

        // fix for #37 and #38.
        // The window will not be resized in case of SOFT_INPUT_ADJUST_NOTHING
        require(softInputAdjust and WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING != WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING) { "Parameter:activity window SoftInputMethod is SOFT_INPUT_ADJUST_NOTHING. In this case window will not be resized" }

        if (listener == null) {
            throw NullPointerException("Parameter:listener must not be null")
        }

        val activityRoot = getActivityRoot(activity)

        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {

            private val r = Rect()

            private var wasOpened = false

            override fun onGlobalLayout() {
                activityRoot.getWindowVisibleDisplayFrame(r)

                val screenHeight = activityRoot.rootView.height
                val heightDiff = screenHeight - r.height()

                val isOpen = heightDiff > screenHeight * KEYBOARD_MIN_HEIGHT_RATIO

                if (isOpen) {
                    //save keyboard height, so it is known even when keyboard is hidden
                    keyboardHeight = heightDiff - latestSystemViewsHeight
                } else {
                    //save height of all other than app's views (statusbar, navbar, etc)
                    latestSystemViewsHeight = heightDiff
                }

                if (isOpen == wasOpened) {
                    // keyboard state has not changed
                    return
                }


                wasOpened = isOpen

                listener(isOpen, keyboardHeight)
            }
        }
        activityRoot.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        return SimpleUnregistrar(activity, layoutListener)
    }

    /**
     * Determine if keyboard is visible
     *
     * @param activity Activity
     * @return Whether keyboard is visible or not
     */
    fun isKeyboardVisible(activity: Activity): Boolean {
        val r = Rect()

        val activityRoot = getActivityRoot(activity)

        activityRoot.getWindowVisibleDisplayFrame(r)

        val screenHeight = activityRoot.rootView.height
        val heightDiff = screenHeight - r.height()

        return heightDiff > screenHeight * KEYBOARD_MIN_HEIGHT_RATIO
    }

    internal fun getActivityRoot(activity: Activity): View {
        return (activity.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0)
    }
}
