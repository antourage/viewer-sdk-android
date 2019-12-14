package com.antourage.weaverlib.screens.vod

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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
import kotlinx.android.synthetic.main.broadcaster_header.*
import kotlinx.android.synthetic.main.fragment_player_vod_portrait.*
import kotlinx.android.synthetic.main.layout_empty_chat_placeholder.*
import kotlinx.android.synthetic.main.player_custom_controls_vod.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.util.*
import kotlin.math.roundToInt

internal class VodPlayerFragment : ChatFragment<VideoViewModel>(),
    CustomDrawerLayout.DrawerDoubleTapListener {

    companion object {
        const val ARGS_STREAM = "args_stream"

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

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_READY -> {
                    hideLoading()
                    viewModel.currentVideo.value?.apply {
                        if (!broadcasterPicUrl.isNullOrEmpty()) {
                            Picasso.get().load(broadcasterPicUrl)
                                .placeholder(R.drawable.antourage_ic_default_user)
                                .error(R.drawable.antourage_ic_default_user)
                                .into(ivUserPhoto)
                            Picasso.get().load(broadcasterPicUrl)
                                .placeholder(R.drawable.antourage_ic_default_user)
                                .error(R.drawable.antourage_ic_default_user)
                                .into(ivControllerUserPhoto)
                        }
                    }
                    ivFirstFrame.visibility = View.INVISIBLE
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
            tvStreamName.text = videoName
            tvBroadcastedBy.text = creatorFullName
            tvControllerStreamName.text = videoName
            tvControllerBroadcastedBy.text = creatorFullName
            txtNumberOfViewers.text = viewsCount.toString()
            /**delete this code if image loading has no serious impact on
             * video loading (as it's too long in low bandwidth conditions)
             */
//            if (!broadcasterPicUrl.isNullOrEmpty()) {
//                Picasso.get().load(broadcasterPicUrl)
//                    .placeholder(R.drawable.antourage_ic_default_user)
//                    .error(R.drawable.antourage_ic_default_user)
//                    .into(ivUserPhoto)
//                Picasso.get().load(broadcasterPicUrl)
//                    .placeholder(R.drawable.antourage_ic_default_user)
//                    .error(R.drawable.antourage_ic_default_user)
//                    .into(ivControllerUserPhoto)
//            }
            context?.let { context ->
                updateWasLiveValueOnUI(startTime, duration)
                streamId?.let { UserCache.getInstance(context)?.saveVideoToSeen(it) }
            }
            viewModel.seekToLastWatchingTime()
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
            playBtnPlaceholder.visibility = View.GONE
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
        viewModel = ViewModelProviders.of(this, activity?.injector?.getVideoViewModelFactory())
            .get(VideoViewModel::class.java)
    }

    override fun subscribeToObservers() {
        super.subscribeToObservers()
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getCurrentVideo().observe(this.viewLifecycleOwner, videoChangeObserver)
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
    }

    private fun initSkipControls() {
        playerView.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector =
                GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                        handleControlsVisibility()
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
        exo_next.alpha = 0.3f
        exo_prev.alpha = 0.3f
    }

    private fun brightenNextPrevButtons() {
        exo_next?.visibility = View.VISIBLE
        exo_prev?.visibility = View.VISIBLE
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
        }
        playerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
        playerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }

    override fun onControlsVisible() {
        context?.let { _ ->
            val streamResponse =
                arguments?.getParcelable<StreamResponse>(PlayerFragment.ARGS_STREAM)
            streamResponse?.apply {
                updateWasLiveValueOnUI(startTime, duration)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        chatUiToLandscape(newOrientation == Configuration.ORIENTATION_LANDSCAPE)

        when (newOrientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                if (isChatDismissed) {
                    drawerLayout.closeDrawer(navView)
                }
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
        controls.findViewById<DefaultTimeBar>(R.id.exo_progress).setMargins(
            if (isLandscape)
                resources.getDimension(R.dimen.ant_margin_seekbar_landscape).toInt() else 0, 0,
            if (isLandscape)
                resources.getDimension(R.dimen.ant_margin_seekbar_landscape).toInt() else 0, 0
        )
    }

    private fun showSkipAnim(vDrawable: AnimatedVectorDrawableCompat?, iv: View) {
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
                iv.visibility = View.VISIBLE
            }
        }
    }

    private fun updateWasLiveValueOnUI(startTime: String?, duration: String?) {
        context?.apply {
            val formattedStartTime =
                duration?.parseToMills()?.plus((startTime?.parseToDate()?.time ?: 0))?.let {
                    Date(it).parseToDisplayAgoTime(this)
                }
            tvWasLive.text = formattedStartTime
            tvWasLive.gone(formattedStartTime.isNullOrEmpty())
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
