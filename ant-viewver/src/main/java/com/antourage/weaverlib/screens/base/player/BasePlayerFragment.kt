package com.antourage.weaverlib.screens.base.player

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.calculatePlayerHeight
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView

/**
 * Handles mostly orientation change andplayer controls
 */
internal abstract class BasePlayerFragment<VM : BasePlayerViewModel> : BaseFragment<VM>() {
    private lateinit var orientationEventListener: OrientationEventListener
    private var loader: AnimatedVectorDrawableCompat? = null
    private var isPortrait: Boolean? = null
    private var isLoaderShowing = false

    companion object {
        const val MIN_TIM_BAR_UPDATE_INTERVAL_MS = 16
    }

    private lateinit var ivLoader: ImageView
    private lateinit var constraintLayoutParent: ConstraintLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivScreenSize: ImageView
    private lateinit var ivClose: ImageView
    protected lateinit var playerControls: PlayerControlView
    private lateinit var controllerHeaderLayout: ConstraintLayout

    private val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            orientationEventListener.enable()
        }
    }

    abstract fun onControlsVisible()

    abstract fun subscribeToObservers()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
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
            constraintLayoutParent = findViewById(R.id.constraintLayoutParent)
            ivScreenSize = findViewById(R.id.ivScreenSize)
            playerControls = findViewById(R.id.controls)
//            playerControls.setTimeBarMinUpdateInterval(MIN_TIM_BAR_UPDATE_INTERVAL_MS)
            controllerHeaderLayout = findViewById(R.id.controllerHeaderLayout)
            ivClose = findViewById(R.id.ivClose)

            controllerHeaderLayout.visibility = View.GONE
            playerView.setOnClickListener { handleControlsVisibility() }

            initLoader()
            initOrientationHandling()
            setPlayerSizePortrait()

            ivClose.setOnClickListener { onCloseClicked() }
            ivScreenSize.setOnClickListener { onFullScreenImgClicked() }
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
                        ContextCompat.getDrawable(context, R.drawable.ic_fullscreen_exit)
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
                        ContextCompat.getDrawable(context, R.drawable.ic_full_screen)
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

        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val epsilon = 5
                val leftLandscape = 90
                val rightLandscape = 270
                val topPortrait = 0
                val bottomPortrait = 360
                isPortrait?.let { isPortrait ->
                    if (!isPortrait) {
                        if (epsilonCheck(orientation, leftLandscape, epsilon) ||
                            epsilonCheck(orientation, rightLandscape, epsilon)
                        ) {
                            setOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE)
                        }
                    } else if (epsilonCheck(orientation, topPortrait, epsilon) ||
                        epsilonCheck(orientation, bottomPortrait, epsilon)
                    ) {
                        setOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT)
                    }
                    Handler(Looper.getMainLooper()).postDelayed(
                        { this@BasePlayerFragment.isPortrait = null }, 100
                    )
                } ?: run {
                    setOrientation(ActivityInfo.SCREEN_ORIENTATION_USER)
                }
            }
        }
        orientationEventListener.enable()
    }

    private fun isRotationOn(): Boolean = Settings.System.getInt(
        activity?.contentResolver,
        Settings.System.ACCELEROMETER_ROTATION,
        0
    ) == 1

    private fun epsilonCheck(a: Int, b: Int, epsilon: Int): Boolean =
        a > b - epsilon && a < b + epsilon

    private fun onFullScreenImgClicked() {
        val currentOrientation = activity?.resources?.configuration?.orientation
        if (!isRotationOn())
            orientationEventListener.disable()
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            isPortrait = false
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        } else {
            isPortrait = true
            setOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    private fun initLoader() {
        loader = context?.let { AnimatedVectorDrawableCompat.create(it, R.drawable.loader_logo) }
        ivLoader.setImageDrawable(loader)
        showLoading()
    }

    private fun setOrientation(orientation: Int) {
        activity?.requestedOrientation = orientation
    }

    private fun onCloseClicked() {
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
}
