package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.MotionScene
import androidx.constraintlayout.widget.ConstraintLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.screens.base.AntourageActivity
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*


class AntourageFab @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler {

    val transitionListener = object :MotionLayout.TransitionListener{
        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

        }

        override fun allowsTransition(p0: MotionScene.Transition?): Boolean {
            return true
        }

        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
        }

        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        }

        override fun onTransitionCompleted(p0: MotionLayout?, currentId: Int) {
            if (currentId == R.id.start){
                expandableLayout.visibility = View.INVISIBLE
            }
        }

    }

    init {
        View.inflate(context,R.layout.antourage_fab_layout,this)
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        floatingActionButton.setOnClickListener {
            context.startActivity(intent)
        }
        fabExpantion.setOnClickListener { context.startActivity(intent) }
        AntourageFabLifecycleObserver.registerActionHandler(this)
    }


    override fun onPause() {
//        expandableLayout.visibility = View.INVISIBLE
//        expandableLayout.setTransitionListener(null)
    }

    override fun onResume() {
        Handler(Looper.getMainLooper()).postDelayed({
            expandableLayout.visibility = View.VISIBLE
            expandableLayout.transitionToEnd()
        }, 5000)
        Handler(Looper.getMainLooper()).postDelayed({
            expandableLayout.transitionToStart()
        }, 10000)
        expandableLayout.setTransitionListener(transitionListener)
    }

    override fun onStart() {
        AppExecutors()
    }

    override fun onStop() {

    }

}