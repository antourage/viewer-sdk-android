package com.antourage.weaverlib.ui.fab

import android.annotation.SuppressLint
import android.content.Context
import android.support.constraint.motion.MotionLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import com.antourage.weaverlib.R
import kotlinx.android.synthetic.main.layout_motion_fab.view.*

class MotionOverlayView @JvmOverloads constructor(context: Context,
                                                  attrs: AttributeSet? = null,
                                                  defStyleAttr: Int = 0) : MotionLayout(context, attrs, defStyleAttr) {
    private var motionLayout: MotionLayout
    private val touchableArea: View

    private val clickableArea: View

    private var startX: Float? = null
    private var startY: Float? = null
    private var listener:FabExpantionListener? =  null

    fun setFabListener(fablistener:FabExpantionListener){
        this.listener = fablistener
    }

    init {
        motionLayout = LayoutInflater.from(context).inflate(R.layout.layout_motion_fab, this, false) as MotionLayout
        addView(motionLayout)

        touchableArea = motionLayout.findViewById(R.id.fabExpantion)
        clickableArea = motionLayout.findViewById(R.id.fabExpantion)
    }


    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val isInProgress = (motionLayout.progress > 0.0f && motionLayout.progress < 1.0f)
        val isInTarget = touchEventInsideTargetView(touchableArea, ev)

        return if (isInProgress || isInTarget) {
            super.onInterceptTouchEvent(ev)
        } else {
            true
        }
    }

    private fun touchEventInsideTargetView(v: View, ev: MotionEvent): Boolean {
        if (ev.x > v.left && ev.x < v.right) {
            if (ev.y > v.top && ev.y < v.bottom) {
                return true
            }
        }
        return false
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (touchEventInsideTargetView(clickableArea, ev)) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x
                    startY = ev.y
                    listener?.onSwipeStarted()
                }

                MotionEvent.ACTION_UP   -> {
                    val endX = ev.x
                    val endY = ev.y
                    if(startX != null && startY != null)
                    if (isAClick(startX!!, endX, startY!!, endY)) {
                        expandableLayout.transitionToStart()
                        listener?.onFabExpantionClicked()
                    } else{
                        listener?.onSwipeStarted()
                    }
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun isAClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
        val differenceX = Math.abs(startX - endX)
        val differenceY = Math.abs(startY - endY)
        return !/* =5 */(differenceX > 200 || differenceY > 200)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false
    }
    interface FabExpantionListener{
        fun onFabExpantionClicked()

        fun onSwipeStarted()
    }
}
