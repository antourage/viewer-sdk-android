package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.hideBadge
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.showBadge
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
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
) : ConstraintLayout(context, attrs, defStyleAttr), FabActionHandler {

    companion object {
        internal const val ARGS_STREAM_SELECTED = "args_stream_selected"
        internal const val TAG = "AntourageFabLogs"
        internal var mLastClickTime: Long = 0


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

    private var goingLiveToLive: Boolean = false
    private var currentlyDisplayedLiveStream: StreamResponse? = null
    private var currentAnimationDrawableId: Int = -1
    private val liveStreams = arrayListOf<StreamResponse>()
    private val shownLiveStreams = linkedSetOf<StreamResponse>()
    private val vods = arrayListOf<StreamResponse>()
    private var currentFabState: FabState = FabState.INACTIVE
    private var nextFabState: FabState? = null
    private var isAnimationRunning = false
    private var circleAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var playIconAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var playIconAlphaHandler: Handler = Handler()
    private var playIconStartHandler: Handler = Handler()
    private var bounceHandler: Handler = Handler()
    private var currentPlayerState: Int = 0
    private var isShowingLive: Boolean = false
    private var badgeColor: Drawable? = null
    private var textBadge: String = ""
        set(value) {
            if (value == badgeView.text) return
            if (value.isNotEmpty()) badgeView.text = value
            updateBadgeColor(value)
            if (ViewCompat.isLaidOut(this)) {
                invalidateBadge(value)
            }
        }

    init {
        if (BASE_URL.isEmptyTrimmed()) BASE_URL =
            UserCache.getInstance(context)?.getBeChoice() ?: DevSettingsDialog.DEFAULT_URL
        View.inflate(context, R.layout.antourage_fab_layout, this)
        fabContainer.onClick { checkWhatToOpen() }
        AntourageFabLifecycleObserver.registerActionHandler(this)
        clearStreams()
        initPlayAnimation()
    }


    override fun onResume() {
            ReceivingVideosManager.setReceivingVideoCallback(object :
                ReceivingVideosManager.ReceivingVideoCallback {
                override fun onVODForFabReceived(resource: Resource<List<StreamResponse>>) {
                    super.onVODForFabReceived(resource)
                    when (val status = resource.status) {
                        is Status.Success -> {
                            if (!status.data.isNullOrEmpty()) {
                                vods.clear()
                                vods.addAll(status.data)
                                manageVODs()
                            } else {
                                vods.clear()
                                manageVODs()
                            }
                        }
                        is Status.Failure -> {
                            vods.clear()
                            manageVODs()
                        }
                    }
                }

                override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
                    when (val status = resource.status) {
                        is Status.Success -> {
                            if (!status.data.isNullOrEmpty()) {
                                liveStreams.clear()
                                liveStreams.addAll(status.data)
                                if (!goingLiveToLive) {
                                    manageLiveStreams()
                                }
                            } else {
                                manageVODs(true)
                                goingLiveToLive = false
                            }
                        }
                        is Status.Failure -> {
                            manageVODs(true)
                            goingLiveToLive = false
                        }
                    }
                }
            })

            StreamPreviewManager.setStreamManager(object : StreamPreviewManager.StreamCallback {
                override fun onNewState(playbackState: Int) {
                    super.onNewState(playbackState)
                    currentPlayerState = playbackState
                    if (playbackState == Player.STATE_READY) {
                        bounceHandler.removeCallbacksAndMessages(null)
                        setIncomingWidgetStatus(FabState.LIVE)
                    }
                }

                override fun onError() {
                    super.onError()
                    manageVODs(true)
                }
            })

        Handler().postDelayed({
            if (userAuthorized()) {
                startAntRequests()
            }
        }, 1200)
    }

    private fun manageLiveStreams() {
        if (!newestStreamWasShown() && !goingLiveToLive) {
            if (isShowingLive) {
                setLiveToLiveState()
            } else {
                setLiveState()
            }
        }
    }

    private fun manageVODs(liveEnded: Boolean = false) {
        if (Global.networkAvailable) {
            if (liveEnded || !isShowingLive) {
                setVodState()
            }
        } else {
            setIncomingWidgetStatus(FabState.INACTIVE)
        }
    }

    private fun setLiveState() {
        setIncomingWidgetStatus(FabState.PRE_LIVE)
    }

    private fun setLiveToLiveState() {
        goingLiveToLive = true
        setIncomingWidgetStatus(FabState.INACTIVE)
    }

    private fun setVodState() {
        if (vods.isNotEmpty()) {
            if (currentFabState != FabState.PRE_VOD && currentFabState != FabState.VOD && nextFabState != FabState.PRE_VOD && nextFabState != FabState.VOD) {
                setIncomingWidgetStatus(FabState.PRE_VOD)
                return
            }
        } else {
            setIncomingWidgetStatus(FabState.INACTIVE)
        }
    }

    private fun setIncomingWidgetStatus(status: FabState?) {
        nextFabState = if (!isAnimationRunning && status != null && status != currentFabState) {
            changeFabState(status)
            null
        } else {
            status
        }
    }

    private fun changeFabState(state: FabState) {
        currentFabState = state
        hideStreamPreview(state)
        when (state) {
            FabState.INACTIVE -> {
                hideBadge()
                circleAnimatedDrawable?.clearAnimationCallbacks()
                if (goingLiveToLive) {
                    Handler().postDelayed({
                        setIncomingWidgetStatus(FabState.PRE_LIVE)
                    }, 1200)
                }
            }
            FabState.PRE_LIVE -> {
                isShowingLive = true
                val streamToDisplay = getNextStreamToDisplay()
                streamToDisplay?.let {
                    currentlyDisplayedLiveStream = streamToDisplay
                    startPlayingStream(streamToDisplay)
                    shownLiveStreams.add(streamToDisplay)
                }
                setTextToBadge(context.getString(R.string.ant_live))
                startAnimation(state)
            }
            FabState.LIVE -> {
                if (goingLiveToLive) goingLiveToLive = false
                showPlayer()
                startAnimation(state)
            }
            FabState.PRE_VOD -> {
                setTextToBadge(context.getString(R.string.ant_new_vod))
                startAnimation(state)
            }
            FabState.VOD -> {
                startAnimation(state)
            }
        }
    }

    private fun startAnimation(animation: FabState) {
        setAnimatedDrawable(animation.animationDrawableId)
        circleAnimatedDrawable?.clearAnimationCallbacks()
        circleAnimatedDrawable?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationStart(drawable: Drawable?) {
                    super.onAnimationStart(drawable)
                    isAnimationRunning = true
                }

                override fun onAnimationEnd(drawable: Drawable?) {
                    isAnimationRunning = false
                    if (nextFabState != null) {
                        changeFabState(nextFabState!!)
                        nextFabState = null
                        return
                    }
                    when (animation) {
                        FabState.PRE_LIVE -> {
                            currentPlayerState.let {
                                if (it != Player.STATE_READY) {
                                    bounceHandler.postDelayed({
                                        startAnimation(FabState.PRE_LIVE)
                                    }, 800)
                                }
                            }
                        }
                        FabState.LIVE -> {
                            currentPlayerState.let {
                                if (it == Player.STATE_READY && playerView.player.isPlaying)
                                    startAnimation(
                                        FabState.LIVE
                                    )
                            }
                        }
                        FabState.PRE_VOD -> {
                            setIncomingWidgetStatus(FabState.VOD)
                        }
                        FabState.VOD -> {
                            startAnimation(FabState.VOD)
                        }
                        FabState.INACTIVE -> {
                        }
                    }
                }
            })
            start()
        }
    }

    private fun hideStreamPreview(state: FabState) {
        if (state != FabState.LIVE && state != FabState.PRE_LIVE) {
            releasePlayer()
        }
    }

    private fun showPlayer() {
        playerView.alpha = 0f
        playerView.visibility = View.VISIBLE
        playerView.animate().alpha(1f).setDuration(300).start()
        logoView.animate().alpha(0f).setDuration(300).start()
        playIconStartHandler.postDelayed({
            showPlayAnimation()
        }, 1200)
    }

    private fun releasePlayer() {
        isShowingLive = false
        clearStreams()
        StreamPreviewManager.releasePlayer()
        playerView.animate().alpha(0f).setDuration(300).start()
        logoView.animate().alpha(1f).setDuration(300).start()
        playIconView.alpha = 0f
        playIconAlphaHandler.removeCallbacksAndMessages(null)
        playIconStartHandler.removeCallbacksAndMessages(null)
        playIconAnimatedDrawable?.clearAnimationCallbacks()
        playIconAnimatedDrawable?.stop()
        playIconView.animation?.cancel()
        playIconView.animation?.reset()
        playIconView.clearAnimation()
    }

    private fun showPlayAnimation() {
        /** play animation doesn't repeat without this on some lower M devices*/
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            initPlayAnimation()
        }
        playIconView.alpha = 0f
        playIconView.visibility = View.VISIBLE
        playIconView.animate().alpha(1f).setDuration(100).start()
        playIconAnimatedDrawable?.apply {
            clearAnimationCallbacks()
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    if (isShowingLive) {
                        showPlayAnimation()
                    }
                }
            })
            start()
            playIconAlphaHandler.postDelayed({
                playIconView.animate().alpha(0f).setDuration(200).start()
            }, 3800)
        }
    }

    private fun initPlayAnimation() {
        playIconAnimatedDrawable = context?.let {
            AnimatedVectorDrawableCompat.create(
                it,
                R.drawable.antourage_fab_play_animation
            )
        }
        playIconView.setImageDrawable(playIconAnimatedDrawable)
    }

    private fun setAnimatedDrawable(drawableId: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || currentAnimationDrawableId != drawableId) {
            currentAnimationDrawableId = drawableId
            circleAnimatedDrawable = context?.let {
                AnimatedVectorDrawableCompat.create(
                    it,
                    drawableId
                )
            }
            circleView.setImageDrawable(circleAnimatedDrawable)
            circleView.background = null
        }
    }

    override fun onPause() {
        StreamPreviewManager.removeEventListener()
        ReceivingVideosManager.stopReceivingVideos()
        currentPlayerState = 0
        isAnimationRunning = false
        goingLiveToLive = false
        vods.clear()
        currentFabState = FabState.INACTIVE
        setIncomingWidgetStatus(null)
        bounceHandler.removeCallbacksAndMessages(null)
        Handler().postDelayed({
            circleAnimatedDrawable?.apply {
                clearAnimationCallbacks()
            }
            circleAnimatedDrawable?.stop()
            textBadge = ""
            releasePlayer()
        }, 100)
    }


    private fun startPlayingStream(stream: StreamResponse) {
        playerView.player = stream.hlsUrl?.get(0)?.let {
            StreamPreviewManager.getExoPlayer(
                it, context
            )
        }
    }

    private fun invalidateBadge(value: String) {
        if (value == context.getString(R.string.ant_live) || value == context.getString(R.string.ant_new_vod)) {
            showBadge()
        } else {
            hideBadge()
        }
    }

    private fun showBadge() {
        badgeView.background = badgeColor
        badgeView.showBadge()
    }

    private fun hideBadge() {
        badgeView.hideBadge()
    }

    private fun updateBadgeColor(value: String) {
        val backgroundDrawableId =
            if (value == context.getString(R.string.ant_live)) R.drawable.antourage_fab_badge_rounded_background_live else R.drawable.antourage_fab_badge_rounded_background_vod
        badgeColor =
            ContextCompat.getDrawable(
                context,
                backgroundDrawableId
            )
    }

    private fun getNextStreamToDisplay(): StreamResponse? {
        for (element in liveStreams) {
            if (shownLiveStreams.none { it.id == element.id }) {
                return element
            }
        }
        return null
    }

    private fun newestStreamWasShown(): Boolean {
        return if (liveStreams.isNotEmpty()) shownLiveStreams.contains(liveStreams[0])
        else false
    }

    fun authWith(
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

    private fun checkWhatToOpen() {
        //to prevent quick multiple taps
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()

        when (currentFabState) {
            FabState.INACTIVE -> {
                openAntActivity()
            }
            FabState.LIVE -> {
                openLiveStreamActivity()
            }
            FabState.PRE_LIVE -> {
                openLiveStreamActivity()
            }
            FabState.PRE_VOD -> {
                openVodActivity()
            }
            FabState.VOD -> {
                openVodActivity()
            }
        }
    }

    private fun clearStreams() {
        currentlyDisplayedLiveStream = null
        shownLiveStreams.clear()
    }

    private fun openLiveStreamActivity() {
        if (currentlyDisplayedLiveStream != null) {
            val intent = Intent(context, AntourageActivity::class.java)
            currentlyDisplayedLiveStream?.isLive = true
            intent.putExtra(ARGS_STREAM_SELECTED, currentlyDisplayedLiveStream)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    private fun openVodActivity() {
        if (vods.isNotEmpty()) {
            Repository.vods = vods.toMutableList()
            val intent = Intent(context, AntourageActivity::class.java)
            intent.putExtra(ARGS_STREAM_SELECTED, vods[0])
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    private fun openAntActivity() {
        setAllLiveStreamsAsSeen()
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
    if user goes to videos list screen, he will see all live videos anyway
    so no need to show them in fab expansion
     */
    private fun setAllLiveStreamsAsSeen() {
        shownLiveStreams.addAll(liveStreams)
    }

    private fun startAntRequests() {
        ReceivingVideosManager.startReceivingLiveStreams(true)
    }

    private fun userAuthorized(): Boolean {
        return !(UserCache.getInstance(context)?.getToken().isNullOrBlank())
    }

    private fun setTextToBadge(text: String) {
        textBadge = text
    }

    internal enum class FabState(val animationDrawableId: Int) {
        INACTIVE(0),
        PRE_LIVE(R.drawable.antourage_fab_bounce_once_animation),
        LIVE(R.drawable.antourage_fab_rotation_animation),
        PRE_VOD(R.drawable.antourage_fab_bounce_animation),
        VOD(R.drawable.antourage_fab_rotation_animation)
    }
}