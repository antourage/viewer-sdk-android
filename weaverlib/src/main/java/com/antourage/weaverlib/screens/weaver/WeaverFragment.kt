package com.antourage.weaverlib.screens.weaver

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.ui.ResizeWidthAnimation
import com.antourage.weaverlib.other.ui.keyboard.KeyboardEventListener
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.poll.PollDetailsFragment
import com.google.android.exoplayer2.Player
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.controller_header.*
import kotlinx.android.synthetic.main.fragment_poll_details.ivDismissPoll
import kotlinx.android.synthetic.main.fragment_weaver_portrait.*
import kotlinx.android.synthetic.main.layout_no_chat.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*
import kotlinx.android.synthetic.main.player_custom_control.*
import java.util.*

/**
 * Be careful not to create multiple instances of player
 * That way the sound will continue to go on after user exits fragment
 * Especially check method onNetworkGained
 */
class WeaverFragment : ChatFragment<WeaverViewModel>() {

    var wasDrawerClosed = false

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
                Player.STATE_READY -> {
                    hideLoading()
                    if (viewModel.isPlaybackPaused()) {
                        playerControls.show()
                    }
                }
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_IDLE -> {
                    if (!viewModel.wasStreamInitialized)
                        viewModel.onNetworkGained()
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
                is WeaverViewModel.ChatStatus.ChatTurnedOff -> {
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
                is WeaverViewModel.ChatStatus.ChatMessages -> {
                    rvMessages.visibility = View.VISIBLE
                    ll_wrapper.visibility = View.VISIBLE
                    llNoChat.visibility = View.GONE
                    etMessage.isEnabled = true
                }
                is WeaverViewModel.ChatStatus.ChatNoMessages -> {
                    etMessage.isEnabled = true
                    ll_wrapper.visibility = View.VISIBLE
                    rvMessages.visibility = View.INVISIBLE
                    val orientation = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        llNoChat.visibility = View.GONE
                    } else {
                        context?.let { context ->
                            ivNoChat.background =
                                ContextCompat.getDrawable(
                                    context,
                                    R.drawable.ic_chat_no_comments_yet
                                )
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
                is WeaverViewModel.PollStatus.NoPoll -> {
                    pollPopupLayout.visibility = View.GONE
                    llPollStatus.visibility = View.GONE
                }
                is WeaverViewModel.PollStatus.ActivePoll -> {
                    tvPollTitle.text = state.poll.question
                    pollPopupLayout.visibility = View.VISIBLE
                    llPollStatus.visibility = View.GONE
                }
                is WeaverViewModel.PollStatus.ActivePollDismissed -> {
                    pollPopupLayout.visibility = View.GONE
                    if (bottomLayout.visibility == View.GONE)
                        llPollStatus.visibility = View.VISIBLE
                    if (state.pollStatus != null)
                        txtPollStatus.text = state.pollStatus
                }
                is WeaverViewModel.PollStatus.PollDetails -> {
                    (activity as AntourageActivity).hideSoftKeyboard()
                    pollPopupLayout.visibility = View.GONE
                    llPollStatus.visibility = View.GONE
                    ll_wrapper.visibility = View.GONE
                    bottomLayout.visibility = View.VISIBLE
                    wasDrawerClosed = false
                    val orientation = resources.configuration.orientation
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        if (drawerLayout.isDrawerOpen(navView)) {
                            drawerLayout.closeDrawer(navView)
                            wasDrawerClosed = true
                        }
                    }
                    if (childFragmentManager.backStackEntryCount == 0)
                        replaceChildFragment(
                            PollDetailsFragment.newInstance(
                                arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.streamId,
                                state.pollId
                            ), R.id.bottomLayout, true
                        )
                    childFragmentManager.addOnBackStackChangedListener {
                        if ((childFragmentManager.findFragmentById(R.id.bottomLayout) !is PollDetailsFragment)) {
                            bottomLayout.visibility = View.GONE
                            ll_wrapper.visibility = View.VISIBLE
                            if (viewModel.currentPoll != null) {
                                llPollStatus.visibility = View.VISIBLE
                                viewModel.startNewPollCoundown()
                            }
                            if (wasDrawerClosed)
                                drawerLayout.openDrawer(navView)
                        }
                        etMessage.isEnabled =
                            !(childFragmentManager.findFragmentById(R.id.bottomLayout) is PollDetailsFragment)
                    }
                }
            }
        }
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            showLoading()
            viewModel.onNetworkGained()
        }
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, activity?.injector?.getWeaverViewModelFactory())
            .get(WeaverViewModel::class.java)
    }

    override fun subscribeToObservers() {
        super.subscribeToObservers()
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getPollStatusLiveData().observe(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getChatStatusLiveData().observe(this.viewLifecycleOwner, chatStateObserver)
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
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
            message.userID =
                FirebaseAuth.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName)).uid
            viewModel.addMessage(
                message,
                arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId!!
            )
            etMessage.setText("")
        }
        viewModel.initUi(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId)
        tvStreamName.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvBroadcastedBy.text =
            arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullName
        tvControllerStreamName.text =
            arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamTitle
        tvControllerBroadcastedBy.text =
            arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.creatorFullName
        if (context != null)
            tvWasLive.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
                ?.startTime?.parseDate(context!!)
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

        initKeyboardListener()
    }

    private fun initKeyboardListener() {
        KeyboardEventListener(activity as AppCompatActivity) {
            try {
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    context?.let { context ->
                        if (it) {
                            etMessage.requestFocus()
                            if (viewModel.getPollStatusLiveData().value is WeaverViewModel.PollStatus.ActivePollDismissed) {
                                llPollStatus.visibility = View.GONE
                            }
                            ivScreenSize.visibility = View.GONE
                            val anim = ResizeWidthAnimation(
                                ll_wrapper, getScreenWidth(activity as Activity)
                                        - dp2px(context, 8f).toInt()
                            )
                            anim.duration = 500
                            ll_wrapper.startAnimation(anim)
                        } else {
                            if (viewModel.getPollStatusLiveData().value is WeaverViewModel.PollStatus.ActivePollDismissed) {
                                llPollStatus.visibility = View.VISIBLE
                            }
                            ivScreenSize.visibility = View.VISIBLE
                            val anim = ResizeWidthAnimation(
                                ll_wrapper, dp2px(context, 300f).toInt()
                            )
                            anim.duration = 500
                            ll_wrapper.startAnimation(anim)
                        }
                    }
                }
            } catch (ex: IllegalStateException) {
                Log.d("Keyboard", "Fragment not attached to a context.")
            }
        }
    }

    override fun onControlsVisible() {
        if (context != null)
            tvWasLive.text = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
                ?.startTime?.parseDate(context!!)
    }

    private fun onPollDetailsClicked() {
        viewModel.seePollDetails()
    }

    private fun startPlayingStream() {
        playerView.player =
            viewModel.getExoPlayer(arguments?.getParcelable<StreamResponse>(ARGS_STREAM)!!.hlsUrl[0])
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
                ll_wrapper.background =
                    ContextCompat.getDrawable(context, R.drawable.rounded_semitransparent_bg)
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
        viewModel.getChatStatusLiveData().reObserve(this.viewLifecycleOwner, chatStateObserver)
        viewModel.getPollStatusLiveData().reObserve(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getPlaybackState().reObserve(this.viewLifecycleOwner, streamStateObserver)
        if (newOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (isChatDismissed) {
                (activity as AntourageActivity).hideSoftKeyboard()
                drawerLayout.closeDrawer(navView)
            }
        }
        ivScreenSize.visibility = View.VISIBLE
    }
}
