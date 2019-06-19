package com.antourage.weaverlib.screens.weaver


import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.parseDate
import com.antourage.weaverlib.other.reobserve
import com.antourage.weaverlib.other.replaceChildFragment
import com.antourage.weaverlib.other.ui.keyboard.KeyboardEventListener
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.poll.PollDetailsFragment
import com.google.android.exoplayer2.Player
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.fragment_poll_details.ivDismissPoll
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.layout_no_chat.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*
import kotlinx.android.synthetic.main.player_custom_control.*
import java.util.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView


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

    private val chatStateObserver: Observer<WeaverViewModel.ChatStatus> = Observer { state ->
        if (state != null)
            when (state) {
                is WeaverViewModel.ChatStatus.CHAT_TURNED_OFF -> {
                    etMessage.isEnabled = false
                    ll_wrapper.visibility = View.INVISIBLE
                    val orientation = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        llNoChat.visibility = View.GONE
                    } else {
                        context?.let { context ->
                            ivNoChat.background =
                                ContextCompat.getDrawable(context, R.drawable.ic_chat_off_layered)
                        }
                        txtNoChat.text = getString(R.string.commenting_off)
                        if (rvMessages.adapter?.itemCount == 0)
                            llNoChat.visibility = View.VISIBLE
                    }
                }
                is WeaverViewModel.ChatStatus.CHAT_MESSAGES -> {
                    rvMessages.visibility = View.VISIBLE
                    ll_wrapper.visibility = View.VISIBLE
                    llNoChat.visibility = View.GONE
                    etMessage.isEnabled = true
                }
                is WeaverViewModel.ChatStatus.CHAT_NO_MESSAGES -> {
                    etMessage.isEnabled = true
                    ll_wrapper.visibility = View.VISIBLE
                    rvMessages.visibility = View.INVISIBLE
                    val orientation = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        llNoChat.visibility = View.GONE
                    } else {
                        context?.let { context ->
                            ivNoChat.background =
                                ContextCompat.getDrawable(context, R.drawable.ic_chat_no_comments_yet)
                        }
                        txtNoChat.text = getString(R.string.no_comments_yet)
                        llNoChat.visibility = View.VISIBLE
                    }
                }
            }
    }
    private val pollStateObserver: Observer<WeaverViewModel.PollStatus> = Observer { state ->
        if (state != null) {
            when (state) {
                is WeaverViewModel.PollStatus.NO_POLL -> {
                    pollPopupLayout.visibility = View.GONE
                    llPollStatus.visibility = View.GONE
                }
                is WeaverViewModel.PollStatus.ACTIVE_POLL -> {
                    tvPollTitle.text = state.poll.question
                    pollPopupLayout.visibility = View.VISIBLE
                    llPollStatus.visibility = View.GONE
                }
                is WeaverViewModel.PollStatus.ACTIVE_POLL_DISMISSED -> {
                    pollPopupLayout.visibility = View.GONE
                    if (bottomLayout.visibility == View.GONE)
                        llPollStatus.visibility = View.VISIBLE
                    if (state.pollStatus != null)
                        txtPollStatus.text = state.pollStatus
                }
                is WeaverViewModel.PollStatus.POLL_DETAILS -> {
                    pollPopupLayout.visibility = View.GONE
                    llPollStatus.visibility = View.GONE
                    bottomLayout.visibility = View.VISIBLE
                    val orientation = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        drawerLayout.closeDrawer(navView)
                    }
                    if (childFragmentManager.backStackEntryCount == 0)
                        replaceChildFragment(
                            PollDetailsFragment.newInstance(
                                arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.streamId,
                                state.pollId
                            ), R.id.bottomLayout, true
                        )
                    childFragmentManager.addOnBackStackChangedListener {
                        if (!(childFragmentManager.findFragmentById(R.id.bottomLayout) is PollDetailsFragment)) {
                            bottomLayout.visibility = View.GONE
                            llPollStatus.visibility = View.VISIBLE
                            drawerLayout.openDrawer(navView)
                            viewModel.startNewPollCoundown()
                        }
                        etMessage.isEnabled =
                            !(childFragmentManager.findFragmentById(R.id.bottomLayout) is PollDetailsFragment)
                    }
                }
            }
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
        viewModel.getPollStatusLiveData().observe(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getChatStatusLiveData().observe(this.viewLifecycleOwner, chatStateObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        llPollStatus.visibility = View.GONE
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_screen)
        startPlayingStream()

        btnSend.setOnClickListener {
            val message = Message(
                "", "osoluk@leobit.co", "my nic", etMessage.text.toString(),
                MessageType.USER, Timestamp(Date())
            )
            message.userID = FirebaseAuth.getInstance().uid
            viewModel.addMessage(message, arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId!!)
            etMessage.setText("")
            (activity as AntourageActivity).hideSoftKeyboard()
        }
        viewModel.initUi(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId)
        tvStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvBroadcastedBy.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        tvControllerStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvControllerBroadcastedBy.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullname
        if (context != null)
            tvWasLive.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.startTime?.parseDate(context!!)
        ivDismissPoll.setOnClickListener {
            viewModel.startNewPollCoundown()
        }
        llPollStatus.setOnClickListener {
            onPollDetailsClicked()
        }
        pollPopupLayout.setOnClickListener {
            playerControls.hide()
            onPollDetailsClicked()
        }
        //initKeyboardListener()
    }

    private fun initKeyboardListener() {
        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastVisiblePosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                //Toast.makeText(context, "messages scrolled", Toast.LENGTH_LONG).show()
            }
        })
        KeyboardEventListener(activity as AppCompatActivity) {
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (it) {
                    Toast.makeText(context, "Keyboard Open", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Keyboard closed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onControlsVisible() {
        if (context != null)
            tvWasLive.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.startTime?.parseDate(context!!)
    }

    private fun onPollDetailsClicked() {
        viewModel.seePollDetails()
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
            context?.let { context ->
                ll_wrapper.background = ContextCompat.getDrawable(context, R.drawable.rounded_semitransparent_bg)
                txtPollStatus.visibility = View.VISIBLE
                devider.visibility = View.GONE

            }
        } else if (newOrientation == Configuration.ORIENTATION_PORTRAIT) {
            context?.let { context ->
                ll_wrapper.setBackgroundColor(ContextCompat.getColor(context, R.color.bg_color))
                txtPollStatus.visibility = View.GONE
                devider.visibility = View.VISIBLE
            }
        }
        viewModel.getPollStatusLiveData().reobserve(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getChatStatusLiveData().reobserve(this.viewLifecycleOwner, chatStateObserver)
        viewModel.getPlaybackState().reobserve(this.viewLifecycleOwner, streamStateObserver)
        btnSend.visibility = View.VISIBLE
    }

    override fun onNetworkConnectionAvailable() {
        showLoading()
        startPlayingStream()

    }
}
