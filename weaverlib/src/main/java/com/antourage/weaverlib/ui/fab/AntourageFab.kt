package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.support.annotation.Keep
import android.support.constraint.ConstraintLayout
import android.support.constraint.motion.MotionLayout
import android.support.constraint.motion.MotionScene
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*

@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler, ReceivingVideosManager.ReceivingVideoCallback {

    companion object {
        const val SHOWING_DURABILITY =5000L
    }

    init {
        AppExecutors()
//        floatingActionButton.size = FloatingActionButton.SIZE_NORMAL
//        floatingActionButton.backgroundTintList = ColorStateList.valueOf(
//            ContextCompat.getColor(
//                context,
//                R.color.bg_color_widget
//            )
//        )

    }

    private val receivingVideoManager = ReceivingVideosManager(this)
    private val listOfSeenStreams = mutableListOf<Int>()

    sealed class WidgetStatus {
        class INACTIVE : WidgetStatus()
        class ACTIVE_LIVE_STREAM(val list: List<StreamResponse>) : WidgetStatus()
        class ACTIVE_UNSEEN_VIDEOS(val numberOfVideos: Int) : WidgetStatus()
    }

    fun changeBadgeStatus(status: WidgetStatus) {
        when (status) {
            is WidgetStatus.INACTIVE -> {
                floatingActionButton.setTextToBadge("")
            }
            is WidgetStatus.ACTIVE_LIVE_STREAM -> {
                for (i in 0 until status.list.size) {
                    if (!listOfSeenStreams.contains(status.list[i].streamId)) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            expandableLayout.visibility = View.VISIBLE
                            expandableLayout.transitionToEnd()
                            tvStreamTitle.text = status.list[i].streamTitle
                            Handler(Looper.getMainLooper()).postDelayed({
                                expandableLayout.transitionToStart()
                            }, SHOWING_DURABILITY)
                            listOfSeenStreams.add(status.list[i].streamId)
                        }, i.toLong() * 2*SHOWING_DURABILITY)
                    }
                }
                floatingActionButton.setTextToBadge(context.getString(R.string.live))
            }
            is WidgetStatus.ACTIVE_UNSEEN_VIDEOS -> {
                floatingActionButton.setTextToBadge(status.numberOfVideos.toString())
            }
        }
    }

    private val transitionListener = object : MotionLayout.TransitionListener {
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
            if (currentId == R.id.start) {
                expandableLayout.visibility = View.INVISIBLE
            }
        }
    }

    init {
        View.inflate(context, R.layout.antourage_fab_layout, this)
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        floatingActionButton.setOnClickListener {
            context.startActivity(intent)
        }
        floatingActionButton.scaleType = ImageView.ScaleType.CENTER
        //fabExpantion.setOnClickListener { context.startActivity(intent) }
        fabExpantion.setOnTouchListener (object:OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                return false
            }

        })
        manageVideos()
        AntourageFabLifecycleObserver.registerActionHandler(this)
    }


    override fun onPause() {
        expandableLayout.visibility = View.INVISIBLE
        expandableLayout.setTransitionListener(null)
        receivingVideoManager.stopReceivingVideos()
    }

    override fun onResume() {
        expandableLayout.setTransitionListener(transitionListener)
        receivingVideoManager.startReceivingVideos()
    }

    fun manageVideos() {
        val seenVideos = UserCache.newInstance().getSeenVideos(context)
        val nonSeenNumber = Repository().getListOfVideos().size - seenVideos.size
        if (nonSeenNumber > 0) {
            changeBadgeStatus(WidgetStatus.ACTIVE_UNSEEN_VIDEOS(nonSeenNumber))
        } else
            changeBadgeStatus(WidgetStatus.INACTIVE())
    }

    override fun onStart() {
        AppExecutors()
    }

    override fun onStop() {

    }

    override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
        when (resource.state) {
            State.LOADING -> {
            }
            State.SUCCESS -> {
                val list = (resource.data)?.toMutableList()
                if (list != null && list.size > 0) {
                    changeBadgeStatus(WidgetStatus.ACTIVE_LIVE_STREAM(list))
                } else {
                    manageVideos()
                }
            }
            State.FAILURE -> {
                changeBadgeStatus(WidgetStatus.INACTIVE())
                BaseViewModel.error.postValue(resource.message)
            }
        }
    }

    override fun onVODReceived() {
    }
}