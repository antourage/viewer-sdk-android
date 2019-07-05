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
import android.view.View
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.generateRandomViewerNumber
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.base.AppExecutors
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import kotlinx.android.synthetic.main.layout_motion_fab.view.*

@Suppress("IMPLICIT_CAST_TO_ANY") //TODO 6/17/2019 handle this
@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler, ReceivingVideosManager.ReceivingVideoCallback,
    MotionOverlayView.FabExpantionListener {

    private lateinit var currentlyDisplayedStream: StreamResponse
    private var listOfStreams: List<StreamResponse>? = null
    val handlerFab: Handler = Handler(Looper.getMainLooper())
    private val setOfDismissed = mutableListOf<Int>()
    var isSwipeInProgress = false
    var counter = 0


    override fun onFabExpantionClicked() {
        val intent = Intent(context, AntourageActivity::class.java)
        intent.putExtra(ARGS_STREAM_SELECTED, currentlyDisplayedStream)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.applicationContext.startActivity(intent)
    }

    override fun onSwipeStarted() {
        isSwipeInProgress = true
    }

    companion object {
        const val SHOWING_DURABILITY = 5000L
        const val ARGS_STREAM_SELECTED = "args_stream_selected"
    }

    init {
        BASE_URL = UserCache.newInstance().getBeChoice(context)!!
        AppExecutors()
        ReceivingVideosManager.newInstance(this)
    }

    sealed class WidgetStatus {
        object Inactive : WidgetStatus()
        class ActiveLiveStream(val list: List<StreamResponse>) : WidgetStatus()
        class ActiveUnseenVideos(val numberOfVideos: Int) : WidgetStatus()
    }

    fun changeBadgeStatus(status: WidgetStatus) {
        when (status) {
            is WidgetStatus.Inactive -> {
                handlerFab.removeCallbacksAndMessages(null)
                listOfStreams = null
                counter = 0
                floatingActionButton.setImageResource(R.drawable.ic_logo_white)
                floatingActionButton.setTextToBadge("")
            }
            is WidgetStatus.ActiveLiveStream -> {
                if(floatingActionButton != null)
                    floatingActionButton.setImageResource(R.drawable.ic_icon_logo)
                if (!handlerFab.hasMessages(0))
                    handlerFab.postDelayed(object : Runnable {
                        override fun run() {
                            listOfStreams?.let { listOfStreams ->
                                if (counter > (listOfStreams.size - 1)) {
                                    counter = 0
                                }
                                if (!setOfDismissed.contains(listOfStreams[counter].streamId)) {
                                    currentlyDisplayedStream = listOfStreams[counter]
                                    expandableLayout.visibility = View.VISIBLE
                                    expandableLayout.transitionToEnd()
                                    tvStreamTitle.text = listOfStreams[counter].streamTitle
                                    tvViewers.text = context.getString(R.string.viewers, generateRandomViewerNumber())
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        expandableLayout.transitionToStart()
                                    }, SHOWING_DURABILITY)
                                    handlerFab.postDelayed(
                                        this,
                                        2 * SHOWING_DURABILITY
                                    )
                                    counter++
                                } else {
                                    counter++
                                    handlerFab.postDelayed(
                                        this,
                                        0
                                    )

                                }
                            }
                        }
                    }, 0)
                floatingActionButton.setTextToBadge(context.getString(R.string.live))
            }
            is WidgetStatus.ActiveUnseenVideos -> {
                floatingActionButton.setImageResource(R.drawable.ic_icon_logo)
                handlerFab.removeCallbacksAndMessages(null)
                listOfStreams = null
                counter = 0
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
                if (isSwipeInProgress) {
                    listOfStreams?.let { listOfStreams ->
                        for (i in 0 until listOfStreams.size) {
                            setOfDismissed.add(listOfStreams[i].streamId)
                        }
                    }
                    handlerFab.removeCallbacksAndMessages(null)
                }
            }
            isSwipeInProgress = false

        }
    }

    init {
        View.inflate(context, R.layout.antourage_fab_layout, this)
        val intent = Intent(context, AntourageActivity::class.java)
        motionOverlayView.setFabListener(this)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        floatingActionButton.setOnClickListener {
            context.startActivity(intent)
        }
        floatingActionButton.scaleType = ImageView.ScaleType.CENTER
        manageVideos()
        AntourageFabLifecycleObserver.registerActionHandler(this)
    }


    override fun onPause() {
        expandableLayout.visibility = View.INVISIBLE
        expandableLayout.setTransitionListener(null)
        ReceivingVideosManager.stopReceivingVideos()

    }

    override fun onResume() {
        expandableLayout.setTransitionListener(transitionListener)
        ReceivingVideosManager.newInstance(this)
        ReceivingVideosManager.startReceivingVideos()

    }

    fun manageVideos() {
        val seenVideos = UserCache.newInstance().getSeenVideos(context)
        val nonSeenNumber = Repository(ApiClient.getInitialClient().webService).getListOfVideos().size - seenVideos.size
        if (nonSeenNumber > 0) {
            changeBadgeStatus(WidgetStatus.ActiveUnseenVideos(nonSeenNumber))
        } else
            changeBadgeStatus(WidgetStatus.Inactive)
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
                    listOfStreams = list
                    changeBadgeStatus(WidgetStatus.ActiveLiveStream(list))
                } else {
                    manageVideos()
                }
            }
            State.FAILURE -> {
                changeBadgeStatus(WidgetStatus.Inactive)
                BaseViewModel.error.postValue(resource.message)
            }
        }
    }

    override fun onVODReceived() {
    }
}