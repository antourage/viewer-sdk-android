package com.antourage.weaverlib.ui.fab

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.ColorInt
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.ConfigManager
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.hideBadge
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor.Companion.internetStateLiveData
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.SocketConnector
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.networking.push.PushRepository
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.other.showBadge
import com.antourage.weaverlib.screens.list.PortalStateManager
import com.google.android.exoplayer2.Player
import com.google.android.material.internal.ContextUtils.getActivity
import kotlinx.android.synthetic.main.antourage_fab_layout.view.*
import kotlinx.android.synthetic.main.antourage_labels_layout.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor
import java.util.*

@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), LifecycleEventObserver {

    private var shouldShowBadge: Boolean = false
    private var badgeVisible: Boolean = false
    private var fallbackUrl: String? = null
    private var idleInterval: Int = 60

    @ColorInt
    private var portalColor: Int = 0
    private var backgroundView: WidgetDarkBackgroundView = WidgetDarkBackgroundView(context)

    private var currentPortalState: PortalState? = null
    private var nextPortalStateToShow: PortalStateResponse? = null

    private var currentAnimationDrawableId: Int = -1
    private var currentWidgetState: WidgetState = WidgetState.INITIAL
    private var expandInProgress = false
    private var nextWidgetState: WidgetState? = null

    private var isAnimationRunning = false
    private var portalAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var currentPlayerState: Int = 0

    private var handlerRevealViews: Handler = Handler(Looper.getMainLooper())
    private var handlerHideViews: Handler = Handler(Looper.getMainLooper())
    private var handlerIdle: Handler = Handler(Looper.getMainLooper())
    val pulseAnimation: Animation = AlphaAnimation(0.0f, 1.0f)


    init {
        View.inflate(context, R.layout.antourage_fab_layout, this)

        this.z = 1000f
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AntourageFab,
            0, 0
        ).apply {

            try {
                setPortalColor(getColor(R.styleable.AntourageFab_portalColor, 0))
                setCTATextColor(getColor(R.styleable.AntourageFab_ctaTextColor, 0))
                setCTABackgroundColor(getColor(R.styleable.AntourageFab_ctaBackgroundColor, 0))
                setLiveDotColor(getColor(R.styleable.AntourageFab_liveDotColor, 0))
                setTitleTextColor(getColor(R.styleable.AntourageFab_titleTextColor, 0))
                setTitleBackgroundColor(getColor(R.styleable.AntourageFab_titleBackgroundColor, 0))
                setNameTextColor(getColor(R.styleable.AntourageFab_nameTextColor, 0))
                setNameBackgroundColor(getColor(R.styleable.AntourageFab_nameBackgroundColor, 0))
                setIdleInterval(getInt(R.styleable.AntourageFab_nameBackgroundColor, 0))
                setOverlayAlpha(getFloat(R.styleable.AntourageFab_overlayAlpha, 0f))
                setOverlayColor(
                    getColor(
                        R.styleable.AntourageFab_overlayColor,
                        0
                    )
                )
            } finally {
                recycle()
            }
        }

        backgroundView.setOnClickListener {
            if (SystemClock.elapsedRealtime() - mLastBackgroundClickTime > 2000) {
                mLastClickTime = SystemClock.elapsedRealtime()
                bigDummyClickableView.visibility = View.GONE
                backgroundView.hideView()
                labelsView.hideView()
            }
        }

        dummyClickableView.onClick {
            portalClicked()
        }
        bigDummyClickableView.onClick {
            portalClicked()
        }
        labelsView.onClick {
            portalClicked()
        }

        initPulseAnimation()

        clearStreams()
    }

    private fun initPulseAnimation() {
        pulseAnimation.duration = 500
        pulseAnimation.repeatMode = Animation.REVERSE
        pulseAnimation.repeatCount = Animation.INFINITE
    }

    private fun clearStreams() {
        currentPortalState = null
        nextPortalStateToShow = null
    }

    companion object {
        internal var teamId: Int = -1
        internal var cachedFcmToken: String = ""
        internal var isSubscribedToPushes = false
        private var pushRegistrationCallback: ((result: RegisterPushNotificationsResult) -> Unit)? =
            null
        internal const val TAG = "AntourageFabLogs"
        internal var mLastClickTime: Long = 0
        internal var mLastBackgroundClickTime: Long = 0
        const val AntourageSenderId = "1090288296965"

        /** added to prevent multiple calls of onResume breaking widget logic*/
        internal var wasPaused = true

        /**
         *  Method for configuring fab that initializes all needed library instances
         *  */
        fun configure(context: Context, teamId: Int) {
            this.teamId = teamId
            UserCache.getInstance(context)
            ConfigManager.init(context)
            handleDeviceId(context)
            setDefaultLocale(context)
            if (!isSubscribedToPushes) retryRegisterNotifications()
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
                    registerNotifications(cachedFcmToken, teamId, pushRegistrationCallback)
                }
            }
        }

        fun registerNotifications(
            fcmToken: String?,
            teamId: Int,
            callback: ((result: RegisterPushNotificationsResult) -> Unit)? = null
        ) {
            Log.d(TAG, "Trying to register ant push notifications...")
            pushRegistrationCallback = callback
            if (fcmToken.isNullOrEmpty()) return
            cachedFcmToken = fcmToken
            val response =
                PushRepository.subscribeToPushNotifications(SubscribeToPushesRequest(fcmToken, teamId))
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

    private fun showBadge(text: String?) {
        shouldShowBadge = true
        text?.let { tvBadge?.text = it }
        if (tvBadge?.text.toString() == context.getString(R.string.ant_live)) {
            liveDotView.startAnimation(pulseAnimation)
            liveDotView?.visibility = View.VISIBLE
        } else {
            liveDotView?.visibility = View.GONE
            liveDotView.clearAnimation()
        }
        if (!badgeVisible) {
            badgeVisible = true
            badgeView?.showBadge(measureAndLayout)
        }
    }

    private fun hideBadge() {
        if(badgeVisible){
            liveDotView.clearAnimation()
            liveDotView?.visibility = View.GONE
            badgeVisible = false
            badgeView.hideBadge(measureAndLayout)
        }
    }

    /**method for React Native for force hide badge on widget appearing*/
    fun forceHideBadge() {
        badgeView?.alpha = 0f
        tvBadge?.text = ""
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

    private val stateFromSocketsObserver =
        Observer<PortalStateResponse> { state ->
            state?.let {
                handlePortalState(it)
            }
        }

    private fun onResume() {
        if (!wasPaused) return
        wasPaused = false
        expandInProgress = false
        badgeVisible = false
        shouldShowBadge = false
        setNextWidgetState(WidgetState.INACTIVE)
        forceHideBadge()
        setLocale()
        internetStateLiveData.observeForever(networkStateObserver)

        PortalStateManager.setReceivedCallback(object :
            PortalStateManager.PortalStateCallback {
            override fun onPortalStateReceived(state: PortalStateResponse) {
                handlePortalState(state)
                SocketConnector.portalStateLD.observeForever(stateFromSocketsObserver)
                SocketConnector.connectToSockets()
            }
        })

        StreamPreviewManager.setCallback(object : StreamPreviewManager.StreamCallback {
            override fun onNewState(playbackState: Int) {
                super.onNewState(playbackState)
                currentPlayerState = playbackState
                if (playbackState == Player.STATE_READY) {
                    if (currentPortalState?.live == true) {
                        setNextWidgetState(WidgetState.LIVE)
                    } else {
                        setNextWidgetState(WidgetState.NEW)
                    }
                }
            }

            override fun onError() {
                super.onError()
                setNextWidgetState(WidgetState.INACTIVE)
            }
        })

        PortalStateManager.fetchPortalState()
    }

    private fun onPause() {
        wasPaused = true
        shouldShowBadge = false
        badgeVisible = false
        clearStreams()
        SocketConnector.disconnectSocket()
        PortalStateManager.onPause()
        isAnimationRunning = false
        currentWidgetState = WidgetState.INITIAL
        setNextWidgetState(null)
        forceHideBadge()
        SocketConnector.portalStateLD.removeObserver(stateFromSocketsObserver)
        handlerHideViews.removeCallbacksAndMessages(null)
        handlerRevealViews.removeCallbacksAndMessages(null)
        Handler(Looper.getMainLooper()).postDelayed({
            releasePlayer(false)
            backgroundView.hideView()
            labelsView.hideView(false)
            portalAnimatedDrawable?.apply {
                clearAnimationCallbacks()
            }
            portalAnimatedDrawable?.stop()
        }, 100)
    }


    private fun handlePortalState(state: PortalStateResponse) {
        fallbackUrl = state.fallbackUrl
        if(currentPortalState?.contentId == state.item?.contentId && currentPortalState?.live == state.item?.live) return
        nextPortalStateToShow =
            if (!expandInProgress) {
                state.item?.live?.let {
                    if (it) {
                        initPreLiveState(state.item!!)
                    } else {
                        initPreVodState(state.item!!)
                    }
                }
                null
            } else {
                state
            }
    }

    private fun initPreLiveState(data: PortalState) {
        expandInProgress = true
        currentPortalState = data
        showBadge(context.getString(R.string.ant_live))
        playerView.player =
            data.assetUrl?.let {
                StreamPreviewManager.startPlayingStream(
                    it,
                    context
                )
            }
    }


    private fun initPreVodState(data: PortalState) {
        var seen = true
        GlobalScope.launch(Dispatchers.IO) {
            data.contentId?.let {
                seen = RoomRepository.getInstance(context).isAlreadySeen(it)
            }
            launch(Dispatchers.Main) {
                expandInProgress = true
                currentPortalState = data
                if (!seen) {
                    showBadge(context.getString(R.string.ant_new_vod))
                }else{
                    hideBadge()
                }
                playerView.player =
                    data.assetUrl?.let {
                        StreamPreviewManager.startPlayingStream(
                            it,
                            context
                        )
                    }
            }
        }
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
        if (state != WidgetState.LIVE && state != WidgetState.NEW) {
            bigDummyClickableView.visibility = View.GONE
            releasePlayer(true)
        }
        when (state) {
            WidgetState.INACTIVE -> {
                currentPortalState = null
                hideBadge()
                bigDummyClickableView.visibility = View.GONE
                portalAnimatedDrawable?.clearAnimationCallbacks()
                startAnimation(state)
            }
            WidgetState.LIVE -> {
                showPlayer()
                handlerRevealViews.postDelayed({
                    hideBadge()
                    labelsView.revealView(currentPortalState)
                    bigDummyClickableView.visibility = View.VISIBLE
                    darkenBackground()
                }, 100)
                handlerHideViews.postDelayed({
                    labelsView.hideView()
                    bigDummyClickableView.visibility = View.GONE
                    backgroundView.hideView()
                }, 5500)
                startAnimation(state)
            }
            WidgetState.NEW -> {
                showPlayer()
                handlerRevealViews.postDelayed({
                    hideBadge()
                    labelsView.revealView(currentPortalState)
                    bigDummyClickableView.visibility = View.VISIBLE
                    darkenBackground()
                }, 100)
                handlerHideViews.postDelayed({
                    labelsView.hideView()
                    bigDummyClickableView.visibility = View.GONE
                    backgroundView.hideView()
                }, 5500)
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
                    if (shouldShowBadge) showBadge(null)
                    isAnimationRunning = false
                    if (currentAnimationDrawableId == WidgetState.LIVE.animationDrawableId) {
                        expandInProgress = false
                        nextPortalStateToShow?.let {
                            it.item?.live?.let { live ->
                                if (live) {
                                    initPreLiveState(it.item!!)
                                    nextPortalStateToShow = null
                                    startAnimation(WidgetState.INACTIVE)
                                    return
                                } else {
                                    initPreVodState(it.item!!)
                                    nextPortalStateToShow = null
                                    startAnimation(WidgetState.INACTIVE)
                                    return
                                }
                            }
                        }

                        if (nextWidgetState != null) {
                            changeWidgetState(nextWidgetState!!)
                            nextWidgetState = null
                            return
                        }
                    } else {
                        if (nextWidgetState != null) {
                            changeWidgetState(nextWidgetState!!)
                            nextWidgetState = null
                            return
                        }
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
            setPortalColor(portalColor)
            ivPortal.background = null
        }
    }


    fun setPortalColor(@ColorInt color: Int) {
        if (color != 0) {
            portalColor = color
            DrawableCompat.setTint(ivPortal.drawable, portalColor)
        }
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
        if (color != 0) {
            dotView.background.setTint(color)
            liveDotView.background.setTint(color)
        }
        postInvalidate()
    }

    fun setTitleTextColor(@ColorInt color: Int) {
        if (color != 0) {
            tvBadge.setTextColor(color)
            tvLiveFirstLine.setTextColor(color)
            tvLiveSecondLine?.setTextColor(color)
        }
        postInvalidate()
    }

    fun setTitleBackgroundColor(@ColorInt color: Int) {
        if (color != 0) {
            badgeView?.background?.setTint(color)
            tvLiveSecondLine?.background?.setTint(color)
            firstLineContainer.background.setTint(color)
        }
        postInvalidate()
    }

    fun setNameTextColor(@ColorInt color: Int) {
        if (color != 0) {
            tvStreamerName.setTextColor(color)
        }
        postInvalidate()
    }

    fun setNameBackgroundColor(@ColorInt color: Int) {
        if (color != 0) {
            tvStreamerName.background.setTint(color)
        }
        postInvalidate()
    }

    fun setIdleInterval(seconds: Int) {
        if (seconds != 0) {
            idleInterval = seconds
        }
    }

    fun setOverlayColor(@ColorInt color: Int) {
        if (color != 0) {
            backgroundView.setOverlayColor(color)
        }
    }

    fun setOverlayAlpha(alpha: Float) {
        if (alpha != 0f) {
            backgroundView.setOverlayAlpha(alpha)
        }
    }

    private fun showPlayer() {
        playerView?.alpha = 0f
        playerView?.visibility = View.VISIBLE
        playerView?.player?.playWhenReady = true
        playerView?.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 150f)
            }
        }

        playerView?.clipToOutline = true
        playerView?.animate()?.alpha(1f)?.setStartDelay(100)?.setDuration(1000)
            ?.withEndAction {
                playerView?.animate()?.alpha(0f)?.setStartDelay(4500)?.setDuration(1000)
                    ?.start()
            }?.start()
    }

    private fun releasePlayer(shouldAnimate: Boolean = false) {
        clearStreams()
        StreamPreviewManager.releasePlayer()
        if (shouldAnimate) {
            if (playerView?.visibility == View.VISIBLE) {
                playerView?.animate()?.alpha(0f)?.setDuration(300)?.start()
            }
        } else {
            playerView?.visibility = View.GONE
        }
    }

    private fun portalClicked() {
        backgroundView.hideView()
        //to prevent quick multiple taps
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        if(currentPortalState?.live == false){
            currentPortalState?.contentId?.let {
                GlobalScope.launch(Dispatchers.IO) {
                    RoomRepository.getInstance(context).addToSeen(Video(it))
                }
            }
        }
        openFullPageActivity(currentPortalState?.ctaUrl ?: fallbackUrl)
    }

    private fun openFullPageActivity(url: String?) {
        url?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
        }
    }

    private val networkStateObserver: Observer<NetworkConnectionState?> =
        Observer { networkState ->
            networkState?.let {
                when (networkState.ordinal) {
                    NetworkConnectionState.LOST.ordinal -> {
                        if (!Global.networkAvailable) {
                            PortalStateManager.networkLost()
                            SocketConnector.disconnectSocket()
                            setNextWidgetState(WidgetState.INACTIVE)
                        }
                    }
                    NetworkConnectionState.AVAILABLE.ordinal -> {
                        PortalStateManager.fetchPortalState()
                    }
                }
            }
        }

    internal enum class WidgetState(val animationDrawableId: Int) {
        INITIAL(R.drawable.antourage_portal_small),
        INACTIVE(R.drawable.antourage_portal_small),
        LIVE(R.drawable.anourage_portal_expanded),
        NEW(R.drawable.anourage_portal_expanded),
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
        backgroundView.z = 500f
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