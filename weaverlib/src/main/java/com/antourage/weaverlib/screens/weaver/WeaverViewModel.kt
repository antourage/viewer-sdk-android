package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.net.Uri
import android.os.Handler
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class WeaverViewModel(application: Application) : ChatViewModel(application) {

    companion object {
        const val NEW_POLL_DELAY_MS = 15000L
    }

    sealed class PollStatus {
        class NO_POLL : PollStatus()
        class ACTIVE_POLL(val poll: Poll) : PollStatus()
        class ACTIVE_POLL_DISMISSED(val pollStatus: String?) : PollStatus()
        class POLL_DETAILS(val pollId: String) : PollStatus()
    }

    sealed class ChatStatus {
        class CHAT_TURNED_OFF : ChatStatus()
        class CHAT_NO_MESSAGES : ChatStatus()
        class CHAT_MESSAGES(val messages: List<Message>) : ChatStatus()
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
        pollStatusLiveData.value = PollStatus.NO_POLL()
        postAnsweredUsers = false
    }

    private val messagesObserver: Observer<Resource<List<Message>>> =
        object : Observer<Resource<List<Message>>> {
            override fun onChanged(data: Resource<List<Message>>?) {
                if (data?.data != null && isChatTurnedOn)
                    if (isChatContainsNonstatusMsg(data.data)) {
                        chatStatusLiveData.postValue(ChatStatus.CHAT_MESSAGES(data.data))
                        messagesLiveData.postValue(data.data)
                    } else {
                        chatStatusLiveData.postValue(ChatStatus.CHAT_NO_MESSAGES())
                    }
            }
        }
    private val activePollObserver: Observer<Resource<List<Poll>>> = Observer { data ->
        if (data?.state == State.SUCCESS) {
            if (data.data != null && data.data.isNotEmpty()) {
                postAnsweredUsers = false
                pollStatusLiveData.postValue(PollStatus.ACTIVE_POLL(data.data[0]))
                currentPoll = data.data[0]
            } else {
                postAnsweredUsers = false
                pollStatusLiveData.postValue(PollStatus.NO_POLL())
                currentPoll = null
            }
        } else if (data?.state == State.FAILURE) {
            error.value = data.message
        }
    }
    private val streamObserver: Observer<Resource<Stream>> = Observer { data ->
        if (data?.data != null) {
            isChatTurnedOn = data.data.isChatActive
            if (!data.data.isChatActive) {
                //TODO 17/06/2019 wth does not actually remove observer
                repository.getMessages(streamId).removeObserver(messagesObserver)
                chatStatusLiveData.postValue(ChatStatus.CHAT_TURNED_OFF())
            } else {
                repository.getMessages(streamId).observeForever(messagesObserver)
            }
        }
    }

    fun initUi(streamId: Int?) {

        streamId?.let {
            this.streamId = it
            repository.getPollLiveData(streamId).observeForever(activePollObserver)
            repository.getStreamLiveData(streamId).observeForever(streamObserver)
        }
    }

    fun seePollDetails() {
        currentPoll?.let {
            pollStatusLiveData.value = (PollStatus.POLL_DETAILS(it.id))
        }
    }

    private fun isChatContainsNonstatusMsg(list: List<Message>): Boolean {
        for (message in list) {
            if (message.type == MessageType.USER) {
                return true
            }
        }
        return false
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
        currentPoll?.let {
            repository.getAnsweredUsers(streamId, it.id).observeForever {
                if (it?.data != null) {
                    if (wasAnswered(it.data)) {
                        postAnsweredUsers = true
                    }
                    if (postAnsweredUsers)
                        pollStatusLiveData.postValue(
                            PollStatus.ACTIVE_POLL_DISMISSED(
                                getApplication<Application>().getString(
                                    R.string.number_answers,
                                    it.data.size
                                )
                            )
                        )
                    else
                        pollStatusLiveData.postValue(PollStatus.ACTIVE_POLL_DISMISSED(getApplication<Application>().getString(R.string.new_poll)))
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