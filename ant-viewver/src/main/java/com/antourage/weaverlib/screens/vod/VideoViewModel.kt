package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.convertUtcToLocal
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.observeOnce
import com.antourage.weaverlib.other.parseToDate
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import okhttp3.OkHttpClient
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

internal class VideoViewModel @Inject constructor(
    application: Application,
    private val roomRepository: RoomRepository
) : ChatViewModel(application) {

    companion object {
        private const val STOP_TIME_UPDATE_INTERVAL_MS = 5000L
        private const val FULLY_VIEWED_VIDEO_SEGMENT = 0.9
        private const val SKIP_VIDEO_TIME_MILLS = 10000
    }

    enum class AutoPlayState {
        START_AUTO_PLAY,     // auto play state should be started
        START_REPLAY,        // replay state should be started
        STOP_ALL_STATES      // leave all states as player's in default state
    }

    private var videoChanged: Boolean = false
    private var stopWatchingTime: Long = 0
    private var chatStateLiveData = MutableLiveData<Boolean>()
    private var startTime: Date? = null

    private var userProcessedMessages = mutableListOf<Message>()
    private var shownMessages = mutableListOf<Message>()
    private val messagesHandler = Handler()
    private var vodId: Int? = null
    private var user: User? = null

    private var isFetching: Boolean =
        false //in case user quickly tapping next fixes possible double fetch
    private val mediaSource = ConcatenatingMediaSource()
    private var timerTickHandler = Handler()
    private var timerTickRunnable = object : Runnable {
        override fun run() {
            setVodStopWatchingTime()
            timerTickHandler.postDelayed(this, STOP_TIME_UPDATE_INTERVAL_MS)
        }
    }

    val currentVideo: MutableLiveData<StreamResponse> = MutableLiveData()
    fun getCurrentVideo(): LiveData<StreamResponse> = currentVideo

    private val autoPlayStateLD: MutableLiveData<AutoPlayState> = MutableLiveData<AutoPlayState>()
    fun getAutoPlayStateLD(): LiveData<AutoPlayState> = autoPlayStateLD

    //method used to check if last added message added by User, but in VOD we don't use this check
    override fun checkIfMessageByUser(userID: String?): Boolean = false

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
                    val success = OnSuccessListener<QuerySnapshot> { snapshots ->
                        val data: MutableList<Message> = mutableListOf()
                        for (i in 0 until (snapshots?.documents?.size ?: 0)) {
                            val document = snapshots?.documents?.get(i)
                            document?.apply {
                                this.toObject(Message::class.java)?.let {
                                    data.add(it)
                                }
                                data[i].id = id
                            }
                        }
                        processVODsChat(data)
                    }

                    this@VideoViewModel.streamId?.let { Repository.getMessagesVOD(it, success) }
                }
            }
        }
    }

    fun initUi(id: Int?, startTime: String?) {
        this.startTime = startTime?.parseToDate()
        initUserAndFetchChat(id)
        id?.let {
            this.streamId = it
            this.stopWatchingTime = roomRepository.getStopTimeById(it) ?: 0
            fetchNextVODsIfTheLast(it)
        }
        this.vodId = id
        this.currentlyWatchedVideoId = id
        chatStateLiveData.postValue(true)
        markVODAsWatched()
    }

    private fun fetchChatList(id: Int?) {
        id?.let {
            Repository.getStream(it).observeOnce(streamObserver)
        }
    }

    private fun initUserAndFetchChat(id: Int?) {
        val response = Repository.getUser()
        response.observeForever(object : Observer<Resource<User>> {
            override fun onChanged(it: Resource<User>?) {
                when (val responseStatus = it?.status) {
                    is Status.Success -> {
                        user = responseStatus.data
                        fetchChatList(id)
                        response.removeObserver(this)
                    }
                    is Status.Failure -> {
                        fetchChatList(id)
                        response.removeObserver(this)
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startUpdatingStopWatchingTime()
        player?.seekTo(stopWatchingTime)
    }

    override fun onPause() {
        super.onPause()
        setReplayStateIfAutoPlayIsActive()
        timerTickHandler.removeCallbacksAndMessages(null)
        setVodStopWatchingTime(isUpdateLocally = true)
        stopMonitoringChatMessages()
    }

    private fun setVodStopWatchingTime(isUpdateLocally: Boolean = false) {
        stopWatchingTime = player?.currentPosition ?: 0
        val duration = player?.duration ?: 0L
        vodId?.let { vodId ->
            if (stopWatchingTime > 0) {
                if (duration != 0L && duration * FULLY_VIEWED_VIDEO_SEGMENT - stopWatchingTime < 0) {
                    roomRepository.addStopTime(VideoStopTime(vodId, 0, getExpirationDate(vodId)))
                } else {
                    roomRepository.addStopTime(
                        VideoStopTime(vodId, stopWatchingTime, getExpirationDate(vodId))
                    )
                }
            }
            if (isUpdateLocally) setVODStopWatchingTimeLocally()
        }
    }


    private fun startUpdatingStopWatchingTime() {
        if (timerTickHandler.hasMessages(0)) {
            timerTickHandler.removeCallbacksAndMessages(null)
        } else {
            timerTickHandler.post(timerTickRunnable)
        }
    }

    //todo: test whether correctly parsed
    private fun getExpirationDate(vodId: Int): Long {
        return Repository.vods?.find { video -> video.id?.equals(vodId) ?: false }
            ?.startTime?.let { convertUtcToLocal(it)?.time } ?: 0L
    }

    override fun onTrackEnd() {
        player?.playWhenReady = false
        autoPlayStateLD.postValue(AutoPlayState.START_AUTO_PLAY)
    }

    fun startReplayState() {
        autoPlayStateLD.postValue(AutoPlayState.START_REPLAY)
    }

    fun stopAutoPlayState(shouldCheckIfInReplayState: Boolean = false) {
        if (shouldCheckIfInReplayState) {
            if (autoPlayStateLD.value == AutoPlayState.START_REPLAY) return
        }
        autoPlayStateLD.postValue(AutoPlayState.STOP_ALL_STATES)
    }

    private fun setReplayStateIfAutoPlayIsActive() {
        if (autoPlayStateLD.value == AutoPlayState.START_AUTO_PLAY) {
            startReplayState()
        }
    }

    fun nextVideoPlay() {
        playNextTrack()
        stopAutoPlayState()
    }

    fun prevVideoPlay() {
        playPrevTrack()
        stopAutoPlayState()
    }

    fun rewindVideoPlay() {
        stopAutoPlayState()
        rewindAndPlayTrack()
    }

    fun getVideoDuration() = getCurrentDuration()
    fun getVideoPosition() = getCurrentPosition()

    override fun onVideoChanged() {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        val currentVod = list[currentWindow]
        currentVod.id?.apply {
            if (this != vodId) {
                videoChanged = true
                resetChronometer = true
                vodId?.let {
                    this@VideoViewModel.stopWatchingTime = roomRepository.getStopTimeById(it) ?: 0
                    postVideoIsClosed(it)
                }
                if (list.size <= currentWindow + 2) {
                    fetchNextVODs()
                }
            }
        }
        this.streamId = currentVod.id
        this.vodId = currentVod.id
        this.currentlyWatchedVideoId = currentVod.id
        this.startTime = currentVod.startTime?.parseToDate()
        currentVod.id?.let { Repository.getStream(it).observeOnce(streamObserver) }
        currentVideo.postValue(currentVod)
        stopAutoPlayState(shouldCheckIfInReplayState = true)
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

    fun onVideoStarted() {
        messagesHandler.post(messagesRunnable)
    }

    fun onVideoPausedOrStopped() {
        messagesHandler.post(messagesSingleEventRunnable)
    }

    fun setCurrentPlayerPosition(videoId: Int) {
        currentWindow = findVideoPositionById(videoId)
    }

    fun getChatStateLiveData() = chatStateLiveData

    private fun findVideoPositionById(videoId: Int): Int {
        val list: List<StreamResponse> = Repository.vods ?: arrayListOf()
        for (i in list.indices) {
            if (list[i].id == videoId) {
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
        val mediaSources = ArrayList<MediaSource?>()
        for (i in 0 until (list?.size ?: 0)) {
            mediaSources.add(list?.get(i)?.videoURL?.let { buildSimpleMediaSource(it) })
        }
        mediaSource.clear()
        mediaSource.addMediaSources(mediaSources)
        return mediaSource
    }

    private fun addToMediaSource(list: List<StreamResponse>) {
        val mediaSources = ArrayList<MediaSource?>()
        for (i in 0 until (list.size)) {
            mediaSources.add(list[i].videoURL?.let { buildSimpleMediaSource(it) })
        }
        mediaSource.addMediaSources(mediaSources)
    }

    private fun fetchNextVODs() {
        if (!isFetching) {
            isFetching = true
            val vodsCount = Repository.vods?.size ?: 0
            val response = Repository.getVODs(vodsCount)
            response.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                    if (resource != null) {
                        when (resource.status) {
                            is Status.Failure -> {
                                isFetching = false
                                Log.d(
                                    "PLAYER_FETCH",
                                    "Failed to load next 15 VODs: ${resource.status.errorMessage}"
                                )
                                response.removeObserver(this)
                            }
                            is Status.Success -> {
                                isFetching = false
                                Log.d(
                                    "PLAYER_FETCH",
                                    "Successfully loaded next 15 VODs"
                                )
                                resource.status.data?.let {
                                    Repository.vods?.addAll(it)
                                    addToMediaSource(it)
                                }
                                response.removeObserver(this)
                            }
                        }
                    }
                }
            })
        }
    }

    private fun fetchNextVODsIfTheLast(id: Int) {
        Repository.vods?.let {
            //todo: handle case for NEW VOD
            if (it.last().id == id && it.size != 1){
                fetchNextVODs()
            }
        }
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

    private fun processVODsChat(messages: List<Message>) {
        val currentUserId = user?.id
        val currentUserName = user?.displayName
        val userMessages = messages.filter { it.type == MessageType.USER }
        for (message in userMessages) {
            message.pushTimeMills =
                ((message.timestamp?.seconds ?: 0) * 1000) - (startTime?.time ?: 0)
            if (message.userID != null && currentUserName != null
                && message.userID == currentUserId.toString()
            ) {
                //changes displayName of current user's
                message.nickname = currentUserName
            }
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

    /**
     * set stopWatching time to avoid additional call to DB when
     * turning back from player to videos list screen
     */
    private fun setVODStopWatchingTimeLocally() {
        Repository.vods?.find { streamResponse -> streamResponse.id?.equals(vodId) ?: false }
            ?.stopTimeMillis = stopWatchingTime
    }

    fun seekToLastWatchingTime() {
        if (videoChanged) {
            player?.seekTo(stopWatchingTime)
            videoChanged = false
        }
    }
}