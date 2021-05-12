package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.SingleLiveEvent
import com.antourage.weaverlib.other.convertUtcToLocal
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.networking.feed.FeedRepository
import com.antourage.weaverlib.other.networking.profile.ProfileRepository
import com.antourage.weaverlib.other.parseTimerToMills
import com.antourage.weaverlib.other.parseToDate
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.*
import kotlin.collections.ArrayList


internal class VideoViewModel constructor(application: Application) : ChatViewModel(application) {

    companion object {
        private const val STOP_TIME_UPDATE_INTERVAL_MS = 2000L
        private const val FULLY_VIEWED_VIDEO_SEGMENT = 0.9
        private const val SKIP_VIDEO_TIME_MILLS = 10000
        internal const val CURTAIN_MARGIN_MILLS = 500
    }

    enum class AutoPlayState {
        START_AUTO_PLAY,     // auto play state should be started
        START_REPLAY,        // replay state should be started
        STOP_ALL_STATES      // leave all states as player's in default state
    }

    private val roomRepository: RoomRepository = RoomRepository.getInstance(application)

    private var videoChanged: Boolean = true
    private var endOfCurtain = 0L //if not 0L - show skip Button
    private var stopWatchingTime: Long = 0
    private var chatStateLiveData = MutableLiveData<Boolean>()
    private var startTime: Date? = null

    private var userProcessedMessages = mutableListOf<Message>()
    private var shownMessages = mutableListOf<Message>()
    private val messagesHandler = Handler()
    private var vodId: Int? = null
    private var curtains: ArrayList<CurtainRangeMillis> = ArrayList()
    private var vodResponseDuration: Long = 0L
    private var user: ProfileResponse? = null

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

    //firstValue - Id, second - Viewers
    private val currentViewers: MutableLiveData<Pair<Int, Long>> = MutableLiveData()
    fun getCurrentViewersLD(): LiveData<Pair<Int, Long>> = currentViewers

    //long - curtain end time in millis
    private val curtainShown: SingleLiveEvent<Long> = SingleLiveEvent()
    fun getCurtainShownLD(): LiveData<Long> = curtainShown

    private val autoPlayStateLD: MutableLiveData<AutoPlayState> = MutableLiveData()
    fun getAutoPlayStateLD(): LiveData<AutoPlayState> = autoPlayStateLD

    private val nextVideosFetchedLD: MutableLiveData<Boolean> = MutableLiveData()
    fun getNextVideosFetchedLD(): LiveData<Boolean> = nextVideosFetchedLD

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
                    } else if (getVideoDuration() != null && getVideoDuration()!! > 0) {
                        if ((getVideoDuration() ?: 0) - (player?.currentPosition ?: 0) <= 1000) {
                            shownMessages.add(message)
                        } else {
                        }
                    } else {
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

    fun initUi(
        id: Int?, startTime: String?, curtains: List<CurtainRange>?, duration: String?,
        isNewVod: Boolean = false
    ) {
        this.startTime = startTime?.parseToDate()
        initUserAndFetchChat(id)
        id?.let {
            this.streamId = it
            this.stopWatchingTime = roomRepository.getStopTimeById(it) ?: 0
            fetchNextVODsIfTheLast(it, isNewVod)
        }
        this.vodId = id
        this.currentlyWatchedVideoId = id
        vodResponseDuration = duration?.parseTimerToMills() ?: 0L
        updateCurtains(curtains)
        chatStateLiveData.postValue(true)
        markVODAsWatched()
    }

    private fun getChatList(id: Int?) {
        id?.let { vodId ->
            viewModelScope.launch {
                val messages = roomRepository.getFirebaseMessagesById(vodId)
                if (messages.isEmpty()) {
                    fetchChat(vodId)
                } else {
                    processVODsChat(messages, isFetched = false)
                }
            }
        }
    }

    private fun fetchChat(vodId: Int) {
        val onSuccess = OnSuccessListener<QuerySnapshot> { snapshots ->
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
            val messages = processVODsChat(data, true)
            roomRepository.insertAllComments(
                Video(vodId, 0, getExpirationDate(vodId)),
                messages
            )
        }
        Repository.getMessagesVOD(vodId, onSuccess)
    }

    private fun initUserAndFetchChat(id: Int?) {
        if(!UserCache.getInstance()?.getIdToken().isNullOrEmpty()) {
            val response = ProfileRepository.getProfile()
            response.observeForever(object : Observer<Resource<ProfileResponse>> {
                override fun onChanged(it: Resource<ProfileResponse>?) {
                    when (val responseStatus = it?.status) {
                        is Status.Success -> {
                            user = responseStatus.data
                            getChatList(id)
                            response.removeObserver(this)
                        }
                        is Status.Failure -> {
                            getChatList(id)
                            response.removeObserver(this)
                        }
                    }
                }
            })
        }else{
            getChatList(id)
        }
    }

    override fun onResume() {
        super.onResume()
        player?.seekTo(stopWatchingTime)
        startUpdatingStopWatchingTime()
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
        var duration = 0L
        player?.duration.let {
            if (it != null)
                if (it >= 0) {
                    duration = it
                } else {
                    return
                }
        }
        vodId?.let { vodId ->
            if (stopWatchingTime > 0) {
                if (duration != 0L && duration * FULLY_VIEWED_VIDEO_SEGMENT - stopWatchingTime < 0) {
                    roomRepository.addStopTime(Video(vodId, 0, getExpirationDate(vodId)))
                } else {
                    roomRepository.addStopTime(
                        Video(vodId, stopWatchingTime, getExpirationDate(vodId))
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

    private fun getExpirationDate(vodId: Int): Long {
        return FeedRepository.vods?.find { video -> video.id?.equals(vodId) ?: false }
            ?.startTime?.let { convertUtcToLocal(it)?.time } ?: 0L
    }

    override fun onTrackEnd() {
        player?.playWhenReady = false
        autoPlayStateLD.postValue(AutoPlayState.START_AUTO_PLAY)
    }

    override fun registerCallbacks(windowIndex: Int) {
        curtains.forEach {
            if ((getVideoDuration() ?: 0) - it.end > 1000) {
                //we shouldn't show skip button if the curtain in the end of video
                createAdCallback(windowIndex, it) { curtainEndTime, windowIndex ->
                    if (windowIndex == currentWindow) curtainShown.postValue(curtainEndTime)
                }
            }
        }
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
        videoChanged = true
        stopWatchingTime = 0
    }

    fun getCurrentCurtains(): ArrayList<CurtainRangeMillis> = curtains

    private fun updateCurtains(newCurtains: List<CurtainRange>?) {
        curtains.clear()
        if (!newCurtains.isNullOrEmpty()) {
            curtains = ArrayList(newCurtains.map {
                CurtainRangeMillis(
                    it.start?.parseTimerToMills() ?: 0L,
                    it.end?.parseTimerToMills() ?: 0L
                )
            })
        }
    }

    fun getVideoDuration() = getCurrentDuration()
    fun getVideoPosition() = getCurrentPosition()

    override fun onVideoChanged() {
        val list: List<StreamResponse> = FeedRepository.vods?.filter { it.type != StreamResponseType.POST } ?: arrayListOf()
        val currentVod = list[currentWindow]
        currentVod.id?.apply {
            if (this != vodId) {
                videoChanged = true
                resetChronometer = true
                stopwatch.stopIfRunning()
                vodId?.let {
                    postVideoIsClosed(it)
                }
                if (list.size <= currentWindow + 2) {
                    fetchNextVODsIfRequired()
                }
                this@VideoViewModel.stopWatchingTime = roomRepository.getStopTimeById(this) ?: 0
                getChatList(this)
                vodResponseDuration = currentVod.duration?.parseTimerToMills() ?: 0L
                updateCurtains(currentVod.curtainRangeModels)
            }
        }
        this.streamId = currentVod.id
        this.vodId = currentVod.id
        this.currentlyWatchedVideoId = currentVod.id
        this.startTime = currentVod.startTime?.parseToDate()
        currentVideo.postValue(currentVod)
        stopAutoPlayState(shouldCheckIfInReplayState = true)
        if (player?.playWhenReady == true && player?.playbackState == Player.STATE_READY) {
            player?.playWhenReady = true
        }
        markVODAsWatched()
    }

    override fun onOpenStatisticUpdate(vodId: Int) {
        val currCount = FeedRepository.vods?.find { it.id?.equals(vodId) ?: false }?.viewsCount
        currCount?.let { count ->
            FeedRepository.vods?.find { it.id?.equals(vodId) ?: false }?.viewsCount = count + 1

            currentViewers.postValue(Pair(vodId, count + 1))
        }
    }

    fun skipForward() {
        player?.seekTo((player?.currentPosition ?: 0) + SKIP_VIDEO_TIME_MILLS)
    }

    fun skipBackward() {
        player?.seekTo((player?.currentPosition ?: 0) - SKIP_VIDEO_TIME_MILLS)
    }

    fun seekPlayerTo(position: Long) {
        player?.seekTo(position)
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
        val list: List<StreamResponse> = FeedRepository.vods?.filter { it.type != StreamResponseType.POST } ?: arrayListOf()
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
    override fun getMediaSource(streamUrl: String?): MediaSource {
        val list: List<StreamResponse>? = FeedRepository.vods
        val mediaSources = ArrayList<MediaSource>()
        for (i in 0 until (list?.size ?: 0)) {
            // url can be even null, as far as we handle errors when user start video playback
            // can't exclude video with url null from playlist, as it breaks video changing logic on UI
            if (list?.get(i)?.type != StreamResponseType.POST) {
                mediaSources.add(buildSimpleMediaSource(list!![i].videoURL.toString()))
            }
        }
        mediaSource.clear()
        mediaSource.addMediaSources(mediaSources)
        return mediaSource
    }

    private fun addToMediaSource(list: List<StreamResponse>) {
        val mediaSources = ArrayList<MediaSource>()
        for (i in 0 until (list.size)) {
            // url can be even null, as far as we handle errors when user start video playback
            // can't exclude video with url null from playlist, as it breaks video changing logic on UI
            if (list[i].type != StreamResponseType.POST) {
                mediaSources.add(buildSimpleMediaSource(list[i].videoURL.toString()))
            }
        }
        if (mediaSources.isNotEmpty()) mediaSource.addMediaSources(mediaSources)
    }

    /**
     * Changes video to next or playbacks previous one, if it the last one.
     * Does nothing, when there is only 1 video in playlist.
     * Used when impossible to playback video and the exoplayer stacked.
     */
    override fun changeVideoAfterPlayerRestart() {
        if (!Global.networkAvailable) return
        val currentIndex = player?.currentWindowIndex
        currentIndex?.let { index ->
            if (index in 0 until mediaSource.size && mediaSource.size != 1) {
                val seekToIndex = if (index + 1 == mediaSource.size && index - 1 >= 0) {
                    index - 1
                } else {
                    index + 1
                }
                player?.setMediaSource(mediaSource)
                player?.prepare()
                player?.seekTo(seekToIndex, C.TIME_UNSET)
                player?.playWhenReady = true
            }
        }
    }

    /**
     * Checks whether current vods count divides on 15, as 15 is the batch of vods from back-end.
     * @isNewVod - marks that we have 1 video in playlist, so will fetch next videos.
     * Will not proceed if next batch is already fetching.
     * If all checks successful - will also fetch next VODs.
     */
    private fun fetchNextVODsIfRequired(isNewVod: Boolean = false) {
        if (!isFetching) {
            val itemsCount = if (isNewVod) 0 else FeedRepository.vods?.size ?: 0
            if (itemsCount % 15 == 0 || isNewVod) {
                fetchNextVODs(itemsCount, isNewVod)
            }
        }
    }



    /**
     * Called after fetching next 15 items in case total number of fetched vods is not enough (more posts than vods)
     */
    fun fetchMoreVodsIfRequired(){
        val list: List<StreamResponse> = FeedRepository.vods?.filter { it.type != StreamResponseType.POST } ?: arrayListOf()
        if (list.size <= currentWindow + 2) {
            fetchNextVODsIfRequired()
        }
    }

    private fun fetchNextVODs(vodsCount: Int, isNewVod: Boolean = false) {
        isFetching = true
        val response = FeedRepository.getVODsWithLastCommentAndStopTime(vodsCount, roomRepository)
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
                            Log.d("PLAYER_FETCH", "Successfully loaded next 15 VODs")
                            resource.status.data?.let {
                                var list = listOf<StreamResponse>()
                                if (isNewVod) {
                                    list = it.drop(1)
                                } else {
                                    val lastVideoId = FeedRepository.vods?.last()?.id ?: 0
                                    if (!it.any { video -> video.id == lastVideoId }) {
                                        list = it
                                    }
                                }

                                FeedRepository.vods?.addAll(list)
                                addToMediaSource(list)
                                nextVideosFetchedLD.value = true
                                fetchMoreVodsIfRequired()
                                Log.d("PLAYER_FETCH", "was new vod $isNewVod")
                            }
                            response.removeObserver(this)
                        }
                        else -> {
                        }
                    }
                }
            }
        })
    }

    private fun fetchNextVODsIfTheLast(id: Int, isNewVod: Boolean = false) {
        FeedRepository.vods?.filter { it.type != StreamResponseType.POST }?.let {
            if (it.last().id == id) {
                fetchNextVODsIfRequired(isNewVod && it.size == 1)
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

        return if (uri.endsWith("mp4", true) || uri.endsWith("flv", true) || uri.endsWith(
                "mov",
                true
            )
        ) {
            val okHttpDataSourceFactory =
                OkHttpDataSourceFactory(okHttpClient, Util.getUserAgent(getApplication(), "Exo2"))
            ProgressiveMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        } else {
            val okHttpDataSourceFactory =
                OkHttpDataSourceFactory(okHttpClient, Util.getUserAgent(getApplication(), "Exo2"))
            HlsMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
    }

    private fun processVODsChat(messages: List<Message>, isFetched: Boolean = true): List<Message> {
        val currentUserId = user?.id
        val currentUserName = user?.nickname

        for (message in messages) {
            if (isFetched) {
                message.pushTimeMills =
                    ((message.timestamp?.seconds ?: 0) * 1000) - (startTime?.time ?: 0)
            }
            if (message.userID != null && currentUserName != null
                && message.userID == currentUserId.toString()
            ) {
                //changes displayName of current user's
                message.nickname = currentUserName
            }
        }

        val userMessages = messages.filter { it.type == MessageType.USER }
        userProcessedMessages.clear()
        userProcessedMessages.addAll(userMessages)
        return messages
    }

    private fun stopMonitoringChatMessages() {
        messagesHandler.removeCallbacksAndMessages(null)
    }

    private fun markVODAsWatched() {
        FeedRepository.vods?.find { it.id?.equals(vodId) ?: false }?.isNew = false
    }

    /**
     * set stopWatching time to avoid additional call to DB when
     * turning back from player to videos list screen
     */
    private fun setVODStopWatchingTimeLocally() {
        FeedRepository.vods?.find { streamResponse -> streamResponse.id?.equals(vodId) ?: false }
            ?.stopTimeMillis = stopWatchingTime
    }

    /**
     * Changes the playback position depending on stop time and curtains.
     * Calls when video changes in playlist or on the replay.
     * Checks whether stop time is in the range of the curtain in the beginning.
     * CURTAIN_MARGIN_MILLS - is the possible deviation of curtain from real curtain time, so the
     * curtain assumed as in the beginning when it's start time less than 500 milliseconds.
     * Also updates var endOfCurtain, as it marks whether SKIP button for curtain should be shown
     * when stop time was in the middle of some not start curtain.
     */
    fun seekToLastWatchingTime() {
        if (videoChanged) {
            var endOfCurtainOnBeginning = 0L
            curtains.forEach {
                if (stopWatchingTime + CURTAIN_MARGIN_MILLS >= it.start && stopWatchingTime < it.end) {
                    if (it.start <= CURTAIN_MARGIN_MILLS) {
                        if (it.end + 1000 >= getDurationOnVideoStart()) {
                            //case curtain duration equals length of whole video
                            endOfCurtain = alignTimeToDuration(it.end)
                        } else {
                            //case curtain on the beginning
                            endOfCurtainOnBeginning = it.end
                        }
                    } else {
                        //case curtain in the middle
                        endOfCurtain = alignTimeToDuration(it.end)
                    }
                    return@forEach
                }
            }
            val timeToSeekTo =
                if (endOfCurtainOnBeginning != 0L) endOfCurtainOnBeginning else stopWatchingTime
            player?.seekTo(timeToSeekTo)
            videoChanged = false
        }
    }

    fun shouldShowSkipButton(): Boolean = endOfCurtain != 0L

    fun showSkipButtonIfRequired() {
        if (endOfCurtain != 0L) {
            curtainShown.postValue(endOfCurtain)
            viewModelScope.launch {
                delay(500)
                endOfCurtain = 0L
            }
        }
    }

    /**
     * @timeToSeek is aligned to not exceed duration of current video
     */
    private fun alignTimeToDuration(timeToSeek: Long): Long {
        var finalTimeToSeek = timeToSeek
        val duration = getDurationOnVideoStart()
        if (duration in (CURTAIN_MARGIN_MILLS)..timeToSeek) {
            finalTimeToSeek = duration - CURTAIN_MARGIN_MILLS
        }
        return finalTimeToSeek
    }

    private fun getDurationOnVideoStart(): Long {
        var duration = getVideoDuration() ?: 0L
        if (duration < 0L) {
            duration = vodResponseDuration
        }
        return duration
    }
}