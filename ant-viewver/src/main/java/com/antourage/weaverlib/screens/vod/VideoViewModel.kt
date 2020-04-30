package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.net.Uri
import android.os.Handler
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.formatDuration
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.observeOnce
import com.antourage.weaverlib.other.parseToDate
import com.antourage.weaverlib.other.parseToMills
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

internal class VideoViewModel @Inject constructor(application: Application) :
    ChatViewModel(application) {

    companion object {
        private const val SKIP_VIDEO_TIME_MILLS = 10000
    }

    private var videoChanged: Boolean = false
    private var predefinedStopWatchingTime: Long? = null
    private var chatDataLiveData: QuerySnapshotLiveData<Message>? = null
    private var chatStateLiveData = MutableLiveData<Boolean>()
    private var startTime: Date? = null
    private var userProcessedMessages = mutableListOf<Message>()
    private var shownMessages = mutableListOf<Message>()
    private val messagesHandler = Handler()
    private var repository = Repository()
    private var vodId: Int? = null

    private val messagesRunnable = object : Runnable {
        override fun run() {
            manageShownMessages()
            messagesHandler.postDelayed(this, 100)
        }
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
                    if (player?.currentPosition ?: 0 >= pushedTimeMills) {
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
                        this@VideoViewModel.streamId?.let { Repository.getMessages(it) }
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

    val currentVideo: MutableLiveData<StreamResponse> = MutableLiveData()
    private val videoEndedLD: MutableLiveData<Boolean> = MutableLiveData()

    fun initUi(streamId: Int?, startTime: String?, vodId: Int?, stopTime: String?) {
        this.startTime = startTime?.parseToDate()
        streamId?.let {
            this.streamId = it
            Repository.getStream(streamId).observeOnce(streamObserver)
        }
        this.vodId = vodId
        this.currentlyWatchedVideoId = vodId
        chatStateLiveData.postValue(true)
        markVODAsWatched()
        this.predefinedStopWatchingTime = stopTime?.parseToMills()
    }

    override fun onResume() {
        super.onResume()
        predefinedStopWatchingTime?.let { player?.seekTo(it) }
    }

    override fun onPause() {
        super.onPause()
        predefinedStopWatchingTime = player?.currentPosition
        setVodStopWatchingTime()
        stopMonitoringChatMessages()
    }

    private fun setVodStopWatchingTime() {
        vodId?.let { vodId ->
            /**
             * set stopWatching time to avoid additional server call when
             * turning back from player to videos list screen
             */
            setVODStopWatchingTimeLocally()
            stopWatchingTime?.let { stopWatchingTime ->
                if (stopWatchingTime > 0) {
                    Repository.stopWatchingVOD(
                        StopWatchVodRequest(
                            vodId, stopWatchingTime.formatDuration()
                        )
                    )
                }
            }
        }
    }

    override fun onTrackEnd() {
        player?.playWhenReady = false
        videoEndedLD.postValue(true)
    }

    fun nextVideoPlay() {
        playNextTrack()
        videoEndedLD.postValue(false)
    }

    fun prevVideoPlay() {
        playPrevTrack()
        videoEndedLD.postValue(false)
    }

    fun rewindVideoPlay() {
        videoEndedLD.postValue(false)
        rewindAndPlayTrack()
    }

    fun getVideoDuration() = getCurrentDuration()
    fun getVideoPosition() = getCurrentPosition()

    override fun onVideoChanged() {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        val currentVod = list[currentWindow]
        list[currentWindow].id?.apply {
            if (this != vodId) {
                videoChanged = true
                this@VideoViewModel.predefinedStopWatchingTime = currentVod.stopTime?.parseToMills()
                setVodStopWatchingTime()
            }
        }
        this.streamId = currentVod.streamId
        this.vodId = currentVod.id
        this.currentlyWatchedVideoId = currentVod.id
        this.startTime = currentVod.startTime?.parseToDate()
        currentVod.streamId?.let { Repository.getStream(it).observeOnce(streamObserver) }
        currentVideo.postValue(currentVod)
        videoEndedLD.postValue(false)
        if (player?.playWhenReady == true && player?.playbackState == Player.STATE_READY) {
            player?.playWhenReady = true
        }
        markVODAsWatched()
    }

    fun skipForward() {
        player?.seekTo((player?.currentPosition ?: 0) + SKIP_VIDEO_TIME_MILLS)
    }

    fun skipBackward() {
        player?.seekTo((player?.currentPosition ?: 0) - SKIP_VIDEO_TIME_MILLS)
    }

    fun onVideoStarted(streamId: Int) {
        messagesHandler.post(messagesRunnable)
    }

    fun onVideoPausedOrStopped() {
        messagesHandler.post(messagesSingleEventRunnable)
    }

    fun getCurrentVideo(): LiveData<StreamResponse> = currentVideo

    fun getVideoEndedLD(): LiveData<Boolean> = videoEndedLD

    fun setCurrentPlayerPosition(videoId: Int) {
        currentWindow = findVideoPositionById(videoId)
    }

    fun getChatStateLiveData() = chatStateLiveData

    private fun findVideoPositionById(videoId: Int): Int {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        for (i in list.indices) {
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
//            mediaSources[i] = list?.get(i)?.videoURL?.let { buildSimpleMediaSource("") }
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

    private fun markVODAsWatched() {
        Repository.vods?.find { it.id?.equals(vodId) ?: false }?.isNew = false
    }

    private fun setVODStopWatchingTimeLocally() {
        Repository.vods?.find { streamResponse -> streamResponse.id?.equals(vodId) ?: false }
            ?.stopTime = stopWatchingTime?.formatDuration()
    }

    fun seekToLastWatchingTime() {
        if (videoChanged) {
            predefinedStopWatchingTime?.let { player?.seekTo(it) }
            videoChanged = false
        }
    }
}