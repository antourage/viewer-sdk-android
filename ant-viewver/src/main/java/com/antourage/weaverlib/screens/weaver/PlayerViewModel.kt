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
import com.antourage.weaverlib.other.networking.profile.ProfileRepository
import com.antourage.weaverlib.other.reObserveForever
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

internal class PlayerViewModel constructor(application: Application) : ChatViewModel(application) {

    companion object {
        const val NEW_POLL_DELAY_MS = 15000L
        const val CLOSE_EXPANDED_POLL_DELAY_MS = 6000L
    }

    var wasStreamInitialized = false
    private var isChatTurnedOn = false
    private var postAnsweredUsers = false
    private var user: ProfileResponse? = null
    private var banner: AdBanner? = null

    private var messagesResponse: QuerySnapshotLiveData<Message>? = null

    private val pollStatusLiveData: MutableLiveData<PollStatus> = MutableLiveData()
    private val chatStatusLiveData: MutableLiveData<ChatStatus> = MutableLiveData()
    private val userInfoLiveData: MutableLiveData<ProfileResponse> = MutableLiveData()
    private val isCurrentStreamStillLiveLiveData: MutableLiveData<Boolean> = MutableLiveData()

    internal var currentPoll: Poll? = null

    var newAvatar: Bitmap? = null
    var oldAvatar: Bitmap? = null
    var avatarDeleted = false
    var profileInfo: UserInfo? = null

    internal fun getPollStatusLiveData(): LiveData<PollStatus> = pollStatusLiveData
    fun getChatStatusLiveData(): LiveData<ChatStatus> = chatStatusLiveData
    fun getUserInfoLiveData(): LiveData<ProfileResponse> = userInfoLiveData
    fun getCurrentLiveStreamInfo() = isCurrentStreamStillLiveLiveData
    fun getUser() = user

    init {
        pollStatusLiveData.value = PollStatus.NoPoll
        postAnsweredUsers = false
        banner = UserCache.getInstance(getApplication())?.getBanner()
    }

    private val messagesObserver: Observer<Resource<List<Message>>> = Observer { resource ->
        resource?.status?.let {
            if (it is Status.Success && it.data != null) {
                if (chatContainsNonStatusMsg(it.data)) {
                    if (isChatTurnedOn) chatStatusLiveData.postValue(ChatStatus.ChatMessages)
                    val name = user?.nickname
                    if (name != null) {
                        changeAndPostDisplayNameForAllMessages(name, it.data)
                    } else {
                        messagesLiveData.value = it.data
                    }
                } else {
                    if (isChatTurnedOn) chatStatusLiveData.postValue(ChatStatus.ChatNoMessages)
                    messagesLiveData.value = listOf()
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
                        if (UserCache.getInstance(getApplication())?.getCollapsedPollId()
                                .equals(it.data[0].id)
                        ) {
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
                else -> {}
            }
        }
    }

    private val streamObserver: Observer<Resource<Stream>> = Observer { resource ->
        resource?.status?.let {
            if (it is Status.Success && it.data != null) {
                isChatTurnedOn = it.data.isChatActive
                if (!isChatTurnedOn) {
                    chatStatusLiveData.postValue(ChatStatus.ChatTurnedOff)
                }
                streamId?.let { it1 ->
                    messagesResponse = Repository.getMessages(it1)
                    messagesResponse?.observeForever(messagesObserver)
                }
            } else if (it is Status.Success && it.data == null) {
                isChatTurnedOn = false
                chatStatusLiveData.postValue(ChatStatus.ChatTurnedOff)
            }
        }
    }

    fun initUi(id: Int?) {
        id?.let {
            this.streamId = it
            Repository.getPoll(it).observeForever(activePollObserver)
            Repository.getStream(it).observeForever(streamObserver)
            this.currentlyWatchedVideoId = it
        }
    }

    //Wasn't deleted, as it can be useful in some next design refinement
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
        currentPoll?.id?.let {
            pollStatusLiveData.value = (PollStatus.PollDetails(it))
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
            if (answeredUser.id == user?.id) {
                return true
            }
        }
        return false
    }

    private fun observeAnsweredUsers() {
        currentPoll?.id?.let { id ->
            streamId?.let { streamId ->
                Repository.getAnsweredUsers(streamId, id).observeForever { resource ->
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

    fun markActivePollDismissed() {
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
        return if (userID == null) {
            false
        } else {
            getUser()?.id == userID
        }
    }

    override fun onStreamStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            wasStreamInitialized = true
        }
    }

    override fun getMediaSource(streamUrl: String?): MediaSource {
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
        if(!UserCache.getInstance()?.getIdToken().isNullOrEmpty()){
            val response = ProfileRepository.getProfile()
            response.observeForever(object : Observer<Resource<ProfileResponse>> {
                override fun onChanged(it: Resource<ProfileResponse>?) {
                    when (val responseStatus = it?.status) {
                        is Status.Success -> {
                            user = responseStatus.data
                            val profile = responseStatus.data
                            UserCache.getInstance()?.saveUserNickName(profile?.nickname ?: "")
                            UserCache.getInstance()?.saveUserImage(profile?.imageUrl ?: "")
                            UserCache.getInstance()?.saveUserId(profile?.id ?: "")
                            userInfoLiveData.postValue(user)
                            response.removeObserver(this)
                        }
                        is Status.Failure -> response.removeObserver(this)
                    }
                }
            })
        }
    }

    fun noDisplayNameSet() =
        user == null || user?.nickname == null || user?.nickname?.isEmptyTrimmed() == true


    private fun changeAndPostDisplayNameForAllMessages(displayName: String, list: List<Message>) {
        val currentUserId = user?.id
        if (currentUserId != null) {
            list.forEach {
                if (it.userID != null && it.userID == currentUserId.toString()) {
                    it.nickname = displayName
                }
            }
        }
        messagesLiveData.postValue(list)
    }

    fun getDuration() = getCurrentDuration()

    override fun onUpdateBannerInfo(banner: AdBanner?) {
        this.banner = banner
        UserCache.getInstance(getApplication())?.saveBanner(banner)
    }

    fun getBanner() = banner
}