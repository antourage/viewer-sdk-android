package com.antourage.weaverlib.screens.vod


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.weaver.WeaverFragment
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.custom_video_controls.*
import kotlinx.android.synthetic.main.custom_video_controls.tvWasLive
import kotlinx.android.synthetic.main.custom_video_controls.txtNumberOfViewers
import kotlinx.android.synthetic.main.fragment_chat.etMessage
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.fragment_weaver_portrait.constraintLayoutParent
import kotlinx.android.synthetic.main.fragment_weaver_portrait.ivLoader
import kotlinx.android.synthetic.main.fragment_weaver_portrait.playerView
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvBroadcastedBy
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvStreamName
import kotlinx.android.synthetic.main.player_custom_control.*

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
            if(context != null)
                tvWasLive.text = video.startTime.parseDate(context!!)
            tvControllerStreamName.text = video.streamTitle
            tvControllerBroadcastedBy.text = video.creatorFullname
            txtNumberOfViewers.text = video.viewerCounter.toString()

        }
    }

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideoViewModel::class.java)
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
        if(context != null)
            tvWasLive.text = arguments?.getParcelable<StreamResponse>(WeaverFragment.ARGS_STREAM)?.startTime?.parseDate(context!!)
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
    }

    override fun onNetworkConnectionLost() {
        viewModel.onNetworkLost()
    }

    override fun onNetworkConnectionAvailable() {
        startPlayingStream()
        viewModel.onNetworkGained()

    }



}
