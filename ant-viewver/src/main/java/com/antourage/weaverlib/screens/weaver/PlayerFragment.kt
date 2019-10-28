package com.antourage.weaverlib.screens.weaver

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import com.antourage.weaverlib.di.injector
import com.antourage.weaverlib.other.*
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.User
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.NetworkConnectionState
import com.antourage.weaverlib.other.ui.AvatarChooser
import com.antourage.weaverlib.other.ui.ResizeWidthAnimation
import com.antourage.weaverlib.other.ui.keyboard.KeyboardEventListener
import com.antourage.weaverlib.other.ui.keyboard.convertDpToPx
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.chat.ChatFragment
import com.antourage.weaverlib.screens.poll.PollDetailsFragment
import com.google.android.exoplayer2.Player
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.broadcaster_header.*
import kotlinx.android.synthetic.main.dialog_user_authorization.*
import kotlinx.android.synthetic.main.fragment_player_live_video_portrait.*
import kotlinx.android.synthetic.main.fragment_poll_details.ivDismissPoll
import kotlinx.android.synthetic.main.layout_empty_chat_placeholder.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*
import kotlinx.android.synthetic.main.player_custom_controls_live_video.*
import java.util.*
import kotlin.math.roundToInt

/**
 * Be careful not to create multiple instances of player
 * That way the sound will continue to go on after user exits fragment
 * Especially check method onNetworkGained
 */
internal class PlayerFragment : ChatFragment<PlayerViewModel>() {

    private var wasDrawerClosed = false

    companion object {
        const val ARGS_STREAM = "args_stream"

        fun newInstance(stream: StreamResponse): PlayerFragment {
            val fragment = PlayerFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARGS_STREAM, stream)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun getLayoutId() = R.layout.fragment_player_live_video_portrait

    private val avatarChooser by lazy { context?.let { AvatarChooser(it) } }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        avatarChooser?.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        avatarChooser?.onActivityResult(requestCode, resultCode, data)
    }

    //region Observers
    private val streamStateObserver: Observer<Int> = Observer { state ->
        if (ivLoader != null)
            when (state) {
                Player.STATE_BUFFERING -> showLoading()
                Player.STATE_READY -> {
                    hideLoading()
                    if (viewModel.isPlaybackPaused()) {
                        playerControls.show()
                    }
                }
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

    private val chatStateObserver: Observer<ChatStatus> = Observer { state ->
        if (state != null)
            when (state) {
                is ChatStatus.ChatTurnedOff -> {
                    disableChatUI()
                    hideRvMessages()
                    showChatTurnedOffPlaceholder(orientation() != Configuration.ORIENTATION_LANDSCAPE)
                }
                is ChatStatus.ChatMessages -> {
                    enableChatUI()
                    showRvMessages()
                    showChatTurnedOffPlaceholder(false)
                }
                is ChatStatus.ChatNoMessages -> {
                    enableChatUI()
                    hideRvMessages()
                    showChatTurnedOffPlaceholder(orientation() != Configuration.ORIENTATION_LANDSCAPE)
                }
            }
    }

    private val userInfoObserver: Observer<User> = Observer { user ->
        user?.apply {
            if (!viewModel.noDisplayNameSet()) {
                etMessage.isFocusable = true
                etMessage.isFocusableInTouchMode = true
                etDisplayName.setText(displayName)
            }
            viewModel.newAvatar?.let {
                ivSetUserPhoto.setImageBitmap(it)
            } ?: run {
                Picasso.get()
                    .load(imageUrl).fit().centerCrop()
                    .placeholder(R.drawable.ic_user_grayed)
                    .error(R.drawable.ic_user_grayed)
                    .into(ivSetUserPhoto)
            }
        }
    }


    private val pollStateObserver: Observer<PollStatus> = Observer { state ->
        if (state != null) {
            when (state) {
                is PollStatus.NoPoll -> {
                    hidePollPopup()
                    hidePollStatusLayout()
                    bottomLayout.visibility = View.GONE
                }
                is PollStatus.ActivePoll -> {
                    tvPollTitle.text = state.poll.question
                    showPollPopup()
                    hidePollStatusLayout()
                }
                is PollStatus.ActivePollDismissed -> {
                    hidePollPopup()
                    if (bottomLayout.visibility == View.GONE)
                        showPollStatusLayout()
                    txtPollStatus.text = state.pollStatus?.let { it }
                }
                is PollStatus.PollDetails -> {
                    (activity as AntourageActivity).hideSoftKeyboard()
                    hidePollPopup()
                    hidePollStatusLayout()
                    removeMessageInput()
                    bottomLayout.visibility = View.VISIBLE
                    wasDrawerClosed = false
                    if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                        if (drawerLayout.isDrawerOpen(navView)) {
                            drawerLayout.closeDrawer(navView)
                            wasDrawerClosed = true
                        }
                    }
                    if (childFragmentManager.backStackEntryCount == 0)
                        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId?.let {
                            PollDetailsFragment.newInstance(
                                it,
                                state.pollId
                            )
                        }?.let {
                            replaceChildFragment(
                                it, R.id.bottomLayout, true
                            )
                        }
                    childFragmentManager.addOnBackStackChangedListener {
                        if ((childFragmentManager.findFragmentById(R.id.bottomLayout) !is PollDetailsFragment)) {
                            bottomLayout.visibility = View.GONE
                            if (viewModel.getChatStatusLiveData().value is ChatStatus.ChatTurnedOff)
                                ll_wrapper.visibility = View.INVISIBLE else ll_wrapper.visibility =
                                View.VISIBLE
                            if (viewModel.currentPoll != null) {
                                showPollStatusLayout()
                                viewModel.startNewPollCountdown()
                            }
                            if (wasDrawerClosed)
                                drawerLayout.openDrawer(navView)
                        }
                        enableMessageInput(childFragmentManager.findFragmentById(R.id.bottomLayout) !is PollDetailsFragment)
                    }
                }
            }
        }
    }

    private fun handlePollDetailsUI() {
        if ((childFragmentManager.findFragmentById(R.id.bottomLayout) !is PollDetailsFragment)) {
            bottomLayout.visibility = View.GONE
            if (viewModel.getChatStatusLiveData().value is ChatStatus.ChatTurnedOff)
                ll_wrapper.visibility = View.INVISIBLE else ll_wrapper.visibility =
                View.VISIBLE
            if (viewModel.currentPoll != null) {
                showPollStatusLayout()
                viewModel.startNewPollCountdown()
            }
            if (wasDrawerClosed)
                drawerLayout.openDrawer(navView)
        }
        enableMessageInput(childFragmentManager.findFragmentById(R.id.bottomLayout) !is PollDetailsFragment)
    }

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            showLoading()
            viewModel.onNetworkGained()
        }
    }
//endregion

    private val onBtnSendClicked = View.OnClickListener {
        val message = Message(
            viewModel.getUser()?.imageUrl ?: "",
            "osoluk@leobit.co",
            viewModel.getUser()?.displayName,
            etMessage.text.toString(),
            MessageType.USER,
            Timestamp(Date())
        )
        message.userID =
            FirebaseAuth.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName)).uid
        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId?.let { streamId ->
            viewModel.addMessage(
                message,
                streamId
            )
        }
        etMessage.setText("")
        rvMessages?.apply {
            adapter?.itemCount?.minus(1)
                ?.let { adapterPosition -> scrollToPosition(adapterPosition) }
        }
    }

    private val onUserSettingsClicked = View.OnClickListener {
        toggleUserSettingsDialog()
        if (!etMessage.isFocused)
            hideKeyboard()
    }

    private val onMessageETClicked = View.OnClickListener {
        checkIfRequireDisplayNameSetting()
    }

    private val onCancelClicked = View.OnClickListener {
        etDisplayName.setText(viewModel.getUser()?.displayName)
        toggleUserSettingsDialog()
        if (!etMessage.isFocused)
            hideKeyboard()
        viewModel.newAvatar = null
        viewModel.oldAvatar?.let {
            ivSetUserPhoto.setImageBitmap(it)
        }
    }

    private val onUserPhotoClicked = View.OnClickListener {
        showImagePickerDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this, activity?.injector?.getWeaverViewModelFactory())
            .get(PlayerViewModel::class.java)
    }

    override fun subscribeToObservers() {
        super.subscribeToObservers()
        viewModel.getPlaybackState().observe(this.viewLifecycleOwner, streamStateObserver)
        viewModel.getPollStatusLiveData().observe(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getChatStatusLiveData().observe(this.viewLifecycleOwner, chatStateObserver)
        viewModel.getUserInfoLiveData().observe(this.viewLifecycleOwner, userInfoObserver)
        ConnectionStateMonitor.internetStateLiveData.observe(
            this.viewLifecycleOwner,
            networkStateObserver
        )
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        setupUIForHidingKeyboardOnOutsideTouch(constraintLayoutParent)
        initUser()
        hidePollStatusLayout()
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_live_video)
        startPlayingStream()

        initStreamInfo(arguments?.getParcelable<StreamResponse>(ARGS_STREAM))
        initClickListeners()
        initKeyboardListener()
        initLabelLive()

        etDisplayName.afterTextChanged {
            btnConfirm.isEnabled =
                !etDisplayName.text.toString().isEmptyTrimmed() && !viewModel.getUser()?.displayName.equals(
                    it
                )
        }
    }

    /**
     * method used to position player controls in proper way according to "Live" label's location
     * as this label should not disappear with all the other player controls (so they are
     * located in different layouts, but at the same time
     * other views should depend ot "Live" label's size
     * */
    private fun initLabelLive() {
        txtLabelLive.post {
            val marginLeft =
                (txtLabelLive?.width ?: 0) + (context?.convertDpToPx(12f)?.roundToInt() ?: 0)
            val marginTop = (context?.convertDpToPx(10f)?.roundToInt() ?: 0)
            if (marginLeft > 0) {
                llTopLeftLabels.setMargins(marginLeft, marginTop, 0, 0)
            }
        }
    }

    private fun initKeyboardListener() {
        KeyboardEventListener(activity as AppCompatActivity) {
            try {
                if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                    context?.let { context ->
                        if (it) {
                            etMessage.requestFocus()
                            if (viewModel.getPollStatusLiveData().value is PollStatus.ActivePollDismissed) {
                                hidePollStatusLayout()
                            }
                            hideFullScreenIcon()
                            if (ll_wrapper.visibility == View.VISIBLE) {
                                val anim = ResizeWidthAnimation(
                                    ll_wrapper, getScreenWidth(activity as Activity)
                                            - dp2px(context, 8f).toInt()
                                )
                                anim.duration = 500
                                ll_wrapper.startAnimation(anim)
                            }
                        } else {
                            if (viewModel.getPollStatusLiveData().value is PollStatus.ActivePollDismissed) {
                                showPollStatusLayout()
                            }
                            showFullScreenIcon()
                            if (ll_wrapper.visibility == View.VISIBLE) {
                                val anim = ResizeWidthAnimation(
                                    ll_wrapper, dp2px(context, 300f).toInt()
                                )
                                anim.duration = 500
                                ll_wrapper.startAnimation(anim)
                            }
                        }
                    }
                }
            } catch (ex: IllegalStateException) {
                Log.d("Keyboard", "Fragment not attached to a context.")
            }
        }
    }

    override fun onControlsVisible() {
        setWasLiveText(context?.let {
            arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
                ?.startTime?.parseDate(it)
        })
    }

    private fun onPollDetailsClicked() {
        viewModel.seePollDetails()
    }

    private fun onConfirmClicked() {
        viewModel.changeUserDisplayName(etDisplayName.text.toString())
        if (viewModel.newAvatar != null) {
            viewModel.oldAvatar = viewModel.newAvatar
            viewModel.changeUserAvatar()
        }
        showUserSettingsDialog(false)
    }

    private fun startPlayingStream() {
        playerView.player =
            arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.hlsUrl?.get(
                0
            )?.let {
                viewModel.getExoPlayer(
                    it
                )
            }
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

//    override fun onStop() {
//        super.onStop()
//        viewModel.onPause()
//    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.releasePlayer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val newOrientation = newConfig.orientation
        when (newOrientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                ll_wrapper.background =
                    context?.let {
                        ContextCompat.getDrawable(it, R.drawable.rounded_semitransparent_bg)
                    }
                txtPollStatus.visibility = View.VISIBLE
                divider.visibility = View.GONE
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                context?.let { ContextCompat.getColor(it, R.color.ant_bg_color) }?.let {
                    ll_wrapper.setBackgroundColor(it)
                }
                txtPollStatus.visibility = View.GONE
                divider.visibility = View.VISIBLE
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
        showFullScreenIcon()
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
            R.string.ant_no_comments_yet
        )
        enableMessageInput(true)
        showMessageInput()
    }

    private fun disableChatUI() {
        setUpNoChatPlaceholder(
            R.drawable.ic_chat_off_layered,
            R.string.ant_commenting_off
        )
        enableMessageInput(false)
        hideMessageInput()
    }

    //endregion

    //region polUI helper func
    private fun showPollStatusLayout() {
        llPollStatus.visibility = View.VISIBLE
    }

    private fun hidePollStatusLayout() {
        llPollStatus.visibility = View.GONE
    }

    private fun showPollPopup() {
        pollPopupLayout.visibility = View.VISIBLE
    }

    private fun hidePollPopup() {
        pollPopupLayout.visibility = View.GONE
    }
    //endregion

    private fun initStreamInfo(streamResponse: StreamResponse?) {
        streamResponse?.apply {
            viewModel.initUi(streamId)
            streamId?.let { viewModel.setStreamId(it) }
            tvStreamName.text = streamTitle
            tvBroadcastedBy.text = creatorFullName
            tvControllerStreamName.text = streamTitle
            tvControllerBroadcastedBy.text = creatorFullName
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
            txtNumberOfViewers.text = viewersCount.toString()
            setWasLiveText(context?.let { startTime?.parseDate(it) })
        }
    }

    private fun initClickListeners() {
        btnSend.setOnClickListener(onBtnSendClicked)
        etMessage.setOnClickListener(onMessageETClicked)
        btnUserSettings.setOnClickListener(onUserSettingsClicked)
        btnCancel.setOnClickListener(onCancelClicked)
        ivSetUserPhoto.setOnClickListener(onUserPhotoClicked)
        avatarChooser?.setListener {
            setNewAvatar(it)
            btnConfirm.isEnabled = true
        }
        ivDismissPoll.setOnClickListener { viewModel.startNewPollCountdown() }
        llPollStatus.setOnClickListener { onPollDetailsClicked() }
        btnConfirm.setOnClickListener { onConfirmClicked() }
        pollPopupLayout.setOnClickListener {
            playerControls.hide()
            onPollDetailsClicked()
        }
    }

    private fun showFullScreenIcon() {
        ivScreenSize.visibility = View.VISIBLE
    }

    private fun hideFullScreenIcon() {
        ivScreenSize.visibility = View.GONE
    }

    private fun setWasLiveText(text: String?) {
        if (text != null && text.isNotEmpty()) {
            tvWasLive.text = text
            tvWasLive.visibility = View.VISIBLE
        } else {
            tvWasLive.visibility = View.GONE
        }
    }

    private fun toggleUserSettingsDialog() {
        showUserSettingsDialog(clUserSettings.visibility == View.GONE)
    }

    private fun userSettingsDialogShown() = clUserSettings.visibility == View.VISIBLE

    private fun showUserSettingsDialog(show: Boolean) {
        if (show && !userSettingsDialogShown() || !show && userSettingsDialogShown()) {
            clUserSettings.visibility =
                if (show) View.VISIBLE else View.GONE
            btnUserSettings.setImageResource(
                if (show) R.drawable.ic_user_settings_highlighted else R.drawable.ic_user_settings
            )
            if (show) {
                etMessage.isFocusable = false
                if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                    activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
                }
                btnConfirm.isEnabled = false
                viewModel.getUser()?.imageUrl?.apply {
                    viewModel.newAvatar?.let {
                        ivSetUserPhoto.setImageBitmap(it)
                    } ?: run {
                        Picasso.get()
                            .load(this).fit().centerCrop()
                            .placeholder(R.drawable.ic_user_grayed)
                            .error(R.drawable.ic_user_grayed)
                            .into(ivSetUserPhoto)
                    }
                }
            } else {
                if (!viewModel.noDisplayNameSet()) {
                    etMessage.isFocusableInTouchMode = true
                    etMessage.isFocusable = true
                }
                if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                    activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
                } else {
                    activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        if (viewModel.avatarDeleted || viewModel.profileInfo?.imagePath == null) {
            avatarChooser?.showChoose(this)
        } else {
            avatarChooser?.showChooseDelete(this)
        }
    }

    private fun setNewAvatar(it: Bitmap?) {
        it?.let {
            ivSetUserPhoto.setImageBitmap(it)
            viewModel.onAvatarChanged(it)
        } ?: run {
            ivSetUserPhoto.setImageResource(R.drawable.ic_user_grayed)
            viewModel.onAvatarDeleted()
        }
    }

    private fun initUser() {
        viewModel.initUser()
    }

    private fun checkIfRequireDisplayNameSetting() {
        if (viewModel.noDisplayNameSet()) {
            showUserSettingsDialog(true)
        }
    }

    private fun setupUIForHidingKeyboardOnOutsideTouch(view: View) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (view !is EditText && view.id != btnCancel.id && view.id != btnSend.id) {
            view.setOnTouchListener { v, event ->
                hideKeyboard()
                false
            }
        }

        //If a layout container, iterate over children and seed recursion.
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUIForHidingKeyboardOnOutsideTouch(innerView)
            }
        }
    }
}
