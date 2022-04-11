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
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getColor
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
import com.antourage.weaverlib.other.generateConfig
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
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.textColor
import java.util.*
import androidx.annotation.ColorInt
import com.antourage.weaverlib.other.networking.SocketConnector.socketConnection

@Keep
class AntourageFab @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), LifecycleEventObserver {

    private lateinit var currentLifecycle: Lifecycle

    @ColorInt
    private var webPortalColor: Int? = null
    private var isConfigOverridden: Boolean = false
    private var shouldShowBadge: Boolean = false
    private var badgeVisible: Boolean = false
    private var fallbackUrl: String? = null

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
    private val pulseAnimation: Animation = AlphaAnimation(0.0f, 1.0f)

    init {
        View.inflate(context, R.layout.antourage_fab_layout, this)

        this.z = 1000f
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AntourageFab,
            0, 0
        ).apply {

            try {
                portalColor = getColor(
                    R.styleable.AntourageFab_portalColor,
                    getColor(context, R.color.ant_colorWidgetBorder)
                )
                ctaTextColor = getColor(
                    R.styleable.AntourageFab_ctaTextColor,
                    getColor(context, R.color.ant_white)
                )
                ctaBackgroundColor = getColor(
                    R.styleable.AntourageFab_ctaBackgroundColor,
                    getColor(context, R.color.ant_colorCtaBg)
                )
                liveDotColor = getColor(
                    R.styleable.AntourageFab_liveDotColor,
                    getColor(context, R.color.ant_colorLive)
                )
                titleTextColor = getColor(
                    R.styleable.AntourageFab_titleTextColor,
                    getColor(context, R.color.ant_colorTitleText)
                )
                titleBackgroundColor = getColor(
                    R.styleable.AntourageFab_titleBackgroundColor,
                    getColor(context, R.color.ant_white)
                )
                nameTextColor = getColor(
                    R.styleable.AntourageFab_nameTextColor,
                    getColor(context, R.color.ant_white)
                )
                nameBackgroundColor = getColor(
                    R.styleable.AntourageFab_nameBackgroundColor,
                    getColor(context, R.color.ant_colorNameBg)
                )
            } finally {
                recycle()
            }
        }

        dummyClickableView.onClick {
            portalClicked()
        }
        bigDummyClickableView.onClick {
            portalClicked()
        }
        btnEnterPortal?.onClick {
            portalClicked()
        }

        initPulseAnimation()

        clearStreams()
    }

    @ColorInt
    var portalColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_colorWidgetBorder)) {
                isConfigOverridden = true
            }
            changePortalColor(value)
        }

    private fun changePortalColor(value: Int) {
        webPortalColor = value
        DrawableCompat.setTint(ivPortal.drawable, value)
        postInvalidate()
    }

    @ColorInt
    var ctaBackgroundColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_colorCtaBg)) {
                isConfigOverridden = true
            }
            changeCtaBackgroundColor(value)
        }

    private fun changeCtaBackgroundColor(value: Int) {
        btnEnterPortal.background.setTint(value)
        postInvalidate()
    }

    @ColorInt
    var ctaTextColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_white)) {
                isConfigOverridden = true
            }
            changeCtaTextColor(value)
        }

    private fun changeCtaTextColor(value: Int) {
        btnCta.textColor = value
        ctaArrow.drawable.setTint(value)
        postInvalidate()
    }

    @ColorInt
    var liveDotColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_colorLive)) {
                isConfigOverridden = true
            }
            changeLiveDotColor(value)
        }

    private fun changeLiveDotColor(value: Int) {
        dotView.background.setTint(value)
        liveDotView.background.setTint(value)
        postInvalidate()
    }

    @ColorInt
    var titleTextColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_colorTitleText)) {
                isConfigOverridden = true
            }
            changeTitleTextColor(value)
        }

    private fun changeTitleTextColor(value: Int) {
        tvBadge.setTextColor(value)
        tvLiveFirstLine.setTextColor(value)
        tvLiveSecondLine?.setTextColor(value)
        postInvalidate()
    }

    @ColorInt
    var titleBackgroundColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_white)) {
                isConfigOverridden = true
            }
            changeTitleBackgroundColor(value)
        }

    private fun changeTitleBackgroundColor(value: Int) {
        badgeView?.background?.setTint(value)
        tvLiveSecondLine?.background?.setTint(value)
        firstLineContainer.background.setTint(value)
        postInvalidate()
    }

    @ColorInt
    var nameTextColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_white)) {
                isConfigOverridden = true
            }
            changeNameTextColor(value)
        }

    private fun changeNameTextColor(value: Int) {
        tvStreamerName.setTextColor(value)
        postInvalidate()
    }

    @ColorInt
    var nameBackgroundColor: Int
        set(value) {
            field = value
            if (field != getColor(context, R.color.ant_colorNameBg)) {
                isConfigOverridden = true
            }
            changeNameBackgroundColor(value)
        }

    private fun changeNameBackgroundColor(value: Int) {
        tvStreamerName.background.setTint(value)
        postInvalidate()
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onResume()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onPause()
    }

    companion object {
        internal val shownLiveIds = mutableSetOf<Int>()
        internal var teamId: Int = -1
        internal var wasAlreadyExpanded: Boolean = false
        internal var cachedFcmToken: String = ""
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
        fun configure(context: Context, teamId: Int) {
            UserCache.getInstance(context)
            ConfigManager.init(context)
            this.teamId = ConfigManager.TEAM_ID ?: teamId
            handleDeviceId(context)
            setDefaultLocale(context)
            if (!isSubscribedToPushes) retryRegisterNotifications()
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
            if (fcmToken.isNullOrEmpty() || teamId == -1) return
            cachedFcmToken = fcmToken
            val response =
                PushRepository.subscribeToPushNotifications(
                    SubscribeToPushesRequest(
                        fcmToken,
                        teamId
                    )
                )
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
        if (badgeVisible) {
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
        currentLifecycle = lifecycle
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

    private val portalStateObserver = object : PortalStateManager.PortalStateCallback {
        override fun onPortalStateReceived(state: PortalStateResponse) {
            handlePortalState(state)
            SocketConnector.portalStateLD.observeForever(stateFromSocketsObserver)
            SocketConnector.connectToSockets()
        }
    }

    private val socketConnectionObserver = Observer<SocketConnector.SocketConnection> {
        if (it == SocketConnector.SocketConnection.CONNECTED && SocketConnector.shouldCallApiRequest) {
            PortalStateManager.fetchPortalState(true)
        }
    }

    private val streamPreviewObserver = object : StreamPreviewManager.StreamCallback {
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
    }

    fun onResume() {
        if (!wasPaused || parent == null) return
        wasPaused = false
        expandInProgress = false
        badgeVisible = false
        shouldShowBadge = false
        setNextWidgetState(WidgetState.INACTIVE)
        forceHideBadge()
        setLocale()
        socketConnection.observeForever(socketConnectionObserver)
        internetStateLiveData.observeForever(networkStateObserver)
        PortalStateManager.setReceivedCallback(portalStateObserver)
        StreamPreviewManager.setCallback(streamPreviewObserver)
        PortalStateManager.fetchPortalState()
    }

    fun onPause() {
        wasPaused = true
        shouldShowBadge = false
        badgeVisible = false
        clearStreams()

//TODO initizalided


        SocketConnector.disconnectSocket()
        PortalStateManager.onPause()
        isAnimationRunning = false
        currentWidgetState = WidgetState.INITIAL
        setNextWidgetState(null)
        forceHideBadge()
        SocketConnector.portalStateLD.removeObserver(stateFromSocketsObserver)
        socketConnection.removeObserver(socketConnectionObserver)
        handlerHideViews.removeCallbacksAndMessages(null)
        handlerRevealViews.removeCallbacksAndMessages(null)
        Handler(Looper.getMainLooper()).postDelayed({
            releasePlayer(false)
            labelsView.hideView(false)
            portalAnimatedDrawable?.apply {
                clearAnimationCallbacks()
            }
            portalAnimatedDrawable?.stop()
        }, 100)
    }

    private fun handlePortalState(state: PortalStateResponse) {
        fallbackUrl = state.fallbackUrl
        setColorsFromConfig(state)
        if (currentPortalState?.contentId == state.item?.contentId && currentPortalState?.live == state.item?.live) return
        if (state.item?.contentId == null) {
            shouldShowBadge = false
            hideBadge()
            return
        }
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
        currentPortalState = data
        showBadge(context.getString(R.string.ant_live))
        data.contentId?.let { id ->
            if (shownLiveIds.none { it == id }) {
                expandInProgress = true
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


    private fun initPreVodState(data: PortalState) {
        var seen = true
        GlobalScope.launch(Dispatchers.IO) {
            data.contentId?.let {
                seen = RoomRepository.getInstance(context).isAlreadySeen(it)
            }
            launch(Dispatchers.Main) {
                currentPortalState = data
                if (!seen) {
                    showBadge(context.getString(R.string.ant_new_vod))
                } else {
                    hideBadge()
                }
                if (!wasAlreadyExpanded) {
                    expandInProgress = true
                    playerView.player =
                        data.assetUrl?.let {
                            StreamPreviewManager.startPlayingStream(
                                it,
                                context,
                                data.curtainMilliseconds
                            )
                        }
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
                }, 100)
                handlerHideViews.postDelayed({
                    labelsView.hideView()
                    bigDummyClickableView.visibility = View.GONE
                }, 5500)
                startAnimation(state)
            }
            WidgetState.NEW -> {
                showPlayer()
                handlerRevealViews.postDelayed({
                    hideBadge()
                    labelsView.revealView(currentPortalState)
                    bigDummyClickableView.visibility = View.VISIBLE
                }, 100)
                handlerHideViews.postDelayed({
                    labelsView.hideView()
                    bigDummyClickableView.visibility = View.GONE
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
            webPortalColor?.let { DrawableCompat.setTint(ivPortal.drawable, it) }
            ivPortal.background = null
        }
    }

    private fun setColorsFromConfig(state: PortalStateResponse) {
        if (isConfigOverridden) return
        val config = state.config?.let { generateConfig(context, it) }
        config?.run {
            changePortalColor(colorWidgetBorder)
            changeCtaTextColor(colorCtaText)
            changeCtaBackgroundColor(colorCtaBg)
            changeLiveDotColor(colorLive)
            changeTitleTextColor(colorTitleText)
            changeTitleBackgroundColor(colorTitleBg)
            changeNameTextColor(colorNameText)
            changeNameBackgroundColor(colorNameBg)
        }
    }

    private fun showPlayer() {
        wasAlreadyExpanded = true
        currentPortalState?.contentId?.let {
            if (currentPortalState?.live == true) shownLiveIds.add(
                it
            )
        }
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
        //to prevent quick multiple taps
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return
        }
        mLastClickTime = SystemClock.elapsedRealtime()
        if (currentPortalState?.live == false) {
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
            currentLifecycle.removeObserver(this)
            onPause()
        }
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