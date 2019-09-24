package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.net.Uri
import android.os.Handler
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.AnsweredUser
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.models.Stream
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class PlayerViewModel @Inject constructor(application: Application, val repository: Repository) :
    ChatViewModel(application) {

    companion object {
        const val NEW_POLL_DELAY_MS = 15000L
    }

    var wasStreamInitialized = false
    var streamId: Int = 0
    private var isChatTurnedOn = false
    private var postAnsweredUsers = false

    private val pollStatusLiveData: MutableLiveData<PollStatus> = MutableLiveData()
    private val chatStatusLiveData: MutableLiveData<ChatStatus> = MutableLiveData()
    var currentPoll: Poll? = null

    fun getPollStatusLiveData(): LiveData<PollStatus> = pollStatusLiveData
    fun getChatStatusLiveData(): LiveData<ChatStatus> = chatStatusLiveData

    init {
        pollStatusLiveData.value = PollStatus.NoPoll
        postAnsweredUsers = false
    }

    private val messagesObserver: Observer<Resource<List<Message>>> = Observer { resource ->
        resource?.status?.let {
            if (it is Status.Success && it.data != null && isChatTurnedOn) {
                if (chatContainsNonStatusMsg(it.data)) {
                    chatStatusLiveData.postValue(ChatStatus.ChatMessages)
                    messagesLiveData.postValue(it.data)
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
                        pollStatusLiveData.postValue(PollStatus.ActivePoll(it.data[0]))
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
                    //TODO 17/06/2019 wth does not actually remove observer
                    repository.getMessages(streamId).removeObserver(messagesObserver)
                    chatStatusLiveData.postValue(ChatStatus.ChatTurnedOff)
                } else {
                    repository.getMessages(streamId).observeForever(messagesObserver)
                }
            }
        }
    }

    fun initUi(streamId: Int?) {
        streamId?.let {
            this.streamId = it
            repository.getPoll(streamId).observeForever(activePollObserver)
            repository.getStream(streamId).observeForever(streamObserver)
        }
    }

    fun seePollDetails() {
        currentPoll?.let {
            pollStatusLiveData.value = (PollStatus.PollDetails(it.id))
        }
    }

    fun startNewPollCoundown() {
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
        for (j in 0 until answeredUsers.size) {
            if (answeredUsers[j].id == FirebaseAuth.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName)).uid) {
                return true
            }
        }
        return false
    }

    fun observeAnsweredUsers() {
        currentPoll?.let { poll ->
            repository.getAnsweredUsers(streamId, poll.id).observeForever { resource ->
                resource?.status?.let {
                    if (it is Status.Success && it.data != null) {
                        if (wasAnswered(it.data)) {
                            postAnsweredUsers = true
                        }
                        if (postAnsweredUsers && it.data.isNotEmpty())
                            pollStatusLiveData.postValue(
                                PollStatus.ActivePollDismissed(
                                    getApplication<Application>().resources.getQuantityString(
                                        R.plurals.number_answers,
                                        it.data.size,
                                        it.data.size
                                    )
                                )
                            )
                        else
                            pollStatusLiveData.postValue(
                                PollStatus.ActivePollDismissed(
                                    getApplication<Application>().getString(
                                        R.string.new_poll
                                    )
                                )
                            )
                    }
                }
            }
        }
    }

    fun addMessage(message: Message, streamId: Int) {
        if (message.text != null && !message.text!!.isEmpty() && !message.text!!.isBlank()) {
            val temp: MutableList<Message> = (messagesLiveData.value)!!.toMutableList()
            temp.add(
                message
            )
            repository.addMessage(message, streamId)
        }
    }

    override fun onStreamStateChanged(playbackState: Int) {
        if (playbackState == Player.STATE_READY) {
            wasStreamInitialized = true
        }
    }

    override fun getMediaSource(streamUrl: String?): MediaSource? {
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(
            getApplication(),
            Util.getUserAgent(getApplication(), "Exo2"), defaultBandwidthMeter
        )
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(streamUrl))
    }

    override fun onVideoChanged() {

    }

    override fun onResume() {
        super.onResume()
        player.seekTo(player.duration)

    }
}