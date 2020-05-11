package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.R
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.reObserveForever
import com.antourage.weaverlib.other.toMultipart
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import javax.inject.Inject

internal class PlayerViewModel @Inject constructor(application: Application) :
    ChatViewModel(application) {

    companion object {
        const val NEW_POLL_DELAY_MS = 15000L
        const val CLOSE_EXPANDED_POLL_DELAY_MS = 6000L
    }

    var wasStreamInitialized = false
    private var isChatTurnedOn = false
    private var postAnsweredUsers = false
    private var user: User? = null

    private var messagesResponse: QuerySnapshotLiveData<Message>? = null

    private val pollStatusLiveData: MutableLiveData<PollStatus> = MutableLiveData()
    private val chatStatusLiveData: MutableLiveData<ChatStatus> = MutableLiveData()
    private val userInfoLiveData: MutableLiveData<User> = MutableLiveData()
    private val loaderLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private val isCurrentStreamStillLiveLiveData: MutableLiveData<Boolean> = MutableLiveData()

    internal var isUserSettingsDialogShown = false

    internal var currentPoll: Poll? = null

    var newAvatar: Bitmap? = null
    var oldAvatar: Bitmap? = null
    var avatarDeleted = false
    var profileInfo: UserInfo? = null

    internal fun getPollStatusLiveData(): LiveData<PollStatus> = pollStatusLiveData
    fun getChatStatusLiveData(): LiveData<ChatStatus> = chatStatusLiveData
    fun getUserInfoLiveData(): LiveData<User> = userInfoLiveData
    fun getLoaderLiveData(): LiveData<Boolean> = loaderLiveData
    fun getCurrentLiveStreamInfo() = isCurrentStreamStillLiveLiveData
    fun getUser() = user

    init {
        pollStatusLiveData.value = PollStatus.NoPoll
        postAnsweredUsers = false
    }

    private val messagesObserver: Observer<Resource<List<Message>>> = Observer { resource ->
        resource?.status?.let {
            if (it is Status.Success && it.data != null && isChatTurnedOn) {
                if (chatContainsNonStatusMsg(it.data)) {
                    chatStatusLiveData.postValue(ChatStatus.ChatMessages)
                    messagesLiveData.value = it.data
                    user?.apply {
                        displayName?.let { displayName ->
                            changeDisplayNameForAllMessagesLocally(displayName)
                        }
                        imageUrl?.let { avatarUrl ->
                            changeAvatarForAllMessagesLocally(avatarUrl)
                        }
                    }
                } else {
                    chatStatusLiveData.postValue(ChatStatus.ChatNoMessages)
                }
            }
        }
    }

    private val activePollObserver: Observer<Resource<List<Poll>>> = Observer { resource ->
        resource?.status?.let {
            when (it) {
                is Status.Success -> {
                    if (it.data != null && it.data.isNotEmpty()) {
                        postAnsweredUsers = false
                        if (UserCache.getInstance(getApplication())?.getCollapsedPollId().equals(it.data[0].id)) {
                            //todo: wtf?
                            val pollStatusText =
                                if (postAnsweredUsers) "2 answers" else getApplication<Application>().getString(
                                    R.string.ant_new_poll
                                )
                            pollStatusLiveData.postValue(
                                PollStatus.ActivePollDismissed(pollStatusText)
                            )
                        } else {
                            pollStatusLiveData.postValue(PollStatus.ActivePoll(it.data[0]))
                        }
                        currentPoll = it.data[0]
                    } else {
                        postAnsweredUsers = false
                        pollStatusLiveData.postValue(PollStatus.NoPoll)
                        currentPoll = null
                    }
                }
                is Status.Failure -> {
                    error.value = it.errorMessage
                }
            }
        }
    }

    private val streamObserver: Observer<Resource<Stream>> = Observer { resource ->
        resource?.status?.let {
            if (it is Status.Success && it.data != null) {
                isChatTurnedOn = it.data.isChatActive
                if (!isChatTurnedOn) {
                    messagesResponse?.removeObserver(messagesObserver)
                    chatStatusLiveData.postValue(ChatStatus.ChatTurnedOff)
                } else {
                    streamId?.let { it1 ->
                        messagesResponse = Repository.getMessages(it1)
                        messagesResponse?.observeForever(messagesObserver)
                    }
                }
            }
        }
    }

    fun initUi(streamId: Int?, currentlyWatchedVideoId: Int?) {
        streamId?.let {
            this.streamId = it
            Repository.getPoll(streamId).observeForever(activePollObserver)
            Repository.getStream(streamId).observeForever(streamObserver)
        }

        currentlyWatchedVideoId?.let {
            this.currentlyWatchedVideoId = it
        }
    }

    fun onAvatarChanged(it: Bitmap) {
        avatarDeleted = false
        newAvatar = it
    }

    fun onAvatarDeleted() {
        avatarDeleted = true
        newAvatar = null
    }

    fun seePollDetails() {
        currentPoll?.id?.let { pollId ->
            UserCache.getInstance(getApplication())?.saveCollapsedPoll(pollId)
        }
        currentPoll?.let {
            pollStatusLiveData.value = (PollStatus.PollDetails(it.id))
        }
    }

    fun startNewPollCountdown() {
        currentPoll?.id?.let { pollId ->
            UserCache.getInstance(getApplication())?.saveCollapsedPoll(pollId)
        }
        observeAnsweredUsers()
        Handler().postDelayed(
            {
                postAnsweredUsers = true
                observeAnsweredUsers()
            },
            NEW_POLL_DELAY_MS
        )
    }

    private fun wasAnswered(answeredUsers: List<AnsweredUser>): Boolean {
        for (answeredUser in answeredUsers) {
            if (answeredUser.id == user?.id.toString()) {
                return true
            }
        }
        return false
    }

    private fun observeAnsweredUsers() {
        currentPoll?.let { poll ->
            streamId?.let { streamId ->
                Repository.getAnsweredUsers(streamId, poll.id).observeForever { resource ->
                    resource?.status?.let {
                        if (it is Status.Success && it.data != null) {
                            if (wasAnswered(it.data)) {
                                postAnsweredUsers = true
                            }
                            if (postAnsweredUsers && it.data.isNotEmpty())
                                pollStatusLiveData.postValue(
                                    PollStatus.ActivePollDismissed(
                                        getApplication<Application>().resources.getQuantityString(
                                            R.plurals.ant_number_answers,
                                            it.data.size,
                                            it.data.size
                                        )
                                    )
                                )
                            else
                                pollStatusLiveData.postValue(
                                    PollStatus.ActivePollDismissed(
                                        getApplication<Application>().getString(
                                            R.string.ant_new_poll
                                        )
                                    )
                                )
                        }
                    }
                }
            }
        }
    }

    fun markActivePollDismissed(){
        pollStatusLiveData.postValue(
            PollStatus.ActivePollDismissed(
                getApplication<Application>().getString(
                    R.string.ant_new_poll
                )
            )
        )
    }

    internal fun addMessage(message: Message, streamId: Int) {
        if (message.text != null && message.text!!.isNotEmpty() && !message.text!!.isBlank()) {
            val temp: MutableList<Message> = (messagesLiveData.value)!!.toMutableList()
            temp.add(message)
            Repository.addMessage(message, streamId)
        }
    }

    override fun checkIfMessageByUser(userID: String?): Boolean {
        return if (userID == null){
            false
        } else {
            getUser()?.id?.toString() == userID
        }
    }

    override fun onStreamStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            wasStreamInitialized = true
        }
    }

    override fun getMediaSource(streamUrl: String?): MediaSource? {
        val defaultBandwidthMeter = DefaultBandwidthMeter.Builder(getApplication()).build()
        val dataSourceFactory = DefaultDataSourceFactory(
            getApplication(),
            Util.getUserAgent(getApplication(), "Exo2"), defaultBandwidthMeter
        )
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(streamUrl))
    }

    override fun onResume() {
        super.onResume()
        player?.seekTo(player?.duration ?: 0)
        messagesResponse?.reObserveForever(messagesObserver)
    }

    override fun onPause() {
        super.onPause()
        messagesResponse?.removeObserver(messagesObserver)
    }

    override fun onLiveStreamEnded() {
        super.onLiveStreamEnded()
        isCurrentStreamStillLiveLiveData.postValue(false)
    }

    fun initUser() {
        val response = Repository.getUser()
        response.observeForever(object : Observer<Resource<User>> {
            override fun onChanged(it: Resource<User>?) {
                when (val responseStatus = it?.status) {
                    is Status.Success -> {
                        user = responseStatus.data
                        userInfoLiveData.postValue(user)
                        response.removeObserver(this)
                    }
                    is Status.Failure -> response.removeObserver(this)
                }
            }
        })
    }

    fun noDisplayNameSet() =
        user == null || user?.displayName == null || user?.displayName?.isEmptyTrimmed() == true

    fun changeUserDisplayName(newDisplayName: String) {
        if (newDisplayName != user?.displayName) {
            changeDisplayNameForAllMessagesLocally(newDisplayName)
            val response = Repository.updateDisplayName(UpdateDisplayNameRequest(newDisplayName))
            response.observeForever(object : Observer<Resource<SimpleResponse>> {
                override fun onChanged(it: Resource<SimpleResponse>?) {
                    when (it?.status) {
                        is Status.Loading -> loaderLiveData.postValue(true)
                        is Status.Success -> {
                            loaderLiveData.postValue(false)
                            user?.displayName = newDisplayName
                            userInfoLiveData.postValue(user)
                            response.removeObserver(this)
                        }
                        is Status.Failure -> {
                            loaderLiveData.postValue(false)
                            response.removeObserver(this)
                        }
                    }
                }
            })
        }
    }

    //TODO: develop normal solution for display name change and delete this function
    /**
     * Method used to change current user display name for all his messages in recycler view
     */
    private fun changeDisplayNameForAllMessagesLocally(newDisplayName: String) {
        getMessagesFromCurrentUser()?.forEach { it.nickname = newDisplayName }
        messagesLiveData.apply {
            postValue(this.value)
        }
    }

    //TODO: develop normal solution for avatar change and delete this function
    /**
     * Method used to change current user avatar for all his messages in recycler view
     */
    private fun changeAvatarForAllMessagesLocally(newAvatar: String) {
        getMessagesFromCurrentUser()?.forEach { it.avatarUrl = newAvatar }
        messagesLiveData.apply {
            postValue(this.value)
        }
    }

    private fun getMessagesFromCurrentUser(): List<Message>? = run {
        val currentUserId = user?.id
        if (currentUserId != null) {
            return messagesLiveData.value?.filter {
                it.userID != null && it.userID == currentUserId.toString()
            }
        }
        return null
    }

    fun changeUserAvatar() {
        newAvatar?.let { avatar ->
            val userImgUpdateResponse = Repository.uploadImage(avatar.toMultipart())
            userImgUpdateResponse.observeForever(object : Observer<Resource<UpdateImageResponse>> {
                override fun onChanged(t: Resource<UpdateImageResponse>?) {
                    t?.let {
                        when (it.status) {
                            is Status.Loading -> loaderLiveData.postValue(true)
                            is Status.Failure -> {
                                loaderLiveData.postValue(false)
                                userImgUpdateResponse.removeObserver(this)
                            }
                            is Status.Success -> {
                                loaderLiveData.postValue(false)
                                val newAvatarUrl = it.status.data?.imageUrl
                                user?.imageUrl = newAvatarUrl
                                newAvatarUrl?.let { newAvatarUrl ->
                                    changeAvatarForAllMessagesLocally(
                                        newAvatarUrl
                                    )
                                }
                                userImgUpdateResponse.removeObserver(this)
                            }
                        }
                    }
                }
            })
        }
    }
}