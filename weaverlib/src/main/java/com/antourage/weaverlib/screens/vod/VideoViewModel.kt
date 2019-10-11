package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.net.Uri
import android.os.Handler
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.formatDuration
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.observeOnce
import com.antourage.weaverlib.other.parseToDate
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.util.Util
import okhttp3.OkHttpClient
import java.util.*
import javax.inject.Inject


class VideoViewModel @Inject constructor(application: Application) :
    ChatViewModel(application) {

    private var chatDataLiveData: QuerySnapshotLiveData<Message>? = null
    private var chatStateLiveData = MutableLiveData<Boolean>()
    private var startTime: Date? = null
    private var userProcessedMessages = mutableListOf<Message>()
    private var shownMessages = mutableListOf<Message>()
    private val messagesHandler = Handler()
    private var repository = Repository()

    private val messagesRunnable = object : Runnable {
        override fun run() {
            manageShownMessages()
            messagesHandler.postDelayed(this, 100)
        }
    }

    private fun updateWatchingTimeSpan(watchingTime: StatisticWatchVideoRequest) {
        UserCache.getInstance(getApplication())?.updateVODWatchingTime(watchingTime)
    }

    private val messagesSingleEventRunnable = Runnable {
        manageShownMessages()
        messagesHandler.removeCallbacksAndMessages(null)
    }

    private fun manageShownMessages() {
        val shownMessagesOldSize = shownMessages.size
        shownMessages.clear()
        if (userProcessedMessages.isNotEmpty()) {
            for (message in userProcessedMessages) {
                message.pushTimeMills?.let { pushedTimeMills ->
                    if (player.currentPosition >= pushedTimeMills) {
                        shownMessages.add(message)
                    }
                }
            }
        }
        val shownMessagesNewSize = shownMessages.size
        if (shownMessagesOldSize == 0 && shownMessagesNewSize > 0) {
            chatStateLiveData.postValue(false)
        } else if (shownMessagesOldSize > 0 && shownMessagesNewSize == 0) {
            chatStateLiveData.postValue(true)
        }
        shownMessages.sortBy { it.pushTimeMills }
        messagesLiveData.postValue(shownMessages)
    }

    private val streamObserver: Observer<Resource<Stream>> = Observer { resource ->
        when (val status = resource?.status) {
            is Status.Success -> {
                status.data?.apply {
                    chatDataLiveData =
                        this@VideoViewModel.streamId?.let { repository.getMessages(it) }
                    chatDataLiveData?.observeOnce(chatDataObserver)
                }
            }
        }
    }

    private val chatDataObserver: Observer<Resource<List<Message>>> = Observer { resource ->
        when (val status = resource?.status) {
            is Status.Success -> {
                status.data?.let { messages ->
                    processVODsChat(messages)
                }
            }
        }
    }

    private val currentVideo: MutableLiveData<StreamResponse> = MutableLiveData()

    fun initUi(streamId: Int?, startTime: String?) {
        this.startTime = startTime?.parseToDate()
        streamId?.let {
            this.streamId = it
            repository.getStream(streamId).observeOnce(streamObserver)
        }
        chatStateLiveData.postValue(true)
    }

    override fun onPause() {
        super.onPause()
        Repository().stopWatchingVOD(
            StopWatchVodRequest(
                streamId, player.currentPosition.formatDuration()
            )
        )
        stopMonitoringChatMessages()
    }

    override fun onVideoChanged() {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        val currentVod = list[currentWindow]
        this.streamId = currentVod.streamId
        this.startTime = currentVod.startTime?.parseToDate()
        currentVod.streamId?.let { repository.getStream(it).observeOnce(streamObserver) }
        currentVideo.postValue(currentVod)
        if (player.playWhenReady && player.playbackState == Player.STATE_READY)
            player.playWhenReady = true
    }

    fun onVideoStarted(streamId: Int) {
        messagesHandler.post(messagesRunnable)
    }

    fun onVideoPausedOrStopped() {
        messagesHandler.post(messagesSingleEventRunnable)
    }

    fun getCurrentVideo(): LiveData<StreamResponse> = currentVideo

    fun setCurrentPlayerPosition(videoId: Int) {
        currentWindow = findVideoPositionById(videoId)
    }

    fun getChatStateLiveData() = chatStateLiveData

    private fun findVideoPositionById(videoId: Int): Int {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        for (i in 0 until list.size) {
            if (list[i].streamId == videoId) {
                currentVideo.postValue(list[i])
                return i
            }
        }
        return -1
    }

    /**
     * using this to create playlist. For now, was approved
     */
    override fun getMediaSource(streamUrl: String?): MediaSource? {
        val list: List<StreamResponse>? = Repository.vods
        val mediaSources = arrayOfNulls<MediaSource>(list?.size ?: 0)
        for (i in 0 until (list?.size ?: 0)) {
            mediaSources[i] = list?.get(i)?.videoURL?.let { buildSimpleMediaSource(it) }
        }
        return ConcatenatingMediaSource(*mediaSources)
    }

    override fun onStreamStateChanged(playbackState: Int) {}

    /**
     * videos do not play on Android 5 without this additional header. IDK why
     */
    private fun buildSimpleMediaSource(uri: String): MediaSource {
        val okHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("Connection", "close").build()
                chain.proceed(request)
            }
            .build()

        val dataSourceFactory =
            OkHttpDataSourceFactory(okHttpClient, Util.getUserAgent(getApplication(), "Exo2"))
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(uri))
    }

    private fun processVODsChat(
        messages: List<Message>
    ) {
        val pushedMessages = mutableListOf<Message>()
        pushedMessages.addAll(messagesLiveData.value ?: mutableListOf())
        val userMessages = messages.filter { it.type == MessageType.USER }
        for (message in userMessages) {
            message.pushTimeMills =
                ((message.timestamp?.seconds ?: 0) * 1000) - (startTime?.time ?: 0)
        }
        userProcessedMessages.clear()
        userProcessedMessages.addAll(userMessages)
    }

    private fun stopMonitoringChatMessages() {
        messagesHandler.removeCallbacksAndMessages(null)
    }
}


