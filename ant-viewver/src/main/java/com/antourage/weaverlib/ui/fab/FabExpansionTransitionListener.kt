package com.antourage.weaverlib.ui.fab

import androidx.constraintlayout.motion.widget.MotionLayout

/**
 * Wrapper interface used to shadow unnecessary implementations of some of the methods
 */

internal interface FabExpansionTransitionListener : MotionLayout.TransitionListener{
    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

    override fun onTransitionCompleted(p0: MotionLayout?, currentId: Int) {}
}