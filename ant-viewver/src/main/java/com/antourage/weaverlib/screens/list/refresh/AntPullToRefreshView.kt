package com.antourage.weaverlib.screens.list.refresh

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.Transformation
import androidx.core.view.MotionEventCompat
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingParent
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.antourage.weaverlib.other.dp2px
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class AntPullToRefreshView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs), NestedScrollingParent, NestedScrollingChild {
    private var mTarget: View? = null
    private var mAntBaseProgressBar: AntBaseProgressBar = AntRefreshView(context)
    private val mDecelerateInterpolator: Interpolator
    private val mTouchSlop: Int
    var totalDragDistance: Int = 0

    private var mCurrentDragPercent = 0f
    private var mCurrentOffsetTop = 0
    var mIsRefreshing: Boolean = false
    private var mActivePointerId = 0
    private var mIsBeingDragged = false
    private var mInitialMotionY = 0f
    private var mFrom = 0
    private var mFromDragPercent = 0f
    private var mNotify = false
    private var mListener: OnRefreshListener? =
        null
    private var mTargetPaddingTop = 0
    private var mTargetPaddingBottom = 0
    private var mTargetPaddingRight = 0
    private var mTargetPaddingLeft = 0

    private var mCustomAnimatedView: View? = null
    private var customAnimatedViewHeight: Int = 0
    private var customAnimatedViewWidth: Int = 0


    private var mCustomView: View? = null
    private var customViewHeight: Int = 0
    private var customViewWidth: Int = 0

    companion object {
        private var DRAG_MAX_DISTANCE = 100f
        private const val DRAG_RATE = .5f
        private const val DECELERATE_INTERPOLATION_FACTOR = 2f
        const val MAX_OFFSET_ANIMATION_DURATION = 500
        private const val INVALID_POINTER = -1
    }

    init {
        mDecelerateInterpolator =
            DecelerateInterpolator(DECELERATE_INTERPOLATION_FACTOR)
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        totalDragDistance = dp2px(context, DRAG_MAX_DISTANCE).toInt()
        initBar()
        setRefreshing(false)
        setWillNotDraw(false)
        ViewCompat.setChildrenDrawingOrderEnabled(this, true)
    }


    fun setCustomView(view: View, height: Int, width: Int) {
        mCustomView = view
        mCustomView?.layoutParams = LayoutParams(height, width)
        customViewHeight = height
        customViewWidth = width
        addView(mCustomView, 0)
    }

    fun setCustomAnimationView(view: View) {
        mCustomAnimatedView = view
        mCustomAnimatedView?.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        customAnimatedViewHeight = dp2px(context, 120f).toInt()
        customAnimatedViewWidth = dp2px(context, 320f).toInt()
        addView(mCustomAnimatedView, 0)
    }

    private fun initBar() {
        setRefreshing(false)
        mAntBaseProgressBar = AntRefreshView(context)
        mAntBaseProgressBar.setParent(this)
        addView(mAntBaseProgressBar)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mWidthMeasureSpec = widthMeasureSpec
        var mHeightMeasureSpec = heightMeasureSpec
        super.onMeasure(mWidthMeasureSpec, mHeightMeasureSpec)
        ensureTarget()
        if (mTarget == null) return
        mWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredWidth - paddingRight - paddingLeft,
            MeasureSpec.EXACTLY
        )
        mHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
            measuredHeight - paddingTop - paddingBottom,
            MeasureSpec.EXACTLY
        )
        mTarget!!.measure(mWidthMeasureSpec, mHeightMeasureSpec)
        mAntBaseProgressBar.measure(mWidthMeasureSpec, mHeightMeasureSpec)
        mCustomView?.measure(width, height)
        mCustomAnimatedView?.measure(widthMeasureSpec, mHeightMeasureSpec)
    }

    private fun ensureTarget() {
        if (mTarget != null) return
        if (childCount > 0) {
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child !== mAntBaseProgressBar) {
                    mTarget = child
                    mTargetPaddingBottom = mTarget!!.paddingBottom
                    mTargetPaddingLeft = mTarget!!.paddingLeft
                    mTargetPaddingRight = mTarget!!.paddingRight
                    mTargetPaddingTop = mTarget!!.paddingTop
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
//        ensureTarget()
        if (!isEnabled || canChildScrollUp() || mIsRefreshing) {
            return false
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                setTargetOffsetTop(0)
                mActivePointerId = ev.getPointerId(0)
                mIsBeingDragged = false
                val initialMotionY = getMotionEventY(ev, mActivePointerId)
                if (initialMotionY == -1f) {
                    return false
                }
                mInitialMotionY = initialMotionY
            }
            MotionEvent.ACTION_MOVE -> {
                if (mActivePointerId == ViewDragHelper.INVALID_POINTER) {
                    return false
                }

                val y = getMotionEventY(ev, mActivePointerId)
                if (y == -1f) {
                    return false
                }

                val yDiff = y - mInitialMotionY;
                if (yDiff > mTouchSlop && !mIsBeingDragged) {
                    mIsBeingDragged = true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                mIsBeingDragged = false
                mActivePointerId = ViewDragHelper.INVALID_POINTER
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
            }
        }
        return mIsBeingDragged
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!mIsBeingDragged) {
            return super.onTouchEvent(ev)
        }
//        if (!isEnabled || canChildScrollUp() || mIsRefreshing || mNestedScrollInProgress) {
//            return false
//        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                if (pointerIndex < 0) {
                    return false
                }
                val y = MotionEventCompat.getY(ev, pointerIndex)
                val yDiff = y - mInitialMotionY
                val scrollTop = yDiff * DRAG_RATE
                mCurrentDragPercent = scrollTop / totalDragDistance
                if (mCurrentDragPercent < 0) {
                    return false
                }
                val boundedDragPercent = min(1f, abs(mCurrentDragPercent))
                val extraOS = abs(scrollTop) - totalDragDistance
                val slingshotDist = totalDragDistance.toFloat()
                val tensionSlingshotPercent =
                    max(0f, min(extraOS, slingshotDist * 2) / slingshotDist)
                val tensionPercent =
                    ((tensionSlingshotPercent / 4) - (tensionSlingshotPercent / 4).pow(2)) * 2f
                val extraMove = slingshotDist * tensionPercent / 2
                val targetY = (slingshotDist * boundedDragPercent + extraMove).toInt()

                val offsetScrollTop = scrollTop - (totalDragDistance / 2)
                if (offsetScrollTop > 0) {
                    mAntBaseProgressBar.setPercent(200 * offsetScrollTop / totalDragDistance)
                    mCurrentDragPercent = offsetScrollTop / totalDragDistance * 2
                }

                setTargetOffsetTop((targetY - mCurrentOffsetTop))
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val index = ev.actionIndex
                mActivePointerId = ev.getPointerId(index)
            }
            MotionEvent.ACTION_POINTER_UP -> {
                onSecondaryPointerUp(ev)
            }
            MotionEvent.ACTION_UP -> {
                if (mActivePointerId == INVALID_POINTER) {
                    return false
                }
                val pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
                val y = MotionEventCompat.getY(ev, pointerIndex)
                val overScrollTop =
                    (y - mInitialMotionY) * DRAG_RATE
                mIsBeingDragged = false
                if (overScrollTop > totalDragDistance) {
                    setRefreshing(true, true)
                } else {
                    mIsRefreshing = false
                    animateOffsetToStartPosition()
                }
                mActivePointerId = INVALID_POINTER
                return false
            }
        }
        return true
    }

    private fun animateOffsetToStartPosition() {
        mFrom = mCurrentOffsetTop
        mFromDragPercent = mCurrentDragPercent
        val animationDuration = abs((MAX_OFFSET_ANIMATION_DURATION * mFromDragPercent).toLong())

        mAnimateToStartPosition.reset()
//        mAnimateToStartPosition.duration = MAX_OFFSET_ANIMATION_DURATION.toLong()
        mAnimateToStartPosition.duration = animationDuration.toLong()
        mAnimateToStartPosition.interpolator = mDecelerateInterpolator
        mAnimateToStartPosition.setAnimationListener(mToStartListener)
        mAntBaseProgressBar.clearAnimation()
        mAntBaseProgressBar.startAnimation(mAnimateToStartPosition)
    }

    private fun animateOffsetToCorrectPosition() {
        mFrom = mCurrentOffsetTop
        mFromDragPercent = mCurrentDragPercent

        mAnimateToCorrectPosition.reset()
        mAnimateToCorrectPosition.duration = MAX_OFFSET_ANIMATION_DURATION.toLong()
        mAnimateToCorrectPosition.interpolator = mDecelerateInterpolator

        mAntBaseProgressBar.clearAnimation()
        mAntBaseProgressBar.startAnimation(mAnimateToCorrectPosition)
        if (mIsRefreshing) {
            mAntBaseProgressBar.start()
            if (mNotify) {
                if (mListener != null) {
                    mListener?.onRefresh()
                }
            }
        } else {
            mAntBaseProgressBar.stop()
            animateOffsetToStartPosition()
        }
        mCurrentOffsetTop = mTarget!!.top
        mTarget!!.setPadding(
            mTargetPaddingLeft,
            mTargetPaddingTop,
            mTargetPaddingRight,
            totalDragDistance
        )
    }

    private val mAnimateToStartPosition: Animation = object : Animation() {
        public override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation
        ) {
            moveToStart(interpolatedTime)
        }
    }
    private val mAnimateToCorrectPosition: Animation = object : Animation() {
        public override fun applyTransformation(
            interpolatedTime: Float,
            t: Transformation
        ) {
            val targetTop: Int
            val endTarget: Int = totalDragDistance
            targetTop = mFrom + ((endTarget - mFrom) * interpolatedTime).toInt()
            val offset = targetTop - mTarget!!.top
            mCurrentDragPercent = mFromDragPercent - (mFromDragPercent - 1.0f) * interpolatedTime
            mAntBaseProgressBar.setPercent(100 * mCurrentDragPercent)
            setTargetOffsetTop(offset)
        }
    }

    private fun moveToStart(interpolatedTime: Float) {
        val targetTop = mFrom - (mFrom * interpolatedTime).toInt()
        val targetPercent = mFromDragPercent * (1.0f - interpolatedTime)
        val offset = targetTop - mTarget!!.top
        mCurrentDragPercent = targetPercent
        mAntBaseProgressBar.setPercent(100 * mCurrentDragPercent)
        mTarget!!.setPadding(
            mTargetPaddingLeft,
            mTargetPaddingTop,
            mTargetPaddingRight,
            mTargetPaddingBottom + targetTop
        )
        setTargetOffsetTop(offset)
    }

    fun setRefreshing(refreshing: Boolean) {
        if (mIsRefreshing != refreshing) {
            setRefreshing(refreshing, false /* notify */)
        }
    }

    private fun setRefreshing(refreshing: Boolean, notify: Boolean) {
        if (mIsRefreshing != refreshing) {
            mNotify = notify
            ensureTarget()
            mIsRefreshing = refreshing
            if (mIsRefreshing) {
//                mAntBaseProgressBar.setPercent(1f)
                animateOffsetToCorrectPosition()
            } else {
                animateOffsetToStartPosition()
            }
        }
    }

    private val mToStartListener: Animation.AnimationListener =
        object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                mAntBaseProgressBar.stop()
                mCurrentOffsetTop = mTarget!!.top
            }
        }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex = MotionEventCompat.getActionIndex(ev)
        val pointerId = MotionEventCompat.getPointerId(ev, pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
        }
    }

    private fun getMotionEventY(ev: MotionEvent, activePointerId: Int): Float {
        val index = MotionEventCompat.findPointerIndex(ev, activePointerId)
        return if (index < 0) {
            (-1).toFloat()
        } else MotionEventCompat.getY(ev, index)
    }

    private fun setTargetOffsetTop(offset: Int) {
        mTarget?.offsetTopAndBottom(offset)
        mCurrentOffsetTop = mTarget!!.top
    }

    private fun canChildScrollUp(): Boolean {
        return ViewCompat.canScrollVertically(mTarget, -1)
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int
    ) {
        ensureTarget()
        if (mTarget == null) return
        val height = measuredHeight
        val width = measuredWidth
        val left = paddingLeft
        val top = paddingTop
        val right = paddingRight
        val bottom = paddingBottom
        mTarget!!.layout(
            left,
            top + mCurrentOffsetTop,
            left + width - right,
            top + height - bottom + mCurrentOffsetTop
        )
        mAntBaseProgressBar.layout(left, top, left + width - right, top + height - bottom)
        mCustomView?.layout(
            left,
            (top + dp2px(context, 0f)).toInt(), left + width - right,
            (customViewHeight + dp2px(context, 0f)).toInt()
        )
        mCustomAnimatedView?.layout(
            left,
            (top + dp2px(context, 0f)).toInt(), left + width - right,
            (customAnimatedViewHeight + dp2px(context, 0f)).toInt()
        )


    }

    fun setOnRefreshListener(listener: OnRefreshListener?) {
        mListener = listener
    }

    interface OnRefreshListener {
        fun onRefresh()
    }

    override fun onStartNestedScroll(
        child: View,
        target: View,
        nestedScrollAxes: Int
    ): Boolean {
        return (isEnabled && !mIsRefreshing
                && nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

}