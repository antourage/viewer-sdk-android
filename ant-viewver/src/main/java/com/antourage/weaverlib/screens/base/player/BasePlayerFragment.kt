package com.antourage.weaverlib.screens.base.player

import android.content.pm.ActivityInfo
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
import androidx.constraintlayout.widget.ConstraintLayout
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
import org.jetbrains.anko.sdk27.coroutines.onClick
import kotlin.math.abs

/**
 * Handles mostly orientation change andplayer controls
 */
internal abstract class BasePlayerFragment<VM : BasePlayerViewModel> : BaseFragment<VM>() {
    private lateinit var orientationEventListener: OrientationEventListener
    private var loader: AnimatedVectorDrawableCompat? = null
    private var isPortrait: Boolean? = null
    private var isLoaderShowing = false
    private var mCurrentOrientation: Int? = null

    companion object {
        const val MIN_TIM_BAR_UPDATE_INTERVAL_MS = 16
    }

    private lateinit var ivLoader: ImageView
    private lateinit var constraintLayoutParent: ConstraintLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivScreenSize: ImageView
    private lateinit var ivClose: ImageView
    protected lateinit var playerControls: PlayerControlView
    protected lateinit var playBtnPlaceholder: View
    private lateinit var controllerHeaderLayout: ConstraintLayout

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            enableOrientationChangeListenerIfAutoRotationEnabled()
        }
    }

    private val errorObserver =
        Observer<String> { errorMessage -> errorMessage?.let { showWarningAlerter(it) } }

    abstract fun onControlsVisible()

    abstract fun subscribeToObservers()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
        viewModel.errorLiveData.observe(viewLifecycleOwner, errorObserver)
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun initUi(view: View?) {
        view?.run {
            ivLoader = findViewById(R.id.ivLoader)
            playerView = findViewById(R.id.playerView)
            playerView.setShutterBackgroundColor(Color.TRANSPARENT)
            constraintLayoutParent = findViewById(R.id.constraintLayoutParent)
            ivScreenSize = findViewById(R.id.ivScreenSize)
            playerControls = findViewById(R.id.controls)
            playerControls.setTimeBarMinUpdateInterval(MIN_TIM_BAR_UPDATE_INTERVAL_MS)
            playBtnPlaceholder = findViewById(R.id.playBtnPlaceholder)
            controllerHeaderLayout = findViewById(R.id.controllerHeaderLayout)
            ivClose = findViewById(R.id.ivClose)

            controllerHeaderLayout.visibility = View.GONE

            initLoader()
            initOrientationHandling()
            setPlayerSizePortrait()

            ivClose.setOnClickListener { onCloseClicked() }
            ivScreenSize.setOnClickListener { onFullScreenImgClicked() }
            if (!Global.networkAvailable) {
                playBtnPlaceholder.visibility = View.VISIBLE
            } else {
                playBtnPlaceholder.visibility = View.GONE
            }
            playBtnPlaceholder.onClick {
                if (!ConnectionStateMonitor.isNetworkAvailable(context) && playerControls.isVisible) {
                    showWarningAlerter(context.resources.getString(R.string.ant_no_internet))
                }
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
        playerControls.hide()
        if (isLoaderShowing)
            initLoader()
    }

    protected fun handleControlsVisibility() {
        if (playerControls.isVisible)
            playerControls.hide()
        else {
            onControlsVisible()
            playerControls.show()
        }
    }

    protected fun showLoading() {
        loader?.apply {
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
        loader?.apply {
            if (ivLoader.visibility == View.VISIBLE && isRunning) {
                ivLoader.visibility = View.INVISIBLE
                clearAnimationCallbacks()
                stop()
                isLoaderShowing = false
            }
        }
    }

    private fun initOrientationHandling() {
        //check HR-92. It is still not fixed
        activity?.contentResolver?.registerContentObserver(
            Settings.System.getUriFor
                (Settings.System.ACCELEROMETER_ROTATION),
            true, contentObserver
        )

        mCurrentOrientation = getScreenOrientation()
        orientationEventListener =
            object : OrientationEventListener(this.context, SensorManager.SENSOR_DELAY_NORMAL) {
                override fun onOrientationChanged(orientation: Int) {
                    var nextOrientation = 0
                    if (orientation < 0) return

                    if (orientation in 315..359 || orientation < 45) {
                        nextOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else if (orientation in 45..134) {
                        nextOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    } else if (orientation in 135..224) {
                        nextOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    } else if (orientation in 225..314) {
                        nextOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }

                    if (mCurrentOrientation != nextOrientation) {
                        if (nextOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//                            if (mVrVideoView.isFullscreen())
//                                mVrVideoView.toggleFullscreen(false)

                            setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                        }
                        if (nextOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
//                            if (!mVrVideoView.isFullscreen())
//                                mVrVideoView.toggleFullscreen(false)

                            setOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                        }
                        if (nextOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
//                            if (mVrVideoView.isFullscreen())
//                                mVrVideoView.toggleFullscreen(false)

                            setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
                        }
                        if (nextOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
//                            if (!mVrVideoView.isFullscreen())
//                                mVrVideoView.toggleFullscreen(false)

                            setOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                        }
                        mCurrentOrientation = nextOrientation
                    }
                }
            }

        enableOrientationChangeListenerIfAutoRotationEnabled()
    }

    private fun isRotationOn(): Boolean {
        activity?.contentResolver?.apply {
            return Settings.System.getInt(
                activity?.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            ) == 1
        }
        return false
    }

    private fun onFullScreenImgClicked() {
        enableOrientationChangeListenerIfAutoRotationEnabled()
        val currentOrientation = activity?.resources?.configuration?.orientation
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = false
            if (orientation() != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        } else {
            isPortrait = true
            if (orientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    private fun initLoader() {
        loader = context?.let {
            AnimatedVectorDrawableCompat.create(
                it,
                R.drawable.antourage_loader_logo
            )
        }
        ivLoader.setImageDrawable(loader)
        showLoading()
    }

    private fun setOrientation(orientation: Int) {
        activity?.requestedOrientation = orientation
    }

    protected fun onCloseClicked() {
        fragmentManager?.let { fragmentManager ->
            if (fragmentManager.backStackEntryCount > 0) {
                fragmentManager.popBackStack()
            } else {
                replaceFragment(VideoListFragment.newInstance(), R.id.mainContent)
            }
        }
    }

    //TODO 31/07/2019 get rid of this method and use ConstraintLayout aspect ratio
    private fun setPlayerSizePortrait() {
        val params = playerView.layoutParams
        params.height = activity?.let { calculatePlayerHeight(it).toInt() } ?: 0
        playerView.layoutParams = params
    }

    private fun orientation() = resources.configuration.orientation

    private fun enableOrientationChangeListenerIfAutoRotationEnabled() {
        if (isRotationOn()) {
            orientationEventListener.enable()
        } else {
            orientationEventListener.disable()
        }
    }

    private fun getScreenOrientation(): Int {
        val rotation = activity?.windowManager?.defaultDisplay?.rotation
        val dm = DisplayMetrics()
        activity?.windowManager?.defaultDisplay?.getMetrics(dm)
        val width = dm.widthPixels
        val height = dm.heightPixels
        val orientation: Int
        // if the device's natural orientation is portrait:
        if ((rotation == ROTATION_0 || rotation == ROTATION_180) && height > width || (rotation == ROTATION_90 || rotation == ROTATION_270) && width > height) {
            orientation = when (rotation) {
                ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        } else {
            orientation = when (rotation) {
                ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                else -> {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }
        }// if the device's natural orientation is landscape or if the device
        // is square:

        return orientation
    }
}
