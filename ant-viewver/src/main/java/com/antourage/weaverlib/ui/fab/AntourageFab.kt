package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Keep
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import kotlinx.android.synthetic.main.layout_motion_fab.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick

/**
 * When integrating to React Native need to add also constraint layout library in declaration
 * so it would know that it is subclass of View
 */

@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler,
    MotionOverlayView.FabExpansionListener {

    companion object {
        internal const val ARGS_STREAM_SELECTED = "args_stream_selected"
        internal const val TAG = "AntourageFabLogs"

        private const val FAB_EXPANSION_ANIM_DURATION = 5000L

        fun registerNotifications(
            fcmToken: String,
            callback: ((result: RegisterPushNotificationsResult) -> Unit)? = null
        ) {
            Log.d(TAG, "Trying to register ant push notifications...")
            val response =
                Repository.subscribeToPushNotifications(SubscribeToPushesRequest(fcmToken))
            response.observeForever(object : Observer<Resource<NotificationSubscriptionResponse>> {
                override fun onChanged(it: Resource<NotificationSubscriptionResponse>?) {
                    when (val responseStatus = it?.status) {
                        is Status.Success -> {
                            responseStatus.data?.topic?.let { topicName ->
                                RegisterPushNotificationsResult.Success(
                                    topicName
                                )
                            }?.let { result -> callback?.invoke(result) }
                            Log.d(TAG, "Push notification registration successful")
                            responseStatus.data?.topic?.let {
                                Log.d(TAG, "Topic name: $it")
                            }
                            response.removeObserver(this)
                        }
                        is Status.Failure -> {
                            callback?.invoke(RegisterPushNotificationsResult.Failure(responseStatus.errorMessage))
                            Log.d(
                                TAG,
                                "Push notification registration failed; ${responseStatus.errorMessage}"
                            )
                            response.removeObserver(this)
                        }
                    }
                }
            })
        }
    }

    private val handlerFabExpansion: Handler = Handler(Looper.getMainLooper())
    private var currentlyDisplayedLiveStreamIndex = 0
    private val shownLiveStreams = linkedSetOf<StreamResponse>()
    private val currentLiveStreams = arrayListOf<StreamResponse>()

    private val transitionListener = object : FabExpansionTransitionListener {

        override fun onTransitionCompleted(p0: MotionLayout?, currentId: Int) {
            if (currentId == R.id.start) {
                expandableLayout.visibility = View.INVISIBLE
            }
        }
    }

    init {
        if (BASE_URL.isEmptyTrimmed())
            BASE_URL =
                UserCache.getInstance(context)?.getBeChoice() ?: DevSettingsDialog.DEFAULT_URL

        View.inflate(context, R.layout.antourage_fab_layout, this)
        motionOverlayView.setFabListener(this)
        floatingActionButton.onClick { openAntActivity() }
        floatingActionButton.scaleType = ImageView.ScaleType.CENTER
        AntourageFabLifecycleObserver.registerActionHandler(this)
        resetFabExpansion()
    }

    override fun onResume() {
        expandableLayout.setTransitionListener(transitionListener)
        ReceivingVideosManager.setReceivingVideoCallback(object :
            ReceivingVideosManager.ReceivingVideoCallback {

            override fun onNewVideosCount(resource: Resource<Int>) {
                super.onNewVideosCount(resource)
                when (val status = resource.status) {
                    is Status.Success -> {
                        manageNewUnseenVideosBadge(status.data ?: 0)
                    }
                }
            }

            override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
                when (val status = resource.status) {
                    is Status.Success -> {
                        if (!status.data.isNullOrEmpty()) {
                            changeBadgeStatus(WidgetStatus.ActiveLiveStream(status.data))
                        } else {
                            if (floatingActionButton.isLiveBadgeShown()) {
                                changeBadgeStatus(WidgetStatus.Inactive)
                            }
                        }
                    }
                    is Status.Failure -> {
                        changeBadgeStatus(WidgetStatus.Inactive)
                    }
                }
            }
        })

        if (userAuthorized()) {
            startAntRequests()
        }
    }

    override fun onPause() {
        expandableLayout.visibility = View.INVISIBLE
        expandableLayout.setTransitionListener(null)
        ReceivingVideosManager.stopReceivingVideos()
    }

    override fun onFabExpansionClicked() {
        val intent = Intent(context, AntourageActivity::class.java)
        intent.putExtra(ARGS_STREAM_SELECTED, getCurrentlyDisplayedLiveStream())
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.applicationContext.startActivity(intent)
    }

    private fun manageNewUnseenVideosBadge(newVideosCount: Int) {
        if (!floatingActionButton.isLiveBadgeShown()) {
            changeBadgeStatus(
                if (newVideosCount > 0) WidgetStatus.ActiveUnseenVideos(newVideosCount)
                else WidgetStatus.Inactive
            )
        }
    }

    private fun changeBadgeStatus(status: WidgetStatus) {
        when (status) {
            is WidgetStatus.Inactive -> {
                resetFabExpansion()
                floatingActionButton?.hideBadge()
            }
            is WidgetStatus.ActiveLiveStream -> {
                if (!handlerFabExpansion.hasMessages(0))
                    handlerFabExpansion.post(object : Runnable {
                        override fun run() {
                            status.list.let { listOfStreams ->
                                if (allTheLiveStreamsWereShown(listOfStreams)) {
                                    handlerFabExpansion.removeCallbacksAndMessages(null)
                                } else {
//                                    currentlyDisplayedLiveStream =
//                                        listOfStreams[currentlyDisplayedLiveStreamIndex]
//                                    findViewById<MotionOverlayView>(R.id.motionOverlayView).findViewById<TextView>(
//                                        R.id.tvStreamTitle
//                                    ).text =
//                                        listOfStreams[currentlyDisplayedLiveStreamIndex].streamTitle
//                                    findViewById<MotionOverlayView>(R.id.motionOverlayView).findViewById<TextView>(
//                                        R.id.tvViewers
//                                    ).text = resources.getQuantityString(
//                                        R.plurals.ant_number_of_viewers,
//                                        currentlyDisplayedLiveStream.viewersCount ?: 0,
//                                        currentlyDisplayedLiveStream.viewersCount
//                                    )
//                                    expandableLayout.visibility = View.VISIBLE
//                                    expandableLayout.transitionToEnd()
//                                    tvStreamTitle.text =
//                                        listOfStreams[currentlyDisplayedLiveStreamIndex].streamTitle
//                                    tvViewers.text = resources.getQuantityString(
//                                        R.plurals.ant_number_of_viewers,
//                                        currentlyDisplayedLiveStream.viewersCount ?: 0,
//                                        currentlyDisplayedLiveStream.viewersCount
//                                    )
//                                    Handler(Looper.getMainLooper()).postDelayed({
//                                        expandableLayout.transitionToStart()
//                                    }, FAB_EXPANSION_ANIM_DURATION)
//                                    handlerFabExpansion.postDelayed(
//                                        this,
//                                        2 * FAB_EXPANSION_ANIM_DURATION
//                                    )
//                                    currentlyDisplayedLiveStreamIndex++
                                }
                            }
                        }
                    })
                floatingActionButton.setTextToBadge(context.getString(R.string.ant_live))
            }
            is WidgetStatus.ActiveUnseenVideos -> {
                resetFabExpansion()
                floatingActionButton?.showNewVODsBadge()
            }
        }
    }

    private fun allTheLiveStreamsWereShown(liveStreams: List<StreamResponse>): Boolean {
        return !liveStreams.any(shownLiveStreams::contains)
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
        } else {
            callback?.invoke(UserAuthResult.Success)
        }
    }

    private fun authorizeUser(
        apiKey: String,
        refUserId: String? = null,
        nickname: String? = null,
        callback: ((result: UserAuthResult) -> Unit)? = null
    ) {
        Log.d(TAG, "Trying to authorize ant user...")
        refUserId?.let { UserCache.getInstance(context)?.saveUserRefId(it) }
        nickname?.let { UserCache.getInstance(context)?.saveUserNickName(it) }
        UserCache.getInstance(context)?.saveApiKey(apiKey)

        val response = Repository.generateUser(UserRequest(apiKey, refUserId, nickname))
        response.observeForever(object : Observer<Resource<User>> {
            override fun onChanged(it: Resource<User>?) {
                when (val responseStatus = it?.status) {
                    is Status.Success -> {
                        val user = responseStatus.data
                        Log.d(TAG, "Ant authorization successful")
                        user?.apply {
                            if (token != null && id != null) {
                                Log.d(
                                    TAG,
                                    "Ant token and ant userId != null, started live video timer"
                                )
                                UserCache.getInstance(context)?.saveUserAuthInfo(token, id)
                                startAntRequests()
                            }
                        }
                        callback?.invoke(UserAuthResult.Success)
                        response.removeObserver(this)
                    }
                    is Status.Failure -> {
                        Log.d(TAG, "Ant authorization failed: ${responseStatus.errorMessage}")
                        callback?.invoke(UserAuthResult.Failure(responseStatus.errorMessage))
                        response.removeObserver(this)
                    }
                }
            }
        })
    }

    private fun openAntActivity() {
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun startAntRequests() {
        ReceivingVideosManager.startReceivingLiveStreams()
        ReceivingVideosManager.startReceivingNewVODsCount()
    }

    private fun userAuthorized(): Boolean {
        return !(UserCache.getInstance(context)?.getToken().isNullOrBlank())
    }

    private fun resetFabExpansion() {
        handlerFabExpansion.removeCallbacksAndMessages(null)
        currentlyDisplayedLiveStreamIndex = 0
        shownLiveStreams.clear()
        currentLiveStreams.clear()
    }

    private fun getCurrentlyDisplayedLiveStream(): StreamResponse =
        shownLiveStreams.elementAt(currentlyDisplayedLiveStreamIndex)
}