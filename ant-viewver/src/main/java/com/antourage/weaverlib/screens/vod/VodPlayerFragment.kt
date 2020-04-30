package com.antourage.weaverlib.screens.vod

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.ui.CustomDrawerLayout
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_player_vod_portrait.*
import kotlinx.android.synthetic.main.layout_empty_chat_placeholder.*
import kotlinx.android.synthetic.main.player_custom_controls_vod.*
import kotlinx.android.synthetic.main.player_header.*
import java.util.*
import kotlin.math.roundToInt

internal class VodPlayerFragment : ChatFragment<VideoViewModel>(),
    CustomDrawerLayout.DrawerDoubleTapListener {

    companion object {
        const val ARGS_STREAM = "args_stream"
        const val MIN_PROGRESS_UPDATE_MILLIS = 50L
        const val MAX_PROGRESS_UPDATE_MILLIS = 500L

        fun newInstance(stream: StreamResponse): VodPlayerFragment {
            val fragment = VodPlayerFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_STREAM, stream)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId() = R.layout.fragment_player_vod_portrait
    private var skipForwardVDrawable: AnimatedVectorDrawableCompat? = null
    private var skipBackwardVDrawable: AnimatedVectorDrawableCompat? = null
    private val progressHandler = Handler()
    private val updateProgressAction = Runnable { updateProgressBar() }

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_READY -> {
                    hideLoading()
                    vod_player_progress.max = viewModel.getVideoDuration()?.toInt() ?: 1
                    if (!playerControls.isVisible) {
                        progressHandler.postDelayed(
                            updateProgressAction,
                            MIN_PROGRESS_UPDATE_MILLIS
                        )
                    }
                    viewModel.currentVideo.value?.apply {
                        if (!broadcasterPicUrl.isNullOrEmpty()) {
                            Picasso.get().load(broadcasterPicUrl)
                                .placeholder(R.drawable.antourage_ic_default_user)
                                .error(R.drawable.antourage_ic_default_user)
                                .into(play_header_iv_photo)

                            Picasso.get().load(broadcasterPicUrl)
                                .placeholder(R.drawable.antourage_ic_default_user)
                                .error(R.drawable.antourage_ic_default_user)
                                .into(
                                    player_control_header
                                        .findViewById(R.id.play_header_iv_photo) as ImageView
                                )
                        }

                    }
                    ivFirstFrame.visibility = View.INVISIBLE
                    updatePrevNextVisibility()
                    if (viewModel.isPlaybackPaused()) {
                        playerControls.show()
                        viewModel.onVideoPausedOrStopped()
                    } else {
                        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
                            ?.streamId?.let { streamId ->
                                viewModel.onVideoStarted(streamId)
                            }
                    }
                }
                Player.STATE_IDLE -> hideLoading()
                Player.STATE_ENDED -> {
                    viewModel.removeStatisticsListeners()
                    viewModel.onVideoPausedOrStopped()
                    hideLoading()
                }
            }
    }

    private val videoChangeObserver: Observer<StreamResponse> = Observer { video ->
        video?.apply {
            if (viewModel.getVideoDuration() != null) {
                vod_player_progress.max = viewModel.getVideoDuration()?.toInt() ?: 1
            }
            tvStreamName.text = videoName
            tvBroadcastedBy.text = creatorFullName
            player_control_header.findViewById<TextView>(R.id.tvStreamName).text = videoName
            player_control_header.findViewById<TextView>(R.id.tvBroadcastedBy).text =
                creatorFullName
            txtNumberOfViewers.text = viewsCount.toString()
            context?.let { context ->
                updateWasLiveValueOnUI(startTime, duration)
                streamId?.let { UserCache.getInstance(context)?.saveVideoToSeen(it) }
            }
            if (!broadcasterPicUrl.isNullOrEmpty()) {
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.antourage_ic_default_user)
                    .error(R.drawable.antourage_ic_default_user)
                    .into(play_header_iv_photo)
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.antourage_ic_default_user)
                    .error(R.drawable.antourage_ic_default_user)
                    .into(
                        player_control_header
                            .findViewById(R.id.play_header_iv_photo) as ImageView
                    )
            }
            viewModel.seekToLastWatchingTime()
        }
    }

    private fun updatePrevNextVisibility() {
        vod_control_prev.visibility = if (viewModel.hasPrevTrack()) View.VISIBLE else View.INVISIBLE
        vod_control_next.visibility = if (viewModel.hasNextTrack()) View.VISIBLE else View.INVISIBLE
    }

    @SuppressLint("ClickableViewAccessibility")
    private val endVideoObserver: Observer<Boolean> = Observer { isEnded ->
        if (isEnded && vod_next_auto_layout.visibility != View.VISIBLE) {
            val mCountDownTimer = object : CountDownTimer(5000, 20) {
                override fun onTick(millisUntilFinished: Long) {
                    vod_progress_bar?.progress = millisUntilFinished.toInt()
                }

                override fun onFinish() {
                    if (vod_next_auto_layout?.visibility == View.VISIBLE) {
                        viewModel.nextVideoPlay()
                    }
                }
            }
            vod_progress_bar.progress = 5000
            mCountDownTimer.start()
            vod_buttons_layout.visibility = View.INVISIBLE
            vod_next_auto_layout.visibility = View.VISIBLE
            vod_auto_next_cancel.setOnClickListener {
                mCountDownTimer.cancel()
                vod_play_pause_layout.visibility = View.INVISIBLE
                vod_buttons_layout.visibility = View.VISIBLE
                vod_next_auto_layout.visibility = View.INVISIBLE
                vod_rewind.visibility = View.VISIBLE
            }
            vod_rewind.setOnClickListener { viewModel.rewindVideoPlay() }
            controls.findViewById<DefaultTimeBar>(R.id.exo_progress).setOnTouchListener { v, _ ->
                mCountDownTimer.cancel()
                vod_buttons_layout.visibility = View.VISIBLE
                vod_next_auto_layout.visibility = View.INVISIBLE
                v.setOnTouchListener { _, _ -> false }
                return@setOnTouchListener false
            }

            vod_controls_auto_next.setOnClickListener {
                mCountDownTimer.cancel()
                viewModel.nextVideoPlay()
            }
        } else if (!isEnded) {
            vod_play_pause_layout.visibility = View.VISIBLE
            vod_buttons_layout.visibility = View.VISIBLE
            vod_next_auto_layout.visibility = View.INVISIBLE
            vod_rewind.visibility = View.INVISIBLE
        }
    }

    private val chatStateObserver: Observer<Boolean> = Observer { showNoMessagesPlaceholder ->
        if (showNoMessagesPlaceholder == true) {
            if (orientation() == Configuration.ORIENTATION_PORTRAIT) {
                showChatTurnedOffPlaceholder(true)
            }
        } else {
            showChatTurnedOffPlaceholder(false)
        }
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            viewModel.onNetworkGained()
            playBtnPlaceholder.visibility = View.INVISIBLE
        } else if (networkState?.ordinal == NetworkConnectionState.LOST.ordinal) {
            if (!Global.networkAvailable) {
                context?.resources?.getString(R.string.ant_no_internet)
                    ?.let { messageToDisplay ->
                        Handler().postDelayed({
                            showWarningAlerter(messageToDisplay)
                            playBtnPlaceholder.visibility = View.VISIBLE
                        }, 500)
                    }
            }
        }
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.injector?.getVideoViewModelFactory()?.let {
            viewModel = ViewModelProvider(this, it).get(VideoViewModel::class.java)
        }
    }

    override fun subscribeToObservers() {
        super.subscribeToObservers()
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getCurrentVideo().observe(this.viewLifecycleOwner, videoChangeObserver)
        viewModel.getVideoEndedLD().observe(this.viewLifecycleOwner, endVideoObserver)
        viewModel.getChatStateLiveData().observe(this.viewLifecycleOwner, chatStateObserver)
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
        viewModel.currentStreamViewsLiveData
            .observe(this.viewLifecycleOwner, currentStreamViewsObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        initSkipAnimations()
        drawerLayout.touchListener = this
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_vod)
        startPlayingStream()
        handleChat()
        val streamResponse = arguments?.getParcelable<StreamResponse>(PlayerFragment.ARGS_STREAM)
        streamResponse?.apply {
            updateWasLiveValueOnUI(startTime, duration)
            viewModel.initUi(streamId, startTime, id, stopTime)
            streamId?.let { viewModel.setStreamId(it) }

            thumbnailUrl?.let {
                Picasso.get()
                    .load(thumbnailUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(ivFirstFrame)
            }
        }
        setUpNoChatPlaceholder(
            R.drawable.antourage_ic_chat_no_comments_yet,
            R.string.ant_no_comments_yet
        )
        initSkipControls()
        player_control_header.findViewById<ImageView>(R.id.play_header_iv_close)
            .setOnClickListener {
                onCloseClicked()
            }
        initControlsVisibilityListener()
        initPlayerClickListeners()
        initPortraitProgressListener()
    }

    private fun initPortraitProgressListener() {
        controls.setProgressUpdateListener { pos, _ -> vod_player_progress.progress = pos.toInt() }
    }

    private fun updateProgressBar() {
        val duration = viewModel.getVideoDuration()?.toInt()
        val position = viewModel.getVideoPosition()?.toInt()
        if (duration != null && position != null) {
            vod_player_progress?.max = duration
            vod_player_progress?.progress = position
        }
        progressHandler.removeCallbacks(updateProgressAction)
        progressHandler.postDelayed(
            updateProgressAction,
            if (!viewModel.isPlaybackPaused()) MIN_PROGRESS_UPDATE_MILLIS else MAX_PROGRESS_UPDATE_MILLIS
        )
    }

    private fun initPlayerClickListeners() {
        vod_control_prev.setOnClickListener { viewModel.prevVideoPlay() }
        vod_control_next.setOnClickListener { viewModel.nextVideoPlay() }
        updatePrevNextVisibility()
    }

    private fun initControlsVisibilityListener() {
        playerControls.setVisibilityListener { visibility ->
            if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                if (visibility == View.VISIBLE) {
                    txtNumberOfViewers.marginDp(12f, 62f)
                } else {
                    txtNumberOfViewers.marginDp(12f, 12f)
                }
            } else if (visibility == View.VISIBLE) {
                //stops progress bar updates
                progressHandler.removeCallbacks(updateProgressAction)

            } else if (visibility != View.VISIBLE) {
                //starts progress bar updates when controls are invisible
                progressHandler.postDelayed(updateProgressAction, MIN_PROGRESS_UPDATE_MILLIS)
            }
        }
    }

    private fun initSkipControls() {
        playerView.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector =
                GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        toggleControlsVisibility()
                        return super.onSingleTapConfirmed(e)
                    }

                    override fun onDoubleTap(e: MotionEvent?): Boolean {
                        controls.hide()
                        e?.x?.let {
                            if (it > playerView.width / 2) {
                                handleSkipForward()
                            } else {
                                handleSkipBackward()
                            }
                        }
                        return super.onDoubleTap(e)
                    }
                })

            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                gestureDetector.onTouchEvent(p1)
                return true
            }
        })

        drawerLayout.doubleTapListener = this
    }

    private fun handleSkipForward() {
        dimNextPrevButtons()
        showSkipAnim(skipForwardVDrawable, skipForward)
        viewModel.skipForward()
    }

    private fun handleSkipBackward() {
        dimNextPrevButtons()
        showSkipAnim(skipBackwardVDrawable, skipBackward)
        viewModel.skipBackward()
    }

    private fun dimNextPrevButtons() {
        vod_control_next.alpha = 0.3f
        vod_control_prev.alpha = 0.3f
    }

    private fun brightenNextPrevButtons() {
        vod_control_next?.visibility = View.VISIBLE
        vod_control_prev?.visibility = View.VISIBLE
        controls?.hide()
    }

    private fun initSkipAnimations() {
        skipForwardVDrawable =
            context?.let {
                AnimatedVectorDrawableCompat.create(
                    it,
                    R.drawable.antourage_skip_forward
                )
            }
        skipBackwardVDrawable =
            context?.let { AnimatedVectorDrawableCompat.create(it, R.drawable.antourage_skip_back) }
        skipForward.setImageDrawable(skipForwardVDrawable)
        skipBackward.setImageDrawable(skipBackwardVDrawable)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
        if (viewModel.isPlaybackPaused()) {
            playerControls.show()
        } else if (playerControls.visibility == View.INVISIBLE && orientation() == Configuration.ORIENTATION_PORTRAIT) {
            progressHandler.postDelayed(updateProgressAction, MIN_PROGRESS_UPDATE_MILLIS)
        }
        playerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
        progressHandler.removeCallbacks(updateProgressAction)
        playerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }

    override fun onControlsVisible() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        chatUiToLandscape(newOrientation == Configuration.ORIENTATION_LANDSCAPE)

        when (newOrientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                showChatTurnedOffPlaceholder(false)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                if (viewModel.getChatStateLiveData().value == true) {
                    showChatTurnedOffPlaceholder(true)
                }
            }
        }

        initSkipControls()
        viewModel.getPlaybackState().reObserve(this.viewLifecycleOwner, streamStateObserver)
    }

    override fun onDrawerDoubleClick() {
        controls.hide()
        handleSkipBackward()
    }
    //region chatUI helper func

    private fun chatUiToLandscape(landscape: Boolean) {
        if (landscape) {
            etMessage.visibility = View.GONE
            btnSend.visibility = View.GONE
            dividerChat.visibility = View.GONE
        } else {
            etMessage.visibility = View.VISIBLE
            btnSend.visibility = View.VISIBLE
            dividerChat.visibility = View.VISIBLE
        }
        changeControlsView(landscape)
    }

    /**
     * Used to change control buttons size on landscape/portrait.
     * I couldn't use simple dimensions change due to specific orientation handling in project.
     */
    private fun changeButtonsSize(isEnlarge: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(vod_buttons_layout)
        updateIconSize(
            R.id.vod_rewind, constraintSet,
            if (isEnlarge) R.dimen.large_play_pause_size else R.dimen.small_play_pause_size
        )
        updateIconSize(
            R.id.vod_control_next, constraintSet,
            if (isEnlarge) R.dimen.large_next_prev_size else R.dimen.small_next_prev_size
        )
        updateIconSize(
            R.id.vod_control_prev, constraintSet,
            if (isEnlarge) R.dimen.large_next_prev_size else R.dimen.small_next_prev_size
        )
        constraintSet.applyTo(vod_buttons_layout)

        val constraintSet2 = ConstraintSet()
        constraintSet2.clone(vod_play_pause_layout)
        updateIconSize(
            R.id.exo_play, constraintSet2,
            if (isEnlarge) R.dimen.large_play_pause_size else R.dimen.small_play_pause_size
        )
        updateIconSize(
            R.id.exo_pause, constraintSet2,
            if (isEnlarge) R.dimen.large_play_pause_size else R.dimen.small_play_pause_size
        )
        constraintSet2.applyTo(vod_play_pause_layout)

        val constraintSet3 = ConstraintSet()
        constraintSet3.clone(vod_next_auto_layout)
        updateIconSize(
            R.id.vod_controls_auto_next, constraintSet3,
            if (isEnlarge) R.dimen.large_play_pause_size else R.dimen.small_play_pause_size
        )
        constraintSet3.applyTo(vod_next_auto_layout)
    }

    private fun updateIconSize(iconId: Int, constraintSet: ConstraintSet, dimenId: Int) {
        val iconSize = resources.getDimension(dimenId).toInt()
        constraintSet.constrainWidth(iconId, iconSize)
        constraintSet.constrainHeight(iconId, iconSize)
    }

    private fun handleChat() {
        etMessage.isEnabled = false
        btnSend.isEnabled = false
        etMessage.hint = getString(R.string.ant_chat_not_available)
    }

    private fun orientation() = resources.configuration.orientation

    private fun startPlayingStream() {
        val streamResponse = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
        streamResponse?.apply {
            streamId?.let { viewModel.setCurrentPlayerPosition(it) }
            playerView.player = videoURL?.let { viewModel.getExoPlayer(it) }
        }
        playerControls.player = playerView.player
    }

    private fun showChatTurnedOffPlaceholder(show: Boolean) {
        llNoChat.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun setUpNoChatPlaceholder(@DrawableRes drawable: Int, @StringRes text: Int) {
        ivNoChat.background =
            context?.let {
                ContextCompat.getDrawable(
                    it,
                    drawable
                )
            }
        txtNoChat.text = getString(text)
    }

    //endregion

    private fun changeControlsView(isLandscape: Boolean) {
        context?.apply {
            rvMessages.setPadding(
                dp2px(this, 0f).roundToInt(),
                dp2px(this, 0f).roundToInt(),
                dp2px(this, 0f).roundToInt(),
                dp2px(this, if (isLandscape) 15f else 0f).roundToInt()
            )
        }
        val progressMarginPx = if (isLandscape) {
            resources.getDimension(R.dimen.ant_margin_seekbar_landscape).toInt()
        } else {
            resources.getDimension(R.dimen.ant_margin_seekbar_portrait).toInt()
        }
        controls.findViewById<DefaultTimeBar>(R.id.exo_progress).setMargins(
            progressMarginPx, 0, progressMarginPx, 0
        )
        vod_shadow.marginDp(bottom = if (isLandscape) 0f else 15f)
        changeButtonsSize(isEnlarge = isLandscape)
    }

    private fun showSkipAnim(vDrawable: AnimatedVectorDrawableCompat?, iv: View?) {
        vDrawable?.apply {
            if (!isRunning) {
                registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable?) {
                        iv?.visibility = View.INVISIBLE
                        brightenNextPrevButtons()
                        clearAnimationCallbacks()
                    }
                })
                start()
                iv?.visibility = View.VISIBLE
            }
        }
    }

    override fun onMinuteChanged() {
        viewModel.currentVideo.value.let {
            if (it?.startTime != null && it?.duration != null) {
                updateWasLiveValueOnUI(it.startTime, it.duration)
            }
        }
    }

    private fun updateWasLiveValueOnUI(startTime: String?, duration: String?) {
        context?.apply {
            val formattedStartTime =
                duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                    Date(it).parseToDisplayAgoTimeLong(this)
                }
            play_header_tv_ago.text = formattedStartTime
            play_header_tv_ago.gone(formattedStartTime.isNullOrEmpty())
            val tvAgoLandscape = player_control_header
                .findViewById<TextView>(R.id.play_header_tv_ago)
            tvAgoLandscape.text = formattedStartTime
            tvAgoLandscape.gone(formattedStartTime.isNullOrEmpty())
        }
    }

    private val currentStreamViewsObserver: Observer<Int> = Observer { currentViewsCount ->
        updateViewsCountUI(currentViewsCount)
    }

    private fun updateViewsCountUI(currentViewsCount: Int?) {
        currentViewsCount?.let {
            txtNumberOfViewers.text = it.toString()
        }
    }
}
