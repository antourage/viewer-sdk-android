package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.net.Uri
import android.os.Handler
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.antourage.weaverlib.screens.base.streaming.StreamingViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.DefaultLoadErrorHandlingPolicy.DEFAULT_MIN_LOADABLE_RETRY_COUNT
import com.google.android.exoplayer2.util.Util

class WeaverViewModel(application: Application) : ChatViewModel(application) {

    companion object{
        const val NEW_POLL_DELAY_MS = 15000L
    }

    sealed class PollStatus {
        class NO_POLL : PollStatus()
        class ACTIVE_POLL_DISMISABLE : PollStatus()
        class ACTIVE_POLL_DISMISSED(val pollStatus: Int) : PollStatus()
    }

    var wasStreamInitialized = false
    private var isNewPoll = true
    var streamId: Int = 0

    private val pollLiveData: MutableLiveData<Poll> =  MutableLiveData()
    private val isChatAllowed:MutableLiveData<Boolean> = MutableLiveData()
    private val pollStatusMessage:MutableLiveData<String> = MutableLiveData()

    fun getPollLiveData(): LiveData<Poll> = pollLiveData
    fun getChatAllowed():LiveData<Boolean> = isChatAllowed
    fun getPollStatusMessage():LiveData<String> = pollStatusMessage

    private  val messagesObserver:Observer<Resource<List<Message>>> = Observer {data->
        if(data?.data !=null )
            messagesLiveData.postValue(data.data)
    }
    private val activePollObserver: Observer<Resource<List<Poll>>> = Observer { data->
        if (data?.state == State.SUCCESS) {
            if (data.data != null && data.data.isNotEmpty()) {
                pollLiveData.postValue(data.data[0])
            } else
                pollLiveData.postValue(null)

        }else if (data?.state == State.FAILURE){
            error.value = data.message
        }
    }
    private val streamObserver:Observer<Resource<Stream>> = Observer { data->
            if (data?.data != null) {
                isChatAllowed.postValue(data.data.isChatActive)
            }
    }

    fun initUi(streamId: Int?){

        streamId?.let {
            this.streamId = it
            repository.getMessages(streamId).observeForever(messagesObserver)
            repository.getPollLiveData(streamId).observeForever(activePollObserver)
            repository.getStreamLiveData(streamId).observeForever(streamObserver)
        }
    }

    fun startNewPollCoundown(){
        pollStatusMessage.postValue(getApplication<Application>().getString(R.string.new_poll))
        Handler().postDelayed(
            {
                pollLiveData.value?.let {
                    isNewPoll = false
                    repository.getAnsweredUsers(streamId, it.id).observeForever {
                        if (it?.data != null) {
                            pollStatusMessage.postValue(getApplication<Application>().getString(R.string.number_answers,
                                it.data.size))
                        }
                    }
                }
            },
            NEW_POLL_DELAY_MS)
    }

    fun addMessage(message:Message,streamId:Int) {
        if (message.text != null && !message.text!!.isEmpty() && !message.text!!.isBlank()) {
            val temp: MutableList<Message> = (messagesLiveData.value)!!.toMutableList()
            temp.add(
                message
            )
            repository.addMessage(message,streamId)
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
        //TODO 10/5/2018 choose one
        //hls
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(streamUrl))
        //rtmp
//        return ExtractorMediaSource.Factory(RtmpDataSourceFactory())
//            .createMediaSource(Uri.parse(streamUrl))
    }
    override fun onVideoChanged() {

    }

    override fun onResume() {
        super.onResume()
        player.seekTo(player.duration)
    }

}