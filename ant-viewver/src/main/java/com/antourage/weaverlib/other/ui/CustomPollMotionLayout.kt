package com.antourage.weaverlib.other.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.constraintlayout.widget.ConstraintSet
import com.antourage.weaverlib.R

/**
 * Based on the official article:
 * https://medium.com/androiddevelopers/working-with-dynamic-data-in-motionlayout-9dbbcfe5ff75
 *
 * Used to manage 2 types of animation, as it was too difficult in xml:
 *  - From unanswered to answered by User;
 *  - From answered by user to answered by another user;
 */

class CustomPollMotionLayout : MotionLayout {

    // The main transition of this widget. We'll rely all animation on this transition.
    private var mTransition: MotionScene.Transition? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    /**
     * Initialization of motion layout
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        mTransition = getTransition(R.id.transition)

        val startId = mTransition!!.startConstraintSetId
        val startSet = getConstraintSet(startId)
        startSet.clone(this)

        val endId = mTransition!!.endConstraintSetId
        val endSet = getConstraintSet(endId)
        endSet.clone(this)

        setTransition(mTransition!!)
        rebuildScene()
    }

    fun setConstraintFromAnsweredToNewAnswer(startPercentage: Float,
                                             endPercentage: Float) {
        val startSet: ConstraintSet = getConstraintSet(mTransition!!.startConstraintSetId)
        startSet.setGuidelinePercent(R.id.item_poll_guideline, startPercentage)

        val endSet: ConstraintSet = getConstraintSet(mTransition!!.endConstraintSetId)
        endSet.setGuidelinePercent(R.id.item_poll_guideline, endPercentage)
    }

    fun setConstraintToJustAnswered(newPercentage: Float) {
        val endSet: ConstraintSet = getConstraintSet(mTransition!!.endConstraintSetId)
        endSet.setGuidelinePercent(R.id.item_poll_guideline, newPercentage)
        endSet.connect(R.id.item_poll_answer, ConstraintSet.END,
            R.id.item_poll_percentage, ConstraintSet.START
        )
        endSet.setHorizontalBias(R.id.item_poll_answer, 0.0f)
        endSet.setVisibility(R.id.item_poll_percentage, View.VISIBLE)
    }

    /**
     * Animate the widget from start to the end.
     * It'll animate based on the [.setConstraint..] functions
     */
    fun animateWidget() {
        val startSet: ConstraintSet = getConstraintSet(mTransition!!.startConstraintSetId)
        val endSet: ConstraintSet = getConstraintSet(mTransition!!.endConstraintSetId)

        setTransition(mTransition!!.startConstraintSetId, mTransition!!.endConstraintSetId)
        setTransitionListener(object: TransitionListener{
            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}

            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                startSet.clone(endSet)
            }
        })
        transitionToEnd()
    }

    fun setAnsweredStateWithoutAnimation(newPercentage: Float){
        val startSet: ConstraintSet = getConstraintSet(mTransition!!.startConstraintSetId)
        startSet.setGuidelinePercent(R.id.item_poll_guideline, newPercentage)
        startSet.connect(R.id.item_poll_answer, ConstraintSet.END,
            R.id.item_poll_percentage, ConstraintSet.START
        )
        startSet.setHorizontalBias(R.id.item_poll_answer, 0.0f)
        startSet.setVisibility(R.id.item_poll_percentage, View.VISIBLE)

        startSet.applyTo(this)
    }
}