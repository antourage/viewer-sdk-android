package com.antourage.weaverlib.screens.vod

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.gone
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.other.reObserve
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.weaver.PlayerFragment
import com.google.android.exoplayer2.Player
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.custom_video_controls.*
import kotlinx.android.synthetic.main.fragment_vod_player.*
import kotlinx.android.synthetic.main.layout_no_chat.*

class VodPlayerFragment : ChatFragment<VideoViewModel>() {

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

    override fun getLayoutId() = R.layout.fragment_vod_player

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_READY -> {
                    hideLoading()
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
            txtNumberOfViewers.text = viewerCounter.toString()
            if (!broadcasterPicUrl.isNullOrEmpty()) {
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.ic_default_user)
                    .error(R.drawable.ic_default_user)
                    .into(ivUserPhoto)
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.ic_default_user)
                    .error(R.drawable.ic_default_user)
                    .into(ivControllerUserPhoto)
            }
            context?.let { context ->
                val formattedStartTime = startTime?.parseDate(context)
                tvWasLive.text = formattedStartTime
                tvWasLive.gone(formattedStartTime.isNullOrEmpty())
                streamId?.let { UserCache.newInstance().saveVideoToSeen(context, it) }
            }
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
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_video_screen)
        startPlayingStream()
        handleChat()
        ll_wrapper.visibility = View.INVISIBLE
        val streamResponse = arguments?.getParcelable<StreamResponse>(PlayerFragment.ARGS_STREAM)
        streamResponse?.apply {
            tvWasLive.text = context?.let { startTime?.parseDate(it) }
            viewModel.initUi(streamId, startTime)
        }
        setUpNoChatPlaceholder(
            R.drawable.ic_chat_no_comments_yet,
            R.string.no_comments_yet
        )
//        showChatTurnedOffPlaceholder(orientation() ==  Configuration.ORIENTATION_PORTRAIT)
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

    override fun onStop() {
        super.onStop()
        viewModel.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }

    override fun onControlsVisible() {
        tvWasLive.text = context?.let { context ->
            arguments?.getParcelable<StreamResponse>(PlayerFragment.ARGS_STREAM)
                ?.startTime?.parseDate(context)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        chatUiToLandscape(newOrientation == Configuration.ORIENTATION_LANDSCAPE)
        ll_wrapper.visibility = View.INVISIBLE

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

        viewModel.getPlaybackState().reObserve(this.viewLifecycleOwner, streamStateObserver)
    }

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
        etMessage.hint = getString(R.string.chat_not_available)
    }

    private fun startPlayingStream() {
        val streamResponse = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
        streamResponse?.apply {
            streamId?.let { viewModel.setCurrentPlayerPosition(it) }
            playerView.player = viewModel.getExoPlayer(videoURL)
        }
        playerControls.player = playerView.player
    }

    //region chatUI helper func

    private fun enableMessageInput(enable: Boolean) {
        etMessage.isEnabled = enable
    }

    private fun hideMessageInput() {
        ll_wrapper.visibility = View.INVISIBLE
    }

    private fun showMessageInput() {
        ll_wrapper.visibility = View.VISIBLE
    }

    private fun removeMessageInput() {
        ll_wrapper.visibility = View.GONE
    }

    private fun orientation() = resources.configuration.orientation

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

    private fun showRvMessages() {
        rvMessages.visibility = View.VISIBLE
    }

    private fun hideRvMessages() {
        rvMessages.visibility = View.INVISIBLE
    }

    private fun enableChatUI() {
        setUpNoChatPlaceholder(
            R.drawable.ic_chat_no_comments_yet,
            R.string.no_comments_yet
        )
        enableMessageInput(true)
        showMessageInput()
    }

    private fun disableChatUI() {
        setUpNoChatPlaceholder(
            R.drawable.ic_chat_off_layered,
            R.string.commenting_off
        )
        enableMessageInput(false)
        hideMessageInput()
    }

    //endregion

    private fun changeControlsView(isLandscape: Boolean) {
//        if (isLandscape) {
//            controls.findViewById<DefaultTimeBar>(R.id.exo_progress).setMargins(
//                resources.getDimension(R.dimen.margin_seekbar_landscape).toInt(), 0,
//                resources.getDimension(R.dimen.margin_seekbar_landscape).toInt(), 0
//            )
//            controls.findViewById<TextView>(R.id.exo_position).setMargins(
//                resources.getDimension(R.dimen.margin_time_landscape).toInt(), 0,
//                0, resources.getDimension(R.dimen.margin_time_landscape).toInt()
//            )
//            ivScreenSize.setMargins(
//                0, 0, resources.getDimension(R.dimen.margin_size_landscape).toInt(),
//                resources.getDimension(R.dimen.margin_size_landscape).toInt()
//            )
//        } else {
//            controls.findViewById<DefaultTimeBar>(R.id.exo_progress).setMargins(0, 0, 0, 0)
//            context?.let { _ ->
//                controls.findViewById<TextView>(R.id.exo_position).setMargins(
//                    resources.getDimension(R.dimen.margin_portrait).toInt(), 0,
//                    0, resources.getDimension(R.dimen.margin_portrait).toInt()
//                )
//                ivScreenSize.setMargins(
//                    0, 0, resources.getDimension(R.dimen.margin_portrait).toInt(),
//                    resources.getDimension(R.dimen.margin_portrait).toInt()
//                )
//            }
//        }
    }
}
