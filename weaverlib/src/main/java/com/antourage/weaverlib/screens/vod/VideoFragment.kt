package com.antourage.weaverlib.screens.vod


import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceChildFragment
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.chat.ChatFragment
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.fragment_weaver_portrait.constraintLayoutParent
import kotlinx.android.synthetic.main.fragment_weaver_portrait.ivLoader
import kotlinx.android.synthetic.main.fragment_weaver_portrait.playerView
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvBroadcastedBy
import kotlinx.android.synthetic.main.fragment_weaver_portrait.tvStreamName

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

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_READY -> hideLoading()
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

    private val videoChangeObserver:Observer<StreamResponse> = Observer { video->
        replaceChildFragment(ChatFragment.newInstance(video.streamId,video.isLive), R.id.chatLayout)
        tvStreamName.text = video.streamTitle
        tvBroadcastedBy.text = video.creatorFullname
    }

    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(VideoViewModel::class.java)
    }

    override fun subscribeToObservers() {
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getCurrentVideo().observe(this.viewLifecycleOwner,videoChangeObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_video_screen)
        startPlayingStream()
        ivClose.setOnClickListener { fragmentManager?.popBackStack() }
    }



    fun startPlayingStream() {
        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId?.let { viewModel.setCurrentPlayerPosition(it) }
        playerView.player = viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.hlsUrl)
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        chatLayout.visibility = View.GONE

    }

    override fun onStop() {
        super.onStop()
        viewModel.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }



}
