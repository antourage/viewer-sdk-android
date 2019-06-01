package com.antourage.weaverlib.screens.vod


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.chat.rv.MessagesAdapter
import com.antourage.weaverlib.screens.list.rv.VideosLayoutManager
import com.antourage.weaverlib.screens.vod.rv.ChatLayoutManager
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.custom_video_controls.*
import kotlinx.android.synthetic.main.fragment_chat.etMessage
import kotlinx.android.synthetic.main.fragment_chat.rvMessages
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.fragment_weaver_portrait.constraintLayoutParent
import kotlinx.android.synthetic.main.fragment_weaver_portrait.ivLoader
import kotlinx.android.synthetic.main.fragment_weaver_portrait.playerView
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvBroadcastedBy
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvStreamName
import kotlinx.android.synthetic.main.layout_chat.*

class VideoFragment : StreamingFragment<VideoViewModel>() {

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
    private val messagesObserver: Observer<List<Message>> = Observer { list ->
        if (list != null) {
            (rvMessages.adapter as MessagesAdapter).setMessageList(list)
        }
    }
    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_READY ->{
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
            tvWasLive.text = "1 day ago"
            tvControllerStreamName.text = video.streamTitle
            tvControllerBroadcastedBy.text = video.creatorFullname
        }
    }

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideoViewModel::class.java)
    }

    override fun subscribeToObservers() {
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getCurrentVideo().observe(this.viewLifecycleOwner, videoChangeObserver)
        viewModel.getMessagesLiveData().observe(this.viewLifecycleOwner, messagesObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_video_screen)
        startPlayingStream()
        initMessagesRV()
        handleChat()
        ivClose.setOnClickListener { fragmentManager?.popBackStack() }
    }

    private fun handleChat() {
        etMessage.isEnabled = false
        btnSend.isEnabled = false
        etMessage.hint = getString(R.string.chat_not_available)
    }

    private fun initMessagesRV() {
        val linearLayoutManager = ChatLayoutManager(
            context
        )
        linearLayoutManager.stackFromEnd = true
        rvMessages.overScrollMode = View.OVER_SCROLL_NEVER
        rvMessages.isVerticalFadingEdgeEnabled = false
        rvMessages.layoutManager = linearLayoutManager
        rvMessages.adapter = MessagesAdapter(listOf(), Configuration.ORIENTATION_PORTRAIT)
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

    override fun onPause() {
        super.onPause()

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
            rvMessages.isVerticalFadingEdgeEnabled = true
            rvMessages.adapter =
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_LANDSCAPE)
            etMessage.visibility = View.GONE
            btnSend.visibility = View.GONE
            deviderChat.visibility = View.GONE
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            rvMessages.isVerticalFadingEdgeEnabled = false
            rvMessages.adapter =
                MessagesAdapter(viewModel.getMessagesLiveData().value!!, Configuration.ORIENTATION_PORTRAIT)
            etMessage.visibility = View.VISIBLE
            btnSend.visibility = View.VISIBLE
            deviderChat.visibility = View.VISIBLE
        }
    }

}
