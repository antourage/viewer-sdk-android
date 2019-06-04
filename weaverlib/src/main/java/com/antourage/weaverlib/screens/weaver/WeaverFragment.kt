package com.antourage.weaverlib.screens.weaver


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.dp2px
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.replaceChildFragment
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.poll.PollDetailsFragment
import com.google.android.exoplayer2.Player
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.fragment_poll_details.ivDismissPoll
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*
import android.widget.Toast
import android.view.ViewTreeObserver.OnGlobalLayoutListener


class WeaverFragment : ChatFragment<WeaverViewModel>() {


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
        super.subscribeToObservers()
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getPollLiveData().observe(this.viewLifecycleOwner,pollObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_screen)
        startPlayingStream()

        btnSend.setOnClickListener {
            viewModel.addMessage(etMessage.text.toString(),"my nic")
            etMessage.setText("")
        }
        tvStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvBroadcastedBy.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        tvControllerStreamName.text =  arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvControllerBroadcastedBy.text =  arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        ivDismissPoll.setOnClickListener{
            pollPopupLayout.visibility = View.GONE
        }
        etMessage.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus && resources.configuration.orientation ==  Configuration.ORIENTATION_LANDSCAPE){
                etMessage.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            } else{
                val layoutParam = etMessage.layoutParams
                layoutParam.width = dp2px(context!!,300f).toInt()
                etMessage.layoutParams = layoutParam
            }
        }
        pollPopupLayout.setOnClickListener {
            pollPopupLayout.visibility = View.GONE
            replaceChildFragment(PollDetailsFragment.newInstance(),R.id.bottomLayout,true)
            childFragmentManager.addOnBackStackChangedListener {
                if((childFragmentManager.findFragmentById(R.id.bottomLayout) is PollDetailsFragment)){
                    headerLayout.visibility = View.GONE
                    etMessage.isEnabled = false
                } else{
                    headerLayout.visibility = View.VISIBLE
                    etMessage.isEnabled = true
                }
            }
        }
    }

    private fun startPlayingStream() {
        playerView.player = viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.hlsUrl)
        playerControls.player = playerView.player
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            context?.let {context ->
                ll_wrapper.background = ContextCompat.getDrawable(context, R.drawable.rounded_semitransparent_bg)

            }
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            context?.let {context ->
                ll_wrapper.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_color))
            }
        }
    }

    var isOpened = false


}
