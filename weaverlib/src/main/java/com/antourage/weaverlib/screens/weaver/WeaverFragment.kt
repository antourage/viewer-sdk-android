package com.antourage.weaverlib.screens.weaver


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.View
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceChildFragment
import com.antourage.weaverlib.screens.base.streaming.StreamingFragment
import com.antourage.weaverlib.screens.chat.ChatFragment
import com.antourage.weaverlib.screens.poll.PollDetailsFragment
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*

class WeaverFragment : StreamingFragment<WeaverViewModel>() {


    companion object {
        const val ARGS_STREAM = "args_stream"

        fun newInstance(stream: StreamResponse): WeaverFragment {
            val bundle = Bundle()
            bundle.putParcelable(ARGS_STREAM, stream)
            val fragment = WeaverFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.fragment_weaver_portrait
    }

    //region Observers

    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_READY -> hideLoading()
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_IDLE -> {
                    if (!viewModel.wasStreamInitialized)
                        startPlayingStream()
                    else
                        hideLoading()
                }
                Player.STATE_ENDED -> {
                    viewModel.removeStatisticsListeners()
                    hideLoading()
                }
            }
    }
    private val pollObserver: Observer<Poll> = Observer { poll ->
        if (poll != null) {
            pollPopupLayout.visibility = View.VISIBLE
            tvPollTitle.text = poll.pollQuestion
        }

    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(WeaverViewModel::class.java)
    }

    override fun subscribeToObservers() {
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getPollLiveData().observe(this.viewLifecycleOwner,pollObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_screen)
        startPlayingStream()

        replaceChildFragment(
            ChatFragment.newInstance(
                arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId!!,
                arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.isLive!!
            ), R.id.chatLayout
        )

        tvStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvBroadcastedBy.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        tvControllerStreamName.text =  arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvControllerBroadcastedBy.text =  arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        ivDismissPoll.setOnClickListener{
            pollPopupLayout.visibility = View.GONE
        }
        pollPopupLayout.setOnClickListener {
            headerLayout.visibility = View.GONE
            pollPopupLayout.visibility = View.GONE
            replaceChildFragment(PollDetailsFragment.newInstance(),R.id.chatLayout,true)
            childFragmentManager.addOnBackStackChangedListener {
                if(childFragmentManager.findFragmentById(R.id.chatLayout) is ChatFragment){
                    headerLayout.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startPlayingStream() {
        playerView.player = viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.hlsUrl)
    }


    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }


}
