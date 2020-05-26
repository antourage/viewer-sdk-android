package com.antourage.weaverlib.screens.base.player

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.OrientationEventListener
import android.view.Surface.*
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.calculatePlayerHeight
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.jetbrains.anko.backgroundColor

/**
 * handles Loader, errors, screen rotation and player controls visibility;
 */
internal abstract class BasePlayerFragment<VM : BasePlayerViewModel> : BaseFragment<VM>() {

    companion object {
        /**
         * this value is set in order to update video timeline frequently to avoid
         * jumping timeline in case when duration of the video is relatively small
         */
        const val MIN_TIME_BAR_UPDATE_INTERVAL_MS = 16
    }

    private lateinit var orientationEventListener: OrientationEventListener
    private var loaderAnimatedDrawable: AnimatedVectorDrawableCompat? = null
    private var isLoaderShowing = false
    private var currentOrientation: Int? = null

    private var snackBarBehaviour: BottomSheetBehavior<View>? = null
    private var errorSnackBar: TextView? =null
    private var snackBarLayout: CoordinatorLayout? =null
    private lateinit var ivLoader: ImageView
    private lateinit var constraintLayoutParent: ConstraintLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivScreenSize: ImageView
    private lateinit var ivClose: ImageView
    protected lateinit var playerControls: PlayerControlView
    private lateinit var playBtnPlaceholder: View
    private lateinit var controllerHeaderLayout: ConstraintLayout
    private var minuteUpdateReceiver: BroadcastReceiver? = null

    /**
     * we need to know if user turns on screen auto rotation or locks
     * it to portrait manually in the android expanding quick settings menu
     */
    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            enableOrientationChangeListenerIfAutoRotationEnabled()
        }
    }

    private val errorObserver = Observer<Boolean> { errorMessage ->
        errorMessage?.let { showErrorSnackBar(getString(R.string.ant_server_error)) } }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            resolveErrorSnackBar(R.string.ant_you_are_online)
            showLoading()
            viewModel.onNetworkGained()
            playBtnPlaceholder.visibility = View.INVISIBLE
        } else if (networkState?.ordinal == NetworkConnectionState.LOST.ordinal) {
            if (!Global.networkAvailable) {
                showErrorSnackBar(getString(R.string.ant_no_connection), false)
            }
        }
    }

    abstract fun onControlsVisible()
    abstract fun onMinuteChanged()

    abstract fun subscribeToObservers()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
        viewModel.errorLiveData.observe(viewLifecycleOwner, errorObserver)
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startMinuteUpdater()
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().unregisterReceiver(minuteUpdateReceiver)
    }

    private fun startMinuteUpdater() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        minuteUpdateReceiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) { onMinuteChanged() }
        }
        requireActivity().registerReceiver(minuteUpdateReceiver, intentFilter)
    }


    override fun initUi(view: View?) {
        view?.run {
            ivLoader = findViewById(R.id.ivLoader)
            playerView = findViewById(R.id.playerView)
            playerView.setShutterBackgroundColor(Color.TRANSPARENT)
            constraintLayoutParent = findViewById(R.id.constraintLayoutParent)

            snackBarLayout = findViewById(R.id.bottom_coordinator)
            errorSnackBar = findViewById(R.id.player_snack_bar)
            initSnackBar()

            ivScreenSize = findViewById(R.id.ivScreenSize)
            ivScreenSize.setOnClickListener { onFullScreenImgClicked() }

            playerControls = findViewById(R.id.controls)
            playerControls.setTimeBarMinUpdateInterval(MIN_TIME_BAR_UPDATE_INTERVAL_MS)
            playBtnPlaceholder = findViewById(R.id.playBtnPlaceholder)
            controllerHeaderLayout = findViewById(R.id.player_control_header)
            //sets white tint for close button in player header
            controllerHeaderLayout
                .findViewById<ImageView>(R.id.play_header_iv_close).isActivated = true

            //for portrait only
            ivClose = findViewById(R.id.play_header_iv_close)
            ivClose.setOnClickListener { onCloseClicked() }

            controllerHeaderLayout.visibility = View.GONE

            initAndShowLoader()
            initOrientationHandling()
            setPlayerSizePortrait()

            if (!Global.networkAvailable) {
                playBtnPlaceholder.visibility = View.VISIBLE
            } else {
                playBtnPlaceholder.visibility = View.INVISIBLE
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                constraintLayoutParent.setState(
                    R.id.constraintStateLandscape,
                    newConfig.screenWidthDp,
                    newConfig.screenHeightDp
                )
                context?.let { context ->
                    ivScreenSize.background =
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.antourage_ic_fullscreen_exit
                        )
                }
                controllerHeaderLayout.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed(
                    { activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN) },
                    100
                )
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                constraintLayoutParent.setState(
                    R.id.constraintStatePortrait,
                    newConfig.screenWidthDp,
                    newConfig.screenHeightDp
                )
                context?.let { context ->
                    ivScreenSize.background =
                        ContextCompat.getDrawable(context, R.drawable.antourage_ic_full_screen)
                }
                controllerHeaderLayout.visibility = View.GONE
                setPlayerSizePortrait()
                activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
        }
        if (!Global.networkAvailable) {
            showErrorSnackBar(getString(R.string.ant_no_connection), false)
        }
        playerControls.hide()
        if (isLoaderShowing)
            initAndShowLoader()
    }

    protected fun toggleControlsVisibility() {
        if (playerControls.isVisible)
            playerControls.hide()
        else {
            onControlsVisible()
            playerControls.show()
        }
    }

    private fun initSnackBar() {
        errorSnackBar.let { snackBar ->
            snackBarBehaviour = BottomSheetBehavior.from(snackBar as View)
            snackBarBehaviour?.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback(){
                override fun onSlide(bottomSheet: View, slideOffset: Float) {}

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                        //to disable user swipe
                        snackBarBehaviour?.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            })
        }
    }

    override fun showNoInternetMessage() {
        super.showNoInternetMessage()
        showErrorSnackBar(getString(R.string.ant_no_connection), false)
    }

    fun showErrorSnackBar(message: String, isAutoCloseable: Boolean = true) {
        errorSnackBar?.text = message
        snackBarLayout?.visibility = View.VISIBLE
        if (snackBarBehaviour?.state != BottomSheetBehavior.STATE_EXPANDED) {
            context?.let {
                errorSnackBar?.backgroundColor =
                    ContextCompat.getColor(it, R.color.ant_error_bg_color)
            }
            snackBarBehaviour?.state = BottomSheetBehavior.STATE_EXPANDED
        }
        if (isAutoCloseable){
            Handler().postDelayed({ hideErrorSnackBar() }, 2000)
        }
    }

    fun hideErrorSnackBar(){
        snackBarBehaviour?.state = BottomSheetBehavior.STATE_COLLAPSED
        snackBarLayout?.visibility = View.INVISIBLE
    }

    private fun resolveErrorSnackBar(messageId: Int) {
        if (snackBarBehaviour?.state == BottomSheetBehavior.STATE_EXPANDED) {
            context?.resources?.getString(messageId)
                ?.let { messageToDisplay ->
                    errorSnackBar?.text = messageToDisplay
                    errorSnackBar?.let { snackBar ->
                        val colorFrom: Int =  ContextCompat.getColor(requireContext(), R.color.ant_error_bg_color)
                        val colorTo: Int =
                            ContextCompat.getColor(requireContext(), R.color.ant_error_resolved_bg_color)
                        val duration = 500L
                        ObjectAnimator.ofObject(snackBar, "backgroundColor",
                            ArgbEvaluator(), colorFrom, colorTo)
                            .setDuration(duration)
                            .start()
                    }
                }
            Handler().postDelayed({ hideErrorSnackBar() }, 2000)
        }
    }

    protected fun showLoading() {
        loaderAnimatedDrawable?.apply {
            if (!isRunning) {
                registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        ivLoader.post { start() }
                    }
                })
                start()
                isLoaderShowing = true
                ivLoader.visibility = View.VISIBLE
                playerControls.hide()
            }
        }
    }

    protected fun hideLoading() {
        loaderAnimatedDrawable?.apply {
            if (ivLoader.visibility == View.VISIBLE && isRunning) {
                ivLoader.visibility = View.INVISIBLE
                clearAnimationCallbacks()
                stop()
                isLoaderShowing = false
            }
        }
    }

    private fun initOrientationHandling() {
        activity?.contentResolver?.registerContentObserver(
            Settings.System.getUriFor
                (Settings.System.ACCELEROMETER_ROTATION),
            true, contentObserver
        )

        currentOrientation = getScreenOrientation()
        orientationEventListener =
            object : OrientationEventListener(this.context, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    if (orientation < 0) return

                    val nextOrientation = when {
                        orientation in 315..359 || orientation < 45 -> SCREEN_ORIENTATION_PORTRAIT
                        orientation in 45..134 -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                        orientation in 135..224 -> SCREEN_ORIENTATION_REVERSE_PORTRAIT
                        orientation in 225..314 -> SCREEN_ORIENTATION_LANDSCAPE
                        else -> SCREEN_ORIENTATION_LANDSCAPE
                    }

                    if (currentOrientation != nextOrientation) {
                        if (nextOrientation in listOf(
                                SCREEN_ORIENTATION_PORTRAIT,
                                SCREEN_ORIENTATION_LANDSCAPE,
                                SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                                SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            )
                        ) {
                            setOrientation(nextOrientation)
                        }
                        currentOrientation = nextOrientation
                    }
                }
            }

        enableOrientationChangeListenerIfAutoRotationEnabled()
    }

    private fun isAutoRotationEnabled(): Boolean {
        activity?.contentResolver?.apply {
            return Settings.System.getInt(
                this,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        }
        return false
    }

    private fun onFullScreenImgClicked() {
        enableOrientationChangeListenerIfAutoRotationEnabled()
        toggleScreenOrientation()
    }

    private fun toggleScreenOrientation() {
        setOrientation(
            if (orientation() == Configuration.ORIENTATION_PORTRAIT)
                SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            else SCREEN_ORIENTATION_PORTRAIT
        )
    }

    private fun orientation() = resources.configuration.orientation

    private fun setOrientation(orientation: Int) {
        activity?.requestedOrientation = orientation
    }

    private fun initAndShowLoader() {
        loaderAnimatedDrawable = context?.let {
            AnimatedVectorDrawableCompat.create(
                it,
                R.drawable.antourage_loader_logo
            )
        }
        ivLoader.setImageDrawable(loaderAnimatedDrawable)
        showLoading()
    }

    protected fun onCloseClicked() {
        parentFragmentManager.let { fragmentManager ->
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            } else {
                replaceFragment(VideoListFragment.newInstance(), R.id.mainContent)
            }
        }
    }

    private fun setPlayerSizePortrait() {
        val params = playerView.layoutParams
        params.height = activity?.let { calculatePlayerHeight(it).toInt() } ?: 0
        playerView.layoutParams = params
    }

    private fun enableOrientationChangeListenerIfAutoRotationEnabled() {
        if (isAutoRotationEnabled()) {
            orientationEventListener.enable()
        } else {
            orientationEventListener.disable()
        }
    }

    fun disableOrientationChange() = orientationEventListener.disable()

    private fun getScreenOrientation(): Int {
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        val width = dm.widthPixels
        val height = dm.heightPixels
        val orientation: Int
        if ((rotation == ROTATION_0 || rotation == ROTATION_180) && height > width || (rotation == ROTATION_90 || rotation == ROTATION_270) && width > height) {
            orientation = when (rotation) {
                ROTATION_0 -> SCREEN_ORIENTATION_PORTRAIT
                ROTATION_90 -> SCREEN_ORIENTATION_LANDSCAPE
                ROTATION_180 -> SCREEN_ORIENTATION_REVERSE_PORTRAIT
                ROTATION_270 -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> {
                    SCREEN_ORIENTATION_PORTRAIT
                }
            }
        } else {
            orientation = when (rotation) {
                ROTATION_0 -> SCREEN_ORIENTATION_LANDSCAPE
                ROTATION_90 -> SCREEN_ORIENTATION_PORTRAIT
                ROTATION_180 -> SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ROTATION_270 -> SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else -> {
                    SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }
        return orientation
    }
}
