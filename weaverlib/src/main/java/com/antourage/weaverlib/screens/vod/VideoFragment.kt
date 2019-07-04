package com.antourage.weaverlib.screens.vod


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.other.setMargins
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.DefaultTimeBar
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.custom_video_controls.*
import kotlinx.android.synthetic.main.fragment_chat.etMessage
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.fragment_video.btnSend
import kotlinx.android.synthetic.main.fragment_video.deviderChat
import kotlinx.android.synthetic.main.fragment_video.drawerLayout
import kotlinx.android.synthetic.main.fragment_video.ll_wrapper
import kotlinx.android.synthetic.main.fragment_video.navView
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.fragment_weaver_portrait.constraintLayoutParent
import kotlinx.android.synthetic.main.fragment_weaver_portrait.ivLoader
import kotlinx.android.synthetic.main.fragment_weaver_portrait.playerView
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvBroadcastedBy
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvStreamName


class VideoFragment : ChatFragment<VideoViewModel>() {

    companion object {
        const val ARGS_STREAM = "args_stream"

        fun newInstance(stream: StreamResponse): VideoFragment {
            val bundle = Bundle()
            bundle.putParcelable(ARGS_STREAM, stream)
            val fragment = VideoFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_video
    }

    //region Observers
    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_READY -> {
                    hideLoading()
                    if (viewModel.isPlaybackPaused()) {
                        playerControls.show()
                    }
                    viewModel.onVideoStarted(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.streamId)
                }
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_IDLE -> {
                    hideLoading()
                }
                Player.STATE_ENDED -> {
                    viewModel.removeStatisticsListeners()
                    hideLoading()
                }
            }
    }

    private val videoChangeObserver: Observer<StreamResponse> = Observer { video ->
        video?.let {
            tvStreamName.text = video.streamTitle
            tvBroadcastedBy.text = video.creatorFullname
            if (context != null)
                tvWasLive.text = video.startTime.parseDate(context!!)
            tvControllerStreamName.text = video.streamTitle
            tvControllerBroadcastedBy.text = video.creatorFullname
            txtNumberOfViewers.text = video.viewerCounter.toString()
            UserCache.newInstance().saveVideoToSeen(context!!, it.streamId)
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

    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_video_screen)
        startPlayingStream()
        handleChat()
        ll_wrapper.visibility = View.INVISIBLE
        if (context != null)
            tvWasLive.text =
                arguments?.getParcelable<StreamResponse>(WeaverFragment.ARGS_STREAM)?.startTime?.parseDate(context!!)
    }

    override fun onControlsVisible() {
//        if(context != null)
//            tvWasLive.text = arguments?.getParcelable<StreamResponse>(WeaverFragment.ARGS_STREAM)?.startTime?.parseDate(context!!)
    }


    private fun handleChat() {
        etMessage.isEnabled = false
        btnSend.isEnabled = false
        etMessage.hint = getString(R.string.chat_not_available)
    }

    fun startPlayingStream() {
        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId?.let { viewModel.setCurrentPlayerPosition(it) }
        playerView.player = viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.hlsUrl)
        playerControls.player = playerView.player
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            etMessage.visibility = View.GONE
            btnSend.visibility = View.GONE
            deviderChat.visibility = View.GONE
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            etMessage.visibility = View.VISIBLE
            btnSend.visibility = View.VISIBLE
            deviderChat.visibility = View.VISIBLE
        }
        ll_wrapper.visibility = View.INVISIBLE
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isChatDismissed) {
                drawerLayout.closeDrawer(navView)
            }
        }

    }

    private fun changeControlsView(isLandscape: Boolean) {
        if (isLandscape) {
            exo_progress.setMargins(148, 0, 148, 0)
            exo_position.setMargins(4, 0, 0, 4)
            ivScreenSize.setMargins(0, 0, 4, 4)
        } else {
            exo_progress.setMargins(0, 0, 0, 0)
            exo_position.setMargins(18, 0, 0, 18)
            ivScreenSize.setMargins(0, 0, 18, 18)
        }
    }

    override fun onNetworkConnectionLost() {

    }

    override fun onNetworkConnectionAvailable() {
        viewModel.onNetworkGained()

    }


}
