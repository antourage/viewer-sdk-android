package com.antourage.weaverlib.screens.weaver

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.antourage.weaverlib.Global
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
import com.google.firebase.Timestamp
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.broadcaster_header.*
import kotlinx.android.synthetic.main.dialog_user_authorization_portrait.*
import kotlinx.android.synthetic.main.fragment_player_live_video_portrait.*
import kotlinx.android.synthetic.main.fragment_player_live_video_portrait.divider
import kotlinx.android.synthetic.main.fragment_poll_details.ivDismissPoll
import kotlinx.android.synthetic.main.layout_empty_chat_placeholder.*
import kotlinx.android.synthetic.main.layout_poll_suggestion.*
import kotlinx.android.synthetic.main.player_custom_controls_live_video.*
import org.jetbrains.anko.sdk27.coroutines.onClick
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
        const val ARGS_USER_ID = "args_user_id"

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
                    ivFirstFrame.visibility = View.INVISIBLE
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
                    .placeholder(R.drawable.antourage_ic_user_grayed)
                    .error(R.drawable.antourage_ic_user_grayed)
                    .into(ivSetUserPhoto)
            }
        }
    }

    private val currentStreamInfoObserver: Observer<Boolean> = Observer { isStreamStillLive ->
        if (!isStreamStillLive) {
            showEndStreamUI()
        }
    }

    private val currentStreamViewersObserver: Observer<Int> = Observer { currentViewersCount ->
        updateViewersCountUI(currentViewersCount)
    }

    private fun updateViewersCountUI(currentViewersCount: Int?) {
        currentViewersCount?.let {
            txtNumberOfViewers.text = it.toString()
        }
    }

    private fun showEndStreamUI() {
        ivThanksForWatching?.visibility = View.VISIBLE
        txtLabelLive.visibility = View.GONE
        controls.visibility = View.GONE
        playerView.visibility = View.INVISIBLE
        ivIndividualCloseImage.visibility = View.VISIBLE
        ivIndividualCloseImage.onClick { onCloseClicked() }
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
                    showUserSettingsDialog(false)
                    bottomLayout.visibility = View.VISIBLE
                    wasDrawerClosed = false
                    if (orientation() == Configuration.ORIENTATION_LANDSCAPE) {
                        if (drawerLayout.isDrawerOpen(navView)) {
                            drawerLayout.closeDrawer(navView)
                            wasDrawerClosed = true
                        }
                    }
                    val streamId = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId
                    val userId = arguments?.getInt(ARGS_USER_ID)
                    if (streamId != null && userId != null) {
                        replaceChildFragment(
                            PollDetailsFragment.newInstance(
                                streamId,
                                state.pollId,
                                userId
                            ), R.id.bottomLayout, true
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

    private val networkStateObserver: Observer<NetworkConnectionState> = Observer { networkState ->
        if (networkState?.ordinal == NetworkConnectionState.AVAILABLE.ordinal) {
            showLoading()
            viewModel.onNetworkGained()
            playBtnPlaceholder.visibility = View.GONE
        } else if (networkState?.ordinal == NetworkConnectionState.LOST.ordinal) {
            if (!Global.networkAvailable) {
                context?.resources?.getString(R.string.ant_no_internet)
                    ?.let { messageToDisplay ->
                        Handler().postDelayed({
                            showWarningAlerter(messageToDisplay)
                            playBtnPlaceholder.visibility = View.VISIBLE
                        }, 500)
                    }
            }
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
        /**
        User id fot firebase synchronized with back end user id with messages;
         */
        message.userID = viewModel.getUser()?.id.toString()
        arguments?.getParcelable<StreamResponse>(ARGS_STREAM)?.streamId?.let { streamId ->
            viewModel.addMessage(
                message,
                streamId
            )
        }
        etMessage.setText("")
        rvMessages?.apply {
            adapter?.itemCount?.minus(0)
                ?.let { adapterPosition ->
                    post {
                        Handler().postDelayed(
                            {
                                layoutManager?.scrollToPosition(adapterPosition)
                            },
                            300
                        )
                    }
                }
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
        viewModel.getCurrentLiveStreamInfo()
            .observe(this.viewLifecycleOwner, currentStreamInfoObserver)
        viewModel.currentStreamViewsLiveData
            .observe(this.viewLifecycleOwner, currentStreamViewersObserver)
    }

    override fun initUi(view: View?) {
        super.initUi(view)
        setupUIForHidingKeyboardOnOutsideTouch(constraintLayoutParent)
        initUser()
        hidePollStatusLayout()
        constraintLayoutParent.loadLayoutDescription(R.xml.cl_states_player_live_video)
        startPlayingStream()

        playerView.setOnTouchListener(object : View.OnTouchListener {
            private val gestureDetector =
                GestureDetectorCompat(
                    context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                            handleControlsVisibility()
                            return super.onSingleTapConfirmed(e)
                        }
                    })

            override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
                gestureDetector.onTouchEvent(p1)
                return true
            }
        })

        initStreamInfo(arguments?.getParcelable(ARGS_STREAM))
        val streamResponse = arguments?.getParcelable<StreamResponse>(ARGS_STREAM)
        streamResponse?.apply {
            thumbnailUrl?.let {
                Picasso.get()
                    .load(thumbnailUrl)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(ivFirstFrame)
            }
        }
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
        hideKeyboard()
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
                        ContextCompat.getDrawable(
                            it,
                            R.drawable.antourage_rounded_semitransparent_bg
                        )
                    }
                txtPollStatus.visibility = View.VISIBLE
                divider.visibility = View.GONE
                if (!viewModel.isUserSettingsDialogShown) {
                    if (keyboardIsVisible) {
                        etMessage.requestFocus()
                    }
                }
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                userSettingsDialogUIToPortrait()
                if (viewModel.isUserSettingsDialogShown) {
                    showUserSettingsDialog(true)
                    if (keyboardIsVisible) {
                        etDisplayName.requestFocus()
                    }
                } else {
                    if (keyboardIsVisible) {
                        etMessage.requestFocus()
                    }
                }
                context?.let { ContextCompat.getColor(it, R.color.ant_bg_color) }?.let {
                    ll_wrapper.setBackgroundColor(it)
                }
                txtPollStatus.visibility = View.GONE
                divider.visibility = View.VISIBLE

                if (viewModel.getCurrentLiveStreamInfo().value == false) {
                    showEndStreamUI()
                }
            }
        }
        viewModel.getChatStatusLiveData().reObserve(this.viewLifecycleOwner, chatStateObserver)
        viewModel.getPollStatusLiveData().reObserve(this.viewLifecycleOwner, pollStateObserver)
        viewModel.getPlaybackState().reObserve(this.viewLifecycleOwner, streamStateObserver)

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
            R.drawable.antourage_ic_chat_no_comments_yet,
            R.string.ant_no_comments_yet
        )
        enableMessageInput(true)
        showMessageInput()
    }

    private fun disableChatUI() {
        setUpNoChatPlaceholder(
            R.drawable.antourage_ic_chat_off_layered,
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
            viewModel.initUi(streamId, id)
            streamId?.let { viewModel.setStreamId(it) }
            tvStreamName.text = streamTitle
            tvBroadcastedBy.text = creatorFullName
            tvControllerStreamName.text = streamTitle
            tvControllerBroadcastedBy.text = creatorFullName
            if (!broadcasterPicUrl.isNullOrEmpty()) {
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.antourage_ic_default_user)
                    .error(R.drawable.antourage_ic_default_user)
                    .into(ivUserPhoto)
                Picasso.get().load(broadcasterPicUrl)
                    .placeholder(R.drawable.antourage_ic_default_user)
                    .error(R.drawable.antourage_ic_default_user)
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
        showUserSettingsDialog(userSettingsDialog?.visibility == View.GONE)
    }

    private fun userSettingsDialogShown() =
        userSettingsDialog?.visibility == View.VISIBLE

    private fun showUserSettingsDialog(show: Boolean) {
        viewModel.isUserSettingsDialogShown = show
        if (show && !userSettingsDialogShown() || !show && userSettingsDialogShown()) {
            userSettingsDialog?.visibility =
                if (show) View.VISIBLE else View.GONE
            btnUserSettings.setImageResource(
                if (show) R.drawable.antourage_ic_user_settings_highlighted else R.drawable.antourage_ic_user_settings
            )
            if (show) {
                if (orientation() == Configuration.ORIENTATION_LANDSCAPE && keyboardIsVisible) {
                    userSettingsDialogUIToLandscape()
                } else {
                    userSettingsDialogUIToPortrait()
                }
                etMessage.isFocusable = false
                btnConfirm.isEnabled = false
                viewModel.getUser()?.imageUrl?.apply {
                    viewModel.newAvatar?.let {
                        ivSetUserPhoto.setImageBitmap(it)
                    } ?: run {
                        Picasso.get()
                            .load(this).fit().centerCrop()
                            .placeholder(R.drawable.antourage_ic_user_grayed)
                            .error(R.drawable.antourage_ic_user_grayed)
                            .into(ivSetUserPhoto)
                    }
                }
            } else {
                if (!viewModel.noDisplayNameSet()) {
                    etMessage.isFocusableInTouchMode = true
                    etMessage.isFocusable = true
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
            ivSetUserPhoto.setImageResource(R.drawable.antourage_ic_user_grayed)
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
        if (view !is EditText && view.id != btnCancel.id &&
            view.id != btnSend.id &&
            view.id != btnConfirm.id &&
            view.id != btnUserSettings.id
        ) {
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

    private fun userSettingsDialogUIToPortrait() {
        ivSetUserPhoto.removeConstraints(clUserSettings)
        etDisplayName.removeConstraints(clUserSettings)
        btnConfirm.removeConstraints(clUserSettings)
        btnCancel.removeConstraints(clUserSettings)

        userSettingsDialog.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        userSettingsDialog.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        clUserSettings.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        activity?.applicationContext?.apply {
            btnCancel.layoutParams.width = dp2px(this, 144f).roundToInt()
            btnConfirm.layoutParams.width = dp2px(this, 186f).roundToInt()
        }

        val constraintSet = ConstraintSet()
        constraintSet.clone(clUserSettings)

        constraintSet.connect(
            ivSetUserPhoto.id,
            ConstraintSet.LEFT,
            R.id.clUserSettings,
            ConstraintSet.LEFT
        )

        constraintSet.connect(
            ivSetUserPhoto.id,
            ConstraintSet.TOP,
            R.id.etDisplayName,
            ConstraintSet.TOP
        )

        constraintSet.connect(
            etDisplayName.id,
            ConstraintSet.BOTTOM,
            R.id.btnCancel,
            ConstraintSet.TOP
        )

        constraintSet.connect(
            etDisplayName.id,
            ConstraintSet.LEFT,
            R.id.ivSetUserPhoto,
            ConstraintSet.RIGHT
        )

        constraintSet.connect(
            etDisplayName.id,
            ConstraintSet.RIGHT,
            ((R.id.clUserSettings)),
            ConstraintSet.RIGHT
        )

        constraintSet.connect(
            etDisplayName.id,
            ConstraintSet.TOP,
            ((R.id.clUserSettings)),
            ConstraintSet.TOP
        )

        constraintSet.connect(
            btnCancel.id,
            ConstraintSet.BOTTOM,
            ((R.id.clUserSettings)),
            ConstraintSet.BOTTOM
        )

        constraintSet.connect(
            btnCancel.id,
            ConstraintSet.LEFT,
            ((R.id.clUserSettings)),
            ConstraintSet.LEFT
        )

        constraintSet.connect(
            btnCancel.id,
            ConstraintSet.RIGHT,
            (R.id.btnConfirm),
            ConstraintSet.LEFT
        )

        constraintSet.connect(
            btnConfirm.id,
            ConstraintSet.BOTTOM,
            R.id.clUserSettings,
            ConstraintSet.BOTTOM
        )

        constraintSet.connect(
            btnConfirm.id,
            ConstraintSet.LEFT,
            R.id.btnCancel,
            ConstraintSet.RIGHT
        )

        constraintSet.connect(
            btnConfirm.id,
            ConstraintSet.RIGHT,
            R.id.clUserSettings,
            ConstraintSet.RIGHT
        )

        constraintSet.applyTo(clUserSettings)

        ivSetUserPhoto.margin(left = 20f)
        etDisplayName.margin(top = 40f, bottom = 20f, left = 20f, right = 20f)
        btnCancel.margin(bottom = 20f, left = 20f)
        btnConfirm.margin(bottom = 20f, left = 20f, right = 20f)
    }

    override fun onHideKeyboard(keyboardHeight: Int) {
        super.onHideKeyboard(keyboardHeight)
        userSettingsDialogUIToPortrait()
    }

    override fun onShowKeyboard(keyboardHeight: Int) {
        super.onShowKeyboard(keyboardHeight)
        if (etDisplayName.isFocused && orientation() == Configuration.ORIENTATION_LANDSCAPE) {
            userSettingsDialogUIToLandscape()
        }
    }

    private fun userSettingsDialogUIToLandscape() {
        userSettingsDialog.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        userSettingsDialog.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        clUserSettings.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        activity?.applicationContext?.apply {
            btnCancel.layoutParams.width = dp2px(this, 120f).roundToInt()
            btnConfirm.layoutParams.width = dp2px(this, 140f).roundToInt()
        }
        val constraintSet = ConstraintSet()
        constraintSet.clone(clUserSettings)
        constraintSet.connect(
            R.id.ivSetUserPhoto,
            ConstraintSet.BOTTOM,
            R.id.etDisplayName,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.connect(
            R.id.etDisplayName,
            ConstraintSet.BOTTOM,
            R.id.clUserSettings,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.connect(
            R.id.etDisplayName,
            ConstraintSet.RIGHT,
            R.id.btnCancel,
            ConstraintSet.LEFT,
            0
        )
        constraintSet.connect(
            R.id.btnCancel,
            ConstraintSet.BOTTOM,
            R.id.etDisplayName,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.connect(
            R.id.btnCancel,
            ConstraintSet.LEFT,
            R.id.etDisplayName,
            ConstraintSet.RIGHT,
            0
        )
        constraintSet.connect(
            R.id.btnCancel,
            ConstraintSet.TOP,
            R.id.etDisplayName,
            ConstraintSet.TOP,
            0
        )
        constraintSet.connect(
            R.id.btnConfirm,
            ConstraintSet.BOTTOM,
            R.id.btnCancel,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.connect(
            R.id.btnConfirm,
            ConstraintSet.TOP,
            R.id.btnCancel,
            ConstraintSet.TOP,
            0
        )

        etDisplayName.margin(top = 20f)
        btnCancel.margin(bottom = 0f)

        constraintSet.applyTo(clUserSettings)
    }
}
