package com.antourage.weaverlib.screens.base.streaming

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.calculatePlayerHeight
import com.antourage.weaverlib.screens.base.BaseFragment
import com.google.android.exoplayer2.ui.PlayerView

abstract class StreamingFragment<VM : StreamingViewModel> : BaseFragment<VM>() {
    private lateinit var orientationEventListener: OrientationEventListener
    private var loader: AnimatedVectorDrawableCompat? = null

    private lateinit var ivLoader: ImageView
    private lateinit var constraintLayoutParent: ConstraintLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivScreenSize: ImageView
    private lateinit var chatLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()
    }

    abstract fun subscribeToObservers()

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {

        } else
            chatLayout.visibility = View.VISIBLE

        playerView.useController = !isInPictureInPictureMode
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    override fun initUi(view: View?) {
        if (view != null) {
            ivLoader = view.findViewById(R.id.ivLoader)
            playerView = view.findViewById(R.id.playerView)
            constraintLayoutParent = view.findViewById(R.id.constraintLayoutParent)
            ivScreenSize = view.findViewById(R.id.ivScreenSize)
            chatLayout = view.findViewById(R.id.chatLayout)
            initLoader()
            initOrientationHandling()
            setPlayerSizePortrait()
            ivScreenSize.setOnClickListener {
                val currentOrientation = activity?.resources?.configuration?.orientation
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    protected fun setPlayerSizePortrait() {
        val params = playerView.layoutParams
        params.height = calculatePlayerHeight(activity!!).toInt()
        playerView.layoutParams = params
    }


    private fun initLoader() {
        if (context != null) {
            loader = AnimatedVectorDrawableCompat.create(context!!, R.drawable.loader_logo)
            ivLoader.setImageDrawable(loader)
        }
        showLoading()
    }

    protected fun showLoading() {
        if (loader != null && !loader!!.isRunning) {
            loader?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    ivLoader.post { loader?.start() }
                }
            })
            loader?.start()
            ivLoader.visibility = View.VISIBLE
        }
    }

    protected fun hideLoading() {
        if (ivLoader.visibility == View.VISIBLE && (loader != null && loader!!.isRunning())) {
            ivLoader.visibility = View.GONE
            loader?.clearAnimationCallbacks()
            loader?.stop()
        }
    }

    private fun initOrientationHandling() {
        orientationEventListener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                val epsilon = 10
                val leftLandscape = 90
                val rightLandscape = 270
                if (epsilonCheck(orientation, leftLandscape, epsilon) ||
                    epsilonCheck(orientation, rightLandscape, epsilon)
                ) {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationEventListener.enable()
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        orientationEventListener.disable()
    }

    private fun epsilonCheck(a: Int, b: Int, epsilon: Int): Boolean {
        return a > b - epsilon && a < b + epsilon
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val newOrientation = newConfig.orientation

        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            constraintLayoutParent.setState(
                R.id.constraintStateLandscape,
                newConfig.screenWidthDp,
                newConfig.screenHeightDp
            )
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            constraintLayoutParent.setState(
                R.id.constraintStatePortrait,
                newConfig.screenWidthDp,
                newConfig.screenHeightDp
            )
            setPlayerSizePortrait()
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
}