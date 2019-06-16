package com.antourage.weaverlib.screens.base.streaming

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.graphics.drawable.Animatable2Compat
import android.support.graphics.drawable.AnimatedVectorDrawableCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.OrientationEventListener
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.calculatePlayerHeight
import com.antourage.weaverlib.other.replaceFragment
import com.antourage.weaverlib.screens.base.BaseFragment
import com.antourage.weaverlib.screens.list.VideoListFragment
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView


abstract class StreamingFragment<VM : StreamingViewModel> : BaseFragment<VM>(){
    private lateinit var orientationEventListener: OrientationEventListener
    private var loader: AnimatedVectorDrawableCompat? = null
    private var isPortrait = false

    private lateinit var ivLoader: ImageView
    private lateinit var constraintLayoutParent: ConstraintLayout
    private lateinit var playerView: PlayerView
    private lateinit var ivScreenSize: ImageView
    private lateinit var btnChooseTrack: Button
    protected lateinit var playerControls: PlayerControlView
    private lateinit var controllerHeaderLayout: ConstraintLayout


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
        //    chatLayout.visibility = View.VISIBLE
        //playerView.useController = !isInPictureInPictureMode
            super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    }

    override fun initUi(view: View?) {
        if (view != null) {
            ivLoader = view.findViewById(R.id.ivLoader)
            playerView = view.findViewById(R.id.playerView)
            constraintLayoutParent = view.findViewById(R.id.constraintLayoutParent)
            ivScreenSize = view.findViewById(R.id.ivScreenSize)
            btnChooseTrack = view.findViewById(R.id.btnChooseTrack)
            playerControls = view.findViewById(R.id.controls)
            controllerHeaderLayout = view.findViewById(R.id.controllerHeaderLayout)
            controllerHeaderLayout.visibility = View.GONE
            playerView.setOnClickListener {
                handleControlsVisibility()
            }
            val ivClose:ImageView = view.findViewById(R.id.ivClose)
            ivClose.setOnClickListener {
                fragmentManager?.let { fragmentManager->
                    if ( fragmentManager.backStackEntryCount>0)
                        fragmentManager.popBackStack()
                    else
                        replaceFragment(VideoListFragment.newInstance(),R.id.mainContent)
                }
            }
            initLoader()
            initOrientationHandling()
            setPlayerSizePortrait()
            ivScreenSize.setOnClickListener {
                val currentOrientation = activity?.resources?.configuration?.orientation
                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                    isPortrait = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    isPortrait = true
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

                }
            }
            btnChooseTrack.setOnClickListener {
                val arrayResolution = viewModel.getStreamGroups()
                val str: MutableList<String> = mutableListOf<String>()
                for (i in 0 until arrayResolution.size) {
                    str.add(arrayResolution[i].width.toString() + "x" + arrayResolution[i].height)
                }
                val builder = AlertDialog.Builder(activity!!)
                builder.setTitle("Pick a resolution")
                builder.setItems(str.toTypedArray()) { _, which ->
                    viewModel.onResolutionChanged(which)
                }
                builder.show()
            }
        }
    }
    protected fun handleControlsVisibility(){
        if(playerControls.isVisible)
            playerControls.hide()
        else {
            onControlsVisible()
            playerControls.show()

        }
    }
    abstract  fun onControlsVisible()

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
            playerControls.hide()
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
                Log.d("StreamingFragment"," "+orientation)
                val epsilon = 10
                val leftLandscape = 90
                val rightLandscape = 270
                if (!isPortrait) {
                    if (epsilonCheck(orientation, leftLandscape, epsilon) ||
                        epsilonCheck(orientation, rightLandscape, epsilon)
                    ) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                    }
                } else if(epsilonCheck(orientation, 0, 5)||epsilonCheck(orientation,360,5)){
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_USER
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
            context?.let {context->
                ivScreenSize.background = ContextCompat.getDrawable(context, R.drawable.ic_fullscreen_exit)
            }
            controllerHeaderLayout.visibility = View.VISIBLE
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            constraintLayoutParent.setState(
                R.id.constraintStatePortrait,
                newConfig.screenWidthDp,
                newConfig.screenHeightDp
            )
            context?.let {context->
                ivScreenSize.background = ContextCompat.getDrawable(context, R.drawable.ic_full_screen)
            }
            controllerHeaderLayout.visibility = View.GONE
            setPlayerSizePortrait()
            activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        playerControls.hide()
    }



}