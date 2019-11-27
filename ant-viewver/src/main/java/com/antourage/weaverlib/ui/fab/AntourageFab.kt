package com.antourage.weaverlib.ui.fab

import android.arch.lifecycle.Observer
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
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.OnSingleClickListener
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import kotlinx.android.synthetic.main.layout_motion_fab.view.*

/**
 * When integrating to React Native need to add also constraint layout library in declaration
 * so it would know that it is subclass of View
 */
@Suppress("IMPLICIT_CAST_TO_ANY") //TODO 6/17/2019 handle this
@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler,
    MotionOverlayView.FabExpansionListener {

    companion object {
        internal const val SHOWING_DURABILITY = 5000L
        internal const val ARGS_STREAM_SELECTED = "args_stream_selected"

        @JvmStatic
        fun registerNotifications(
            fcmToken: String,
            callback: ((result: RegisterPushNotificationsResult) -> Unit)? = null
        ) {
            val response =
                Repository().subscribeToPushNotifications(SubscribeToPushesRequest(fcmToken))
            response.observeForever(object : Observer<Resource<NotificationSubscriptionResponse>> {
                override fun onChanged(it: Resource<NotificationSubscriptionResponse>?) {
                    when (val responseStatus = it?.status) {
                        is Status.Success -> {
                            responseStatus.data?.topic?.let { topicName ->
                                RegisterPushNotificationsResult.Success(
                                    topicName
                                )
                            }?.let { result -> callback?.invoke(result) } ?: run {
                                callback?.invoke(
                                    RegisterPushNotificationsResult.Failure(
                                        "empty topic name"
                                    )
                                )
                            }
                            response.removeObserver(this)
                        }
                        is Status.Failure -> {
                            callback?.invoke(RegisterPushNotificationsResult.Failure(responseStatus.errorMessage))
                            response.removeObserver(this)
                        }
                    }
                }
            })
        }

        @JvmStatic
        fun authWithApiKey(
            apiKey: String,
            refUserId: String? = null,
            nickname: String? = null,
            callback: ((result: UserAuthResult) -> Unit)? = null
        ) {
            val userCache = UserCache.getInstance()
            val token = userCache?.getToken()
            if (token == null || token.isEmptyTrimmed()) {
                authorizeUser(apiKey, refUserId, nickname, callback)
            }
        }

        private fun authorizeUser(
            apiKey: String,
            refUserId: String? = null,
            nickname: String? = null,
            callback: ((result: UserAuthResult) -> Unit)? = null
        ) {
            val response = Repository.generateUser(UserRequest(apiKey, refUserId, nickname))
            response.observeForever(object : Observer<Resource<User>> {
                override fun onChanged(it: Resource<User>?) {
                    when (val responseStatus = it?.status) {
                        is Status.Success -> {
                            val user = responseStatus.data
                            user?.apply {
                                if (token != null && id != null)
                                    UserCache.getInstance()?.saveUserAuthInfo(token, id)
                            }
                            UserCache.getInstance()?.saveApiKey(apiKey)
                            callback?.invoke(UserAuthResult.Success)
                            response.removeObserver(this)
                        }
                        is Status.Failure -> {
                            callback?.invoke(UserAuthResult.Failure(responseStatus.errorMessage))
                            response.removeObserver(this)
                        }
                    }
                }
            })
        }
    }

    private lateinit var currentlyDisplayedStream: StreamResponse
    private var listOfStreams: List<StreamResponse>? = null
    internal val handlerFab: Handler = Handler(Looper.getMainLooper())
    private val setOfDismissed = mutableListOf<Int>()
    internal var isSwipeInProgress = false

    private var repo: Repository = Repository()

    internal var counter = 0

    private var newVideosCount = 0

    private val transitionListener = object : MotionLayout.TransitionListener {
        override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {}

        override fun allowsTransition(p0: MotionScene.Transition?): Boolean {
            return true
        }

        override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {}

        override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {}

        override fun onTransitionCompleted(p0: MotionLayout?, currentId: Int) {
            if (currentId == R.id.start) {
                expandableLayout.visibility = View.INVISIBLE
                if (isSwipeInProgress) {
                    listOfStreams?.let { listOfStreams ->
                        for (stream in listOfStreams) {
                            stream.streamId?.let { setOfDismissed.add(it) }
                        }
                    }
                    handlerFab.removeCallbacksAndMessages(null)
                }
            }
            isSwipeInProgress = false
        }
    }

    init {
        if (BASE_URL.isEmptyTrimmed())
            BASE_URL =
                UserCache.getInstance(context)?.getBeChoice() ?: DevSettingsDialog.DEFAULT_URL

        View.inflate(context, R.layout.antourage_fab_layout, this)
        motionOverlayView.setFabListener(this)

        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        floatingActionButton.setOnClickListener(object : OnSingleClickListener() {
            override fun onSingleClick(v: View) {
                context.startActivity(intent)
            }
        })
        floatingActionButton.scaleType = ImageView.ScaleType.CENTER
        AntourageFabLifecycleObserver.registerActionHandler(this)
    }

    override fun onResume() {
        expandableLayout.setTransitionListener(transitionListener)
        ReceivingVideosManager.setReceivingVideoCallback(object :
            ReceivingVideosManager.ReceivingVideoCallback {
            override fun onNewVideosCount(resource: Resource<Int>) {
                super.onNewVideosCount(resource)
                when (val status = resource.status) {
                    is Status.Success -> {
                        newVideosCount = status.data ?: 0
                        manageVideos(status.data ?: 0)
                    }
                    is Status.Failure -> {
                        changeBadgeStatus(WidgetStatus.Inactive)
                    }
                }
            }

            override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
                when (resource.status) {
                    is Status.Success -> {
                        val list = (resource.status.data)?.toMutableList()
                        if (list != null && list.size > 0) {
                            listOfStreams = list
                            changeBadgeStatus(WidgetStatus.ActiveLiveStream(list))
                        } else {
                            manageVideos(newVideosCount)
                        }
                    }
                    is Status.Failure -> {
                        changeBadgeStatus(WidgetStatus.Inactive)
                        BaseViewModel.error.postValue(resource.status.errorMessage)
                    }
                }
            }
        })
        ReceivingVideosManager.startReceivingVideos()
        ReceivingVideosManager.getNewVODsCount()
    }

    override fun onPause() {
        expandableLayout.visibility = View.INVISIBLE
        expandableLayout.setTransitionListener(null)
        ReceivingVideosManager.stopReceivingVideos()
    }

    override fun onFabExpansionClicked() {
        val intent = Intent(context, AntourageActivity::class.java)
        intent.putExtra(ARGS_STREAM_SELECTED, currentlyDisplayedStream)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.applicationContext.startActivity(intent)
    }

    override fun onSwipeStarted() {
        isSwipeInProgress = true
    }

    private fun manageVideos(newVideosCount: Int) {
        changeBadgeStatus(
            if (newVideosCount > 0) WidgetStatus.ActiveUnseenVideos(newVideosCount)
            else WidgetStatus.Inactive
        )
    }

    private fun changeBadgeStatus(status: WidgetStatus) {
        when (status) {
            is WidgetStatus.Inactive -> {
                handlerFab.removeCallbacksAndMessages(null)
                listOfStreams = null
                counter = 0
                floatingActionButton.setImageResource(R.drawable.antourage_ic_antourage_logo_white)
                floatingActionButton.setTextToBadge("")
            }
            is WidgetStatus.ActiveLiveStream -> {
                if (floatingActionButton != null)
                    floatingActionButton.setImageResource(R.drawable.antourage_ic_antourage_logocolor)
                if (!handlerFab.hasMessages(0))
                    handlerFab.postDelayed(object : Runnable {
                        override fun run() {
                            listOfStreams?.let { listOfStreams ->
                                if (counter > (listOfStreams.size - 1)) {
                                    counter = 0
                                }
                                if (!setOfDismissed.contains(listOfStreams[counter].streamId)) {
                                    currentlyDisplayedStream = listOfStreams[counter]
                                    findViewById<MotionOverlayView>(R.id.motionOverlayView).findViewById<TextView>(
                                        R.id.tvStreamTitle
                                    ).text = listOfStreams[counter].streamTitle
                                    findViewById<MotionOverlayView>(R.id.motionOverlayView).findViewById<TextView>(
                                        R.id.tvViewers
                                    ).text = resources.getQuantityString(
                                        R.plurals.ant_number_of_viewers,
                                        currentlyDisplayedStream.viewersCount ?: 0,
                                        currentlyDisplayedStream.viewersCount
                                    )
                                    expandableLayout.visibility = View.VISIBLE
                                    expandableLayout.transitionToEnd()
                                    tvStreamTitle.text = listOfStreams[counter].streamTitle
                                    tvViewers.text = resources.getQuantityString(
                                        R.plurals.ant_number_of_viewers,
                                        currentlyDisplayedStream.viewersCount ?: 0,
                                        currentlyDisplayedStream.viewersCount
                                    )
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
                floatingActionButton.setTextToBadge(context.getString(R.string.ant_live))
            }
            is WidgetStatus.ActiveUnseenVideos -> {
                if (floatingActionButton != null) {
                    floatingActionButton.setImageResource(R.drawable.antourage_ic_antourage_logocolor)
                    floatingActionButton.setTextToBadge(status.numberOfVideos.toString())
                }
                handlerFab.removeCallbacksAndMessages(null)
                listOfStreams = null
                counter = 0
            }
        }
    }

    public fun authWith(
        apiKey: String,
        refUserId: String? = null,
        nickname: String? = null,
        callback: ((result: UserAuthResult) -> Unit)? = null
    ) {
        val userCache = UserCache.getInstance(context)
        val token = userCache?.getToken()
        if (token == null || token.isEmptyTrimmed()) {
            authorizeUser(apiKey, refUserId, nickname, callback)
        }
    }

    private fun authorizeUser(
        apiKey: String,
        refUserId: String? = null,
        nickname: String? = null,
        callback: ((result: UserAuthResult) -> Unit)? = null
    ) {
        val response = Repository.generateUser(UserRequest(apiKey, refUserId, nickname))
        response.observeForever(object : Observer<Resource<User>> {
            override fun onChanged(it: Resource<User>?) {
                when (val responseStatus = it?.status) {
                    is Status.Success -> {
                        val user = responseStatus.data
                        user?.apply {
                            if (token != null && id != null)
                                UserCache.getInstance(context)?.saveUserAuthInfo(token, id)
                        }
                        UserCache.getInstance(context)?.saveApiKey(apiKey)
                        callback?.invoke(UserAuthResult.Success)
                        response.removeObserver(this)
                    }
                    is Status.Failure -> {
                        callback?.invoke(UserAuthResult.Failure(responseStatus.errorMessage))
                        response.removeObserver(this)
                    }
                }
            }
        })
    }
}