package com.antourage.weaverlib.ui.fab

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.ConfigManager
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.hideBadge
import com.antourage.weaverlib.other.models.NotificationSubscriptionResponse
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.SubscribeToPushesRequest
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor.Companion.internetStateLiveData
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.showBadge
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.google.android.exoplayer2.Player
import com.google.android.material.internal.ContextUtils.getActivity
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*
import android.graphics.Outline

import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.core.graphics.drawable.DrawableCompat
import kotlinx.android.synthetic.main.antourage_live_name_layout.view.*
import kotlinx.android.synthetic.main.antourage_onboarding_layout.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor


/**
 * When integrating to React Native need to add also constraint layout library in declaration
 * so it would know that it is subclass of View
 */

@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), LifecycleEventObserver {

    private var idleInterval: Int = 60
    private var backgroundView: WidgetDarkBackgroundView = WidgetDarkBackgroundView(context)
    private var goingLiveToLive: Boolean = false
    private var currentlyDisplayedLiveStream: StreamResponse? = null
    private var currentAnimationDrawableId: Int = -1
    private val liveStreams = arrayListOf<StreamResponse>()
    private var vod: StreamResponse? = null
    private val shownLiveStreams = linkedSetOf<StreamResponse>()
    private var currentWidgetState: WidgetState = WidgetState.INITIAL
    private var nextWidgetState: WidgetState? = null
    private var isAnimationRunning = false
    private var portalAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var isShowingLive: Boolean = false
    private var currentPlayerState: Int = 0


    init {
        View.inflate(context, R.layout.antourage_fab_layout, this)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AntourageFab,
            0, 0
        ).apply {

            try {


//    <attr name="portalColor" format="color"/>
//    <attr name="ctaBackgroundColor" format="color"/>
//    <attr name="ctaTextColor" format="color"/>
//    <attr name="overlayBackgroundColor" format="color"/>
//    <attr name="overlayAlpha" format="float"/>
//    <attr name="liveDotColor" format="color"/>
//    <attr name="titleTextColor" format="color"/>
//    <attr name="titleBackgroundColor" format="color"/>
//    <attr name="nameTextColor" format="color"/>
//    <attr name="nameBackgroundColor" format="color"/>
//    <attr name="idleInterval" format="integer"/>


//                setPortalColor(getColor(R.styleable.AntourageFab_portalColor, 0))
//                setCTATextColor(getColor(R.styleable.AntourageFab_ctaTextColor, 0))
//                setCTABackgroundColor(getColor(R.styleable.AntourageFab_ctaBackgroundColor, 0))
                setLiveDotColor(getColor(R.styleable.AntourageFab_liveDotColor, 0))
                setTitleTextColor(getColor(R.styleable.AntourageFab_titleTextColor, 0))
                setTitleBackgroundColor(getColor(R.styleable.AntourageFab_titleBackgroundColor, 0))
                setNameTextColor(getColor(R.styleable.AntourageFab_nameTextColor, 0))
                setNameBackgroundColor(getColor(R.styleable.AntourageFab_nameBackgroundColor, 0))
                setIdleInterval(getInt(R.styleable.AntourageFab_nameBackgroundColor, 0))
//                setOverlayAlpha(getFloat(R.styleable.AntourageFab_overlayAlpha, 0f))
//                setOverlayBackgroundColor(getColor(R.styleable.AntourageFab_overlayBackgroundColor, 0))
            } finally {
                recycle()
            }
        }

//        backgroundView.setOnClickListener {
//            backgroundView.hideView()
//            liveNameView.hideView()
//        }

        dummyView.onClick {
//            backgroundView.hideView()
            checkWhatToOpen()
        }
        bigDummyView.onClick {
//            backgroundView.hideView()
            checkWhatToOpen()
        }
        clearStreams()
    }

    companion object {
        private var cachedFcmToken: String = ""
        internal var isSubscribedToPushes = false
        private var pushRegistrationCallback: ((result: RegisterPushNotificationsResult) -> Unit)? =
            null
        internal const val TAG = "AntourageFabLogs"
        internal var mLastClickTime: Long = 0
        const val AntourageSenderId = "1090288296965"

        /** added to prevent multiple calls of onResume breaking widget logic*/
        internal var wasPaused = true

        /**
         *  Method for configuring fab that initializes all needed library instances
         *  */
        fun configure(context: Context) {
            UserCache.getInstance(context)
            ConfigManager.init(context)
            handleDeviceId(context)
            setDefaultLocale(context)
            if (!isSubscribedToPushes) retryRegisterNotifications()
            startAntRequests()
        }

        internal fun reconfigure(context: Context) {
            UserCache.getInstance(context)
            ConfigManager.init(context)
            handleDeviceId(context)
            setDefaultLocale(context)
        }

        private fun setDefaultLocale(context: Context) {
            val defaultLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                context.resources.configuration.locale
            }

            if (defaultLocale.toLanguageTag().contains("sv")) {
                Global.defaultLocale = defaultLocale
            } else {
                Global.defaultLocale = Locale("en", "US")
            }
        }

        @SuppressLint("HardwareIds")
        private fun handleDeviceId(context: Context) {
            if (UserCache.getInstance()?.getDeviceId() == null) {
                UserCache.getInstance()?.saveDeviceId(
                    Settings.Secure.getString(
                        context.contentResolver,
                        Settings.Secure.ANDROID_ID
                    ) ?: ""
                )
            }
        }

        private fun startAntRequests(isInitial: Boolean = true) {
            ReceivingVideosManager.isFirstRequest = true
            ReceivingVideosManager.startReceivingLiveStreams(true)
        }

        fun retryRegisterNotifications(firebaseToken: String? = null) {
            if (pushRegistrationCallback == null) return
            if (firebaseToken != null) cachedFcmToken = firebaseToken
            if (UserCache.getInstance() != null && UserCache.getInstance()!!.getIdToken()
                    .isNullOrBlank()
            ) {
                return
            }
            if (cachedFcmToken.isNotEmpty() && pushRegistrationCallback != null) {
                Handler(Looper.getMainLooper()).post {
                    registerNotifications(cachedFcmToken, pushRegistrationCallback)
                }
            }
        }

        fun registerNotifications(
            fcmToken: String?,
            callback: ((result: RegisterPushNotificationsResult) -> Unit)? = null
        ) {
            Log.d(TAG, "Trying to register ant push notifications...")
            pushRegistrationCallback = callback
            if (fcmToken.isNullOrEmpty()) return
            cachedFcmToken = fcmToken
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
                            isSubscribedToPushes = true
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

    private fun showBadge(text: String) {
        if (text == badgeView?.text) return
        badgeView?.text = text
        when (text) {
            context.getString(R.string.ant_live) -> {
                badgeView?.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.antourage_fab_badge_rounded_background_live
                )
            }
            context.getString(R.string.ant_new_vod) -> {
                badgeView?.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.antourage_fab_badge_rounded_background_vod
                )
            }
        }
        badgeView?.showBadge(measureAndLayout)
    }

    private fun hideBadge() {
        badgeView.hideBadge(measureAndLayout)
    }

    /**method for React Native for force hide badge on widget appearing*/
    fun forceHideBadge() {
        badgeView?.alpha = 0f
        badgeView?.text = ""
    }

    fun setLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }


    /**
     * Method to force set locale (currently default or Swedish)
     */
    fun setLocale(lang: String? = null) {
        if (lang != null) {
            when (lang) {
                "sv" -> forceLocale(Locale("sv", "SE"))
                "en" -> forceLocale(Locale("en", "US"))
                else -> Log.e(TAG, "Trying to set unsupported locale $lang")
            }
        } else if (Global.chosenLocale != null) {
            forceLocale(Global.chosenLocale!!)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun forceLocale(locale: Locale) {
        if (getActivity(context) != null) {
            val config =
                Configuration(getActivity(context)!!.resources.configuration)
            Locale.setDefault(locale)
            config.setLocale(locale)
            Global.chosenLocale = locale
            getActivity(context)?.baseContext?.resources?.updateConfiguration(
                config,
                getActivity(context)?.baseContext?.resources?.displayMetrics
            )
        }
    }

    private fun onResume() {
        if (!wasPaused) return
        wasPaused = false
        setNextWidgetState(WidgetState.INACTIVE)
        forceHideBadge()
        setLocale()
        internetStateLiveData.observeForever(networkStateObserver)

        ReceivingVideosManager.setReceivingVideoCallback(object :
            ReceivingVideosManager.ReceivingVideoCallback {
            override fun onVODForFabReceived(resource: Resource<List<StreamResponse>>) {
                super.onVODForFabReceived(resource)

                // TODO manage vods

//                when (val status = resource.status) {
//                    is Status.Success -> {
//                        if (status.data != null && status.data.isNotEmpty()) {
//                            vod = status.data[0]
//                            manageVODs()
//                        } else {
//                            vod = null
//                            manageVODs()
//                        }
//                    }
//                    is Status.Failure -> {
//                        vod = null
//                        manageVODs()
//                    }
//                }
            }

            override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
//                when (val status = resource.status) {
//                    is Status.Success -> {
//                        if (!status.data.isNullOrEmpty()) {
//                            liveStreams.clear()
//                            liveStreams.addAll(status.data)
//                            if (!goingLiveToLive) {
//                                manageLiveStreams()
//                            }
//                        } else {
//                            liveStreams.clear()
//                            manageVODs(true)
//                            goingLiveToLive = false
//                        }
//                    }
//                    is Status.Failure -> {
//                        manageVODs(true)
//                        goingLiveToLive = false
//                    }
//                }
            }
        })

        StreamPreviewManager.setCallback(object : StreamPreviewManager.StreamCallback {
            override fun onNewState(playbackState: Int) {
                super.onNewState(playbackState)
                currentPlayerState = playbackState
                if (playbackState == Player.STATE_READY) {
                    setLiveState()
                }
            }

            override fun onError() {
                super.onError()
                setNextWidgetState(WidgetState.INACTIVE)
            }
        })

        initPreLiveState()

//        Handler(Looper.getMainLooper()).postDelayed({
//            startAntRequests()
//        }, 500)
    }

    private fun onPause() {
        wasPaused = true
        isShowingLive = false
        clearStreams()
        ReceivingVideosManager.stopReceivingVideos()
        isAnimationRunning = false
        goingLiveToLive = false
        vod = null
        currentWidgetState = WidgetState.INITIAL
        setNextWidgetState(null)
        forceHideBadge()
        Handler(Looper.getMainLooper()).postDelayed({
            portalAnimatedDrawable?.apply {
                clearAnimationCallbacks()
            }
            portalAnimatedDrawable?.stop()
        }, 100)
    }

    private fun manageLiveStreams() {
        if (!newestStreamWasShown() && !goingLiveToLive) {
            if (isShowingLive) {
                setLiveToLiveState()
            } else {
                initPreLiveState()
            }
        }
    }


    private fun initPreLiveState() {
        val streamToDisplay = getNextStreamToDisplay()
//        streamToDisplay?.let {
//            isShowingLive = true
//            showBadge(context.getString(R.string.ant_live))
//            currentlyDisplayedLiveStream = streamToDisplay
//            playerView.player = streamToDisplay.hlsUrl?.let {
        playerView.player =
            StreamPreviewManager.startPlayingStream(
                "http://cdnapi.kaltura.com/p/1878761/sp/187876100/playManifest/entryId/1_usagz19w/flavorIds/1_5spqkazq,1_nslowvhp,1_boih5aji,1_qahc37ag/format/applehttp/protocol/http/a.m3u8",
                context
            )
//            shownLiveStreams.add(streamToDisplay)
//        }
    }

    private fun setLiveState() {
        setNextWidgetState(WidgetState.LIVE)
    }

    private fun setLiveToLiveState() {
        goingLiveToLive = true
        setNextWidgetState(WidgetState.INACTIVE)
    }

    private fun setNextWidgetState(status: WidgetState?) {
        nextWidgetState =
            if (!isAnimationRunning && status != null && status != currentWidgetState) {
                changeWidgetState(status)
                null
            } else {
                status
            }
    }

    private fun changeWidgetState(state: WidgetState) {
        currentWidgetState = state
        if (state != WidgetState.LIVE && !goingLiveToLive) {
            isShowingLive = false
            releasePlayer()
            clearStreams()
        }
        when (state) {
            WidgetState.INACTIVE -> {
                hideBadge()
                bigDummyView.visibility = View.GONE
                portalAnimatedDrawable?.clearAnimationCallbacks()
                if (goingLiveToLive) {
                    initPreLiveState()
                }
                startAnimation(state)
            }
            WidgetState.LIVE -> {
                if (goingLiveToLive) goingLiveToLive = false
                showPlayer()
                Handler(Looper.getMainLooper()).postDelayed({
                    liveNameView.revealView()
                    bigDummyView.visibility = View.VISIBLE
                    darkenBackground()
                }, 2300)
                Handler(Looper.getMainLooper()).postDelayed({
                    liveNameView.hideView()
                    bigDummyView.visibility = View.GONE
                    backgroundView.hideView()
                }, 6000)
                startAnimation(state)
            }
        }
    }

    private fun startAnimation(animation: WidgetState) {
        setAnimatedDrawable(animation.animationDrawableId)
        portalAnimatedDrawable?.clearAnimationCallbacks()
        portalAnimatedDrawable?.apply {
            registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationStart(drawable: Drawable?) {
                    super.onAnimationStart(drawable)
                    isAnimationRunning = true
                }

                override fun onAnimationEnd(drawable: Drawable?) {
                    isAnimationRunning = false
                    if (nextWidgetState != null) {
                        changeWidgetState(nextWidgetState!!)
                        nextWidgetState = null
                        return
                    }
                    startAnimation(WidgetState.INACTIVE)
                }
            })
            start()
        }
    }

    private fun setAnimatedDrawable(drawableId: Int) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M || currentAnimationDrawableId != drawableId) {
            currentAnimationDrawableId = drawableId
            portalAnimatedDrawable = context?.let {
                AnimatedVectorDrawableCompat.create(
                    it,
                    drawableId
                )
            }
            ivPortal.setImageDrawable(portalAnimatedDrawable)
//            setPortalColor(ContextCompat.getColor(context, R.color.ant_purple ))
            ivPortal.background = null
        }
    }


    fun setPortalColor(@ColorInt color: Int) {
        if (color != 0)
            DrawableCompat.setTint(ivPortal.drawable, color)
        postInvalidate()
    }

    fun setCTABackgroundColor(@ColorInt color: Int) {
        if (color != 0)
            btnCta.backgroundColor = color
        postInvalidate()
    }

    fun setCTATextColor(@ColorInt color: Int) {
        if (color != 0)
            btnCta.textColor = color
        postInvalidate()
    }

    fun setLiveDotColor(@ColorInt color: Int) {
        if (color != 0)
            dotView.background.setTint(color)
        postInvalidate()
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        if (color != 0){
            tvLiveFirstLine.setTextColor(color)
            tvLiveSecondLine?.setTextColor(color)
        }
        postInvalidate()
    }

    fun setTitleBackgroundColor(@ColorInt color: Int) {
        if (color != 0){
            tvLiveSecondLine?.background?.setTint(color)
            firstLineContainer.background.setTint(color)
        }
        postInvalidate()
    }

    fun setNameTextColor(@ColorInt color: Int) {
        if (color != 0){
            tvStreamerName.setTextColor(color)
        }
        postInvalidate()
    }

    fun setNameBackgroundColor(@ColorInt color: Int) {
        if (color != 0){
            tvStreamerName.background.setTint(color)
        }
        postInvalidate()
    }

    fun setIdleInterval(seconds: Int) {
        if (seconds != 0){
            idleInterval = seconds
        }
    }

    fun setOverlayBackgroundColor(@ColorInt color: Int) {
        if (color != 0){
            onboardingContainer.background.setTint(color)
        }
        backgroundView.postInvalidate()
    }

    fun setOverlayAlpha(alpha: Float) {
        if (alpha != 0f){
            backgroundView.alpha = alpha
        }
        backgroundView.postInvalidate()
    }

    private fun showPlayer() {
        playerView?.player?.playWhenReady = true
        playerView?.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 150f)
            }
        }

        playerView?.clipToOutline = true
        playerView?.alpha = 0f
        playerView?.visibility = View.VISIBLE
        playerView?.animate()?.alpha(1f)?.setStartDelay(2300)?.setDuration(1000)?.withEndAction {
            playerView?.animate()?.alpha(0f)?.setStartDelay(2000)?.setDuration(1000)?.start()
        }?.start()
    }

    private fun releasePlayer() {
        isShowingLive = false
        clearStreams()
        StreamPreviewManager.releasePlayer()
        playerView?.animate()?.alpha(0f)?.setDuration(300)?.start()
    }

    private fun clearStreams() {
        currentlyDisplayedLiveStream = null
        shownLiveStreams.clear()
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

    private fun checkWhatToOpen() {
        //to prevent quick multiple taps
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        openPreFeedActivity()
    }

    private fun openPreFeedActivity() {
        val intent = Intent(context, AntourageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(intent)
    }

    /**
    if user goes to videos list screen, he will see all live videos anyway
    so no need to show them in fab expansion
     */
    private fun setAllLiveStreamsAsSeen() {
        shownLiveStreams.addAll(liveStreams)
    }


    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        when (networkState?.ordinal) {
            NetworkConnectionState.LOST.ordinal -> {
                if (!Global.networkAvailable) {
                    clearStreams()
                    ReceivingVideosManager.pauseWhileNoNetwork()
                    setNextWidgetState(WidgetState.INACTIVE)
                }
            }
            NetworkConnectionState.AVAILABLE.ordinal -> {
                startAntRequests()
            }
        }
    }

    internal enum class WidgetState(val animationDrawableId: Int) {
        INITIAL(R.drawable.antourage_portal_small),
        INACTIVE(R.drawable.antourage_portal_small),
        LIVE(R.drawable.anourage_portal_expanded),
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            onResume()
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            onPause()
        }
    }

    /**
     * Method to darken background behind the fab
     */
    private fun darkenBackground() {
        if (backgroundView.parent != null) {
            (backgroundView.parent as ViewGroup).removeView(backgroundView)
        }
        ((rootView as ViewGroup).findViewById<ViewGroup>(android.R.id.content)
            .getChildAt(0) as ViewGroup).addView(backgroundView)
        backgroundView.startAnimation(this)
    }

    override fun requestLayout() {
        super.requestLayout()
        post(measureAndLayout)
    }

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

    private fun runOnUi(method: () -> Unit) {
        Handler(Looper.getMainLooper()).post {
            method()
        }
    }

}