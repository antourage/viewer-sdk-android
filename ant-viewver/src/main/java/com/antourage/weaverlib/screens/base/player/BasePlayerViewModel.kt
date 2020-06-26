package com.antourage.weaverlib.screens.base.player

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.SingleLiveEvent
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.models.LiveOpenedRequest
import com.antourage.weaverlib.other.models.StatisticWatchVideoRequest
import com.antourage.weaverlib.other.models.VideoOpenedRequest
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.SocketConnector
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.statistic.StatisticActions
import com.antourage.weaverlib.other.statistic.Stopwatch
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.ReceivingVideosManager
import com.antourage.weaverlib.screens.vod.VideoViewModel
import com.antourage.weaverlib.screens.weaver.PlayerViewModel
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.TAG
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultLoadControl.Builder
import com.google.android.exoplayer2.ExoPlaybackException.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistTracker
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultAllocator
import java.sql.Timestamp

internal abstract class BasePlayerViewModel(application: Application) : BaseViewModel(application) {
    companion object {
        private const val END_VIDEO_CALLBACK_OFFSET_MS = 200
    }

    override fun onCleared() {
        postVideoIsClosed(streamId)
        super.onCleared()
    }

    private var playWhenReady = true
    protected var currentWindow = 0
    var streamId: Int? = null
    var currentlyWatchedVideoId: Int? = null
    protected var streamUrl: String? = null
    private var playbackStateLiveData: MutableLiveData<Int> = MutableLiveData()

    //var to track whether opened video request was sent in order to send close
    private var lastStatOpenedID: Int? = null

    //should be used for all kinds of error, which user should be informed with.
    //will always show same error message to user, that's why used Boolean
    var errorLiveData: SingleLiveEvent<Boolean> = SingleLiveEvent()

    private var stopwatch = Stopwatch()
    protected var resetChronometer = true

    protected var player: SimpleExoPlayer? = null
    private lateinit var trackSelector: DefaultTrackSelector

    private var batteryStatus: Intent? = null

    val requestingStreamInfoHandler = Handler()
    val currentStreamViewsLiveData: MutableLiveData<Long> = MutableLiveData()

    init {
        currentWindow = 0
    }

    fun setStreamId(streamId: Int) {
        this.streamId = streamId
    }

    fun getPlaybackState(): LiveData<Int> = playbackStateLiveData

    abstract fun onStreamStateChanged(playbackState: Int)

    abstract fun getMediaSource(streamUrl: String?): MediaSource

    open fun onTrackEnd() {}

    open fun registerCallbacks(windowIndex: Int) {}

    open fun onVideoChanged() {}

    //should be used only in vod
    open fun onOpenStatisticUpdate(vodId: Int) {}

    //should be used only in live
    open fun onUpdateBannerInfo(banner: AdBanner?) {}

    open fun onResume() {
        initStatisticsListeners()
        if (player?.playbackState != Player.STATE_READY) {
            player?.playWhenReady = true
        }
        batteryStatus = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            getApplication<Application>().registerReceiver(null, filter)
        }

        if (this@BasePlayerViewModel is PlayerViewModel) {
            checkShouldUseSockets()
        }
    }

    open fun onPause() {
        removeStatisticsListeners()
        player?.playWhenReady = false
        stopwatch.stopIfRunning()
        stopUpdatingCurrentStreamInfo()
    }

    open fun onPauseSocket(shouldDisconnectSocket: Boolean = true) {
        disconnectSocket(shouldDisconnectSocket)
    }

    fun getExoPlayer(streamUrl: String): SimpleExoPlayer? {
        player = getSimpleExoPlayer()
        this.streamUrl = streamUrl
        player?.playWhenReady = playWhenReady
        player?.prepare(getMediaSource(streamUrl), false, true)
        player?.seekTo(currentWindow, C.TIME_UNSET)
        initStatisticsListeners()
        return player
    }

    fun isPlaybackPaused(): Boolean = !(player?.playWhenReady ?: false)

    fun releasePlayer() {
        removeStatisticsListeners()
        currentWindow = player?.currentWindowIndex ?: 0
        playWhenReady = player?.playWhenReady ?: false
        player?.release()
    }

    fun onNetworkGained() {
        player?.prepare(getMediaSource(streamUrl), false, true)
        player?.seekTo(currentWindow, player?.currentPosition ?: 0)
    }

    private fun initStatisticsListeners() {
        player?.addAnalyticsListener(streamAnalyticsListener)
        player?.addListener(playerEventListener)
    }

    fun removeStatisticsListeners() {
        player?.removeAnalyticsListener(streamAnalyticsListener)
        player?.removeListener(playerEventListener)
    }

    private fun getSimpleExoPlayer(): SimpleExoPlayer {
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder().build()
        val loadControl = Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                2500,
                5000,
                2500,
                2500
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .createDefaultLoadControl()
        return ExoPlayerFactory.newSimpleInstance(
            getApplication(),
            DefaultRenderersFactory(getApplication()),
            trackSelector,
            loadControl
        )
    }

    private fun getBatteryLevel(): Int {
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
    }


    protected fun playNextTrack() {
        val nextWindowIndex = player?.nextWindowIndex
        if (nextWindowIndex != C.INDEX_UNSET && nextWindowIndex != null) {
            player?.seekTo(nextWindowIndex, C.TIME_UNSET)
            player?.playWhenReady = true
        }
    }

    protected fun playPrevTrack() {
        val previousWindowIndex = player?.previousWindowIndex
        if (previousWindowIndex != C.INDEX_UNSET && previousWindowIndex != null) {
            player?.seekTo(previousWindowIndex, C.TIME_UNSET)
            player?.playWhenReady = true
        }
    }

    protected fun rewindAndPlayTrack() {
        player?.seekTo(currentWindow, C.TIME_UNSET)
        player?.playWhenReady = true
    }

    protected fun getCurrentDuration() = player?.duration
    protected fun getCurrentPosition() = player?.currentPosition

    fun hasPrevTrack(): Boolean = !(player == null || player?.previousWindowIndex == C.INDEX_UNSET)

    fun hasNextTrack(): Boolean = !(player == null || player?.nextWindowIndex == C.INDEX_UNSET)

    private fun sendStatisticData(
        statisticAction: StatisticActions,
        span: String = "00:00:00",
        timestamp: String = Timestamp(System.currentTimeMillis()).toString()
    ) {
        streamId?.let { streamId ->
            StatisticWatchVideoRequest(
                streamId,
                statisticAction.ordinal,
                getBatteryLevel(),
                timestamp,
                span
            )
        }?.let { statsItem ->
            when (this) {
                is VideoViewModel -> Repository.statisticWatchVOD(statsItem)
                is PlayerViewModel -> Repository.statisticWatchLiveStream(statsItem)
                else -> {
                }
            }
        }
    }

    private fun postVideoIsOpen(
        timestamp: String = Timestamp(System.currentTimeMillis()).toString()
    ) {
        streamId?.let { id ->
            when (this) {
                is VideoViewModel -> {
                    val response = Repository.postVideoOpened(
                        VideoOpenedRequest(id, getBatteryLevel(), timestamp)
                    )
                    response.observeForever(object : Observer<Resource<SimpleResponse>> {
                        override fun onChanged(resource: Resource<SimpleResponse>?) {
                            if (resource != null) {
                                when (resource.status) {
                                    is Status.Failure -> {
                                        Log.d(
                                            "STAT_OPEN",
                                            "Failed to send /open: ${resource.status.errorMessage}"
                                        )
                                        response.removeObserver(this)
                                    }
                                    is Status.Success -> {
                                        Log.d("STAT_OPEN", "Successfully sent /open")
                                        lastStatOpenedID = id
                                        onOpenStatisticUpdate(id)
                                        response.removeObserver(this)
                                    }
                                }
                            }
                        }
                    })
                }
                is PlayerViewModel -> {
                    val response = Repository.postLiveOpened(
                        LiveOpenedRequest(id, getBatteryLevel(), timestamp)
                    )
                    response.observeForever(object : Observer<Resource<AdBanner>> {
                        override fun onChanged(resource: Resource<AdBanner>?) {
                            if (resource != null) {
                                when (resource.status) {
                                    is Status.Failure -> {
                                        Log.d(
                                            "STAT_OPEN",
                                            "Failed to send /open: ${resource.status.errorMessage}"
                                        )
                                        response.removeObserver(this)
                                    }
                                    is Status.Success -> {
                                        Log.d("STAT_OPEN", "Successfully sent /open")
                                        lastStatOpenedID = id
                                        onUpdateBannerInfo(resource.status.data)
                                        response.removeObserver(this)
                                    }
                                }
                            }
                        }
                    })
                }
                else -> {
                    return
                }
            }

        }
    }

    protected fun postVideoIsClosed(
        videoId: Int? = null,
        timestamp: String = Timestamp(System.currentTimeMillis()).toString()
    ) {
        val currentId = videoId ?: streamId
        currentId?.let { id ->
            if (id == lastStatOpenedID) {
                when (this) {
                    is VideoViewModel -> Repository.postVideoClosedInternalObserve(
                        VideoClosedRequest(id, getBatteryLevel(), timestamp, stopwatch.toString())
                    )
                    is PlayerViewModel -> Repository.postLiveClosedInternalObserve(
                        LiveClosedRequest(id, getBatteryLevel(), timestamp, stopwatch.toString())
                    )
                    else -> {
                    }
                }
                lastStatOpenedID = null
            }
        }
    }


    //region Listeners

    private val streamAnalyticsListener = object : AnalyticsListener {
        override fun onTracksChanged(
            eventTime: AnalyticsListener.EventTime,
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            Log.d("TAG", "onTracksChanged")
        }
    }

    private val playerEventListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    if (isPlaybackPaused()) {
                        stopwatch.stopIfRunning()
                    } else {
                        if (resetChronometer) {
                            postVideoIsOpen()
                            stopwatch.start()
                            resetChronometer = false
                        } else {
                            stopwatch.resume()
                        }
                    }
                }
                Player.STATE_ENDED -> {
                    stopwatch.stopIfRunning()
                    onLiveStreamEnded()
                }

                Player.STATE_IDLE -> {
                    stopwatch.stopIfRunning()
                }
            }
            onStreamStateChanged(playbackState)
            playbackStateLiveData.postValue(playbackState)
        }

        override fun onPlayerError(err: ExoPlaybackException) {
            if (ConnectionStateMonitor.isNetworkAvailable(application.baseContext)) {
                currentWindow = player?.currentWindowIndex ?: 0
                if (err.cause !is HlsPlaylistTracker.PlaylistStuckException) errorLiveData.value =
                    true
            }
            Log.d(TAG, "player error: ${err.cause.toString()}")
            Log.d(
                TAG, when (err.type) {
                    TYPE_SOURCE -> "error type: ${err.type} error message: ${err.sourceException.message}"
                    TYPE_RENDERER -> "error type: ${err.type} error message: ${err.rendererException.message}"
                    TYPE_UNEXPECTED -> "error type: ${err.type} error message: ${err.unexpectedException.message}"
                    TYPE_REMOTE -> "error type: ${err.type} error message: ${err.message}"
                    TYPE_OUT_OF_MEMORY -> "error type: ${err.type} error message: ${err.outOfMemoryError.message}"
                    else -> err.message
                } ?: ""
            )
            if (err.cause is BehindLiveWindowException) {
                player?.prepare(getMediaSource(streamUrl), false, true)
            } else if (err.cause is HlsPlaylistTracker.PlaylistStuckException) {
                if (this@BasePlayerViewModel is PlayerViewModel) {
                    player?.prepare(getMediaSource(streamUrl), false, true)
                }
                errorLiveData.value = true
                error.postValue(err.toString())
            } else {
                error.postValue(err.toString())
            }
        }

        override fun onPositionDiscontinuity(reason: Int) {
            currentWindow = player?.currentWindowIndex ?: 0
            stopUpdatingCurrentStreamInfo()
            onVideoChanged()
            if (this@BasePlayerViewModel is PlayerViewModel) {
                checkShouldUseSockets()
//                currentlyWatchedVideoId?.let { subscribeToCurrentStreamInfo(it) }
            }
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            if (player != null) {
                if (player!!.duration != C.TIME_UNSET) {
                    player!!.createMessage { _: Int, _: Any? -> onTrackEnd() }
                        .setHandler(Handler())
                        .setPosition(
                            currentWindow,
                            player!!.duration - END_VIDEO_CALLBACK_OFFSET_MS
                        )
                        .setDeleteAfterDelivery(false)
                        .send()
                    registerCallbacks(currentWindow)
                }
            }
        }
    }
    //endregion

    protected fun createAdCallback(
        windowIndex: Int,
        curtainRangeMillis: CurtainRangeMillis,
        onAddStarted: (curtainEndTime: Long, windowIndex: Int) -> Unit
    ) {
        player!!.createMessage { _, _ -> onAddStarted(curtainRangeMillis.end, windowIndex) }
            .setHandler(Handler())
            .setPosition(windowIndex, curtainRangeMillis.start)
            .setDeleteAfterDelivery(false)
            .send()
    }

    /**
     * method used to update live viewers count in real time on player screen ONLY for LIVE
     */
    private fun subscribeToCurrentStreamInfo(currentlyWatchedVideoId: Int) {
        requestingStreamInfoHandler.postDelayed(object : Runnable {
            override fun run() {
                if (Global.networkAvailable) {
                    val currentStreamInfo = Repository.getLiveViewers(currentlyWatchedVideoId)
                    val streamInfoObserver = object : Observer<Resource<Viewers>> {
                        override fun onChanged(resource: Resource<Viewers>?) {
                            if (resource != null) {
                                when (val result = resource.status) {
                                    is Status.Failure -> {
                                        currentStreamInfo.removeObserver(this)
                                    }
                                    is Status.Success -> {
                                        val streamInfo = result.data
                                        currentStreamViewsLiveData.postValue(streamInfo?.viewers)
                                        currentStreamInfo.removeObserver(this)
                                    }
                                }
                            }
                        }
                    }
                    currentStreamInfo.observeForever(streamInfoObserver)
                    requestingStreamInfoHandler.postDelayed(
                        this,
                        ReceivingVideosManager.LIVE_STREAMS_REQUEST_INTERVAL
                    )
                }
            }
        }, 0)
    }

    private fun stopUpdatingCurrentStreamInfo() {
        requestingStreamInfoHandler.removeCallbacksAndMessages(null)
    }

    private fun disconnectSocket(shouldDisconnectSocket: Boolean = true) {
        if (shouldDisconnectSocket) SocketConnector.disconnectSocket()
        SocketConnector.socketConnection.removeObserver(socketConnectionObserver)
        SocketConnector.newLivesLiveData.removeObserver(liveFromSocketObserver)
    }

    private fun checkShouldUseSockets() {
        initSocketListeners()
        if (ReceivingVideosManager.shouldUseSockets) {
            UserCache.getInstance(getApplication<Application>().applicationContext)?.getToken()
                ?.let {
                    SocketConnector.connectToSockets(it)
                }
        } else {
            if (this@BasePlayerViewModel is PlayerViewModel) {
                this.currentlyWatchedVideoId?.let { subscribeToCurrentStreamInfo(it) }
            }
        }
    }

    private val socketConnectionObserver =
        Observer<SocketConnector.SocketConnection> { socketConnection ->
            if (socketConnection == SocketConnector.SocketConnection.DISCONNECTED) {
                if (Global.networkAvailable && this@BasePlayerViewModel is PlayerViewModel) this.currentlyWatchedVideoId?.let {
                    subscribeToCurrentStreamInfo(
                        it
                    )
                }
            }
        }

    private val liveFromSocketObserver = Observer<List<StreamResponse>> { newStreams ->
        if (!newStreams.isNullOrEmpty()) {
            for (stream in newStreams) {
                if(stream.id == this.currentlyWatchedVideoId){
                    currentStreamViewsLiveData.postValue(stream.viewersCount)
                    break
                }
            }
        }
    }

    private fun initSocketListeners() {
        SocketConnector.socketConnection.observeForever(socketConnectionObserver)
        SocketConnector.newLivesLiveData.observeForever(liveFromSocketObserver)
    }

    open fun onLiveStreamEnded() {}
}