package com.antourage.weaverlib.screens.base.streaming

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import android.view.Surface
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import java.io.IOException

abstract class StreamingViewModel(application: Application) : BaseViewModel(application) {
    private var playWhenReady = true
    protected var currentWindow = 0
    private var playbackPosition: Long = 0
    private lateinit var trackSelector: DefaultTrackSelector
    protected var streamUrl: String? = null

    protected lateinit var player: SimpleExoPlayer
    private var playbackStateLiveData: MutableLiveData<Int> = MutableLiveData()

    fun getPlaybackState(): LiveData<Int> {
        return playbackStateLiveData
    }

    init {
        currentWindow = 0
    }

    abstract fun onStreamStateChanged(playbackState: Int)

    abstract fun getMediaSource(streamUrl: String?): MediaSource?

    abstract fun onVideoChanged()

    fun getExoPlayer(streamUrl: String?): SimpleExoPlayer? {
        player = getSimpleExoplayer()
        this.streamUrl = streamUrl
        val mediaSource = getMediaSource(streamUrl)
        player.playWhenReady = playWhenReady
        player.prepare(mediaSource, true, false)
        player.seekTo(currentWindow, C.TIME_UNSET)
        initStatisticsListeners()
        return player
    }

    fun getStreamGroups(): List<Format> {
        val mappedTrackInfo = trackSelector.currentMappedTrackInfo
        val list: MutableList<Format> = mutableListOf()
        mappedTrackInfo?.let {
            val trackGroupArray = mappedTrackInfo.getTrackGroups(0)
            val trackGroup = trackGroupArray[0]
            for (j in 0 until trackGroup.length) {
                list.add(trackGroup.getFormat(j))
            }
        }
        return list
    }

    fun isPlaybackPaused(): Boolean {
        return !player.playWhenReady
    }

    open fun onResume() {
        initStatisticsListeners()
        if (player.playbackState != Player.STATE_READY) {
            player.playWhenReady = true
        }

    }

    fun onPause() {
        removeStatisticsListeners()
        player.playWhenReady = false
    }

    fun releasePlayer() {
        removeStatisticsListeners()
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        playWhenReady = player.playWhenReady
        player.release()
    }

    fun onNetworkGained() {
        player.prepare(getMediaSource(streamUrl), false, true)
        player.seekTo(currentWindow, playbackPosition)
    }

    fun onResolutionChanged(pos: Int) {
        val builder = trackSelector.parameters.buildUpon()
        val i = 0
        builder
            .clearSelectionOverrides(/* rendererIndex= */i)
            .setRendererDisabled(
                /* rendererIndex= */ i,
                i == pos
            )
        val overrides: MutableList<DefaultTrackSelector.SelectionOverride> = mutableListOf()
        overrides.add(0, DefaultTrackSelector.SelectionOverride(0, pos))
        if (!overrides.isEmpty()) {
            builder.setSelectionOverride(
                /* rendererIndex= */ i,
                trackSelector.currentMappedTrackInfo?.getTrackGroups(/* rendererIndex= */i),
                overrides[0]
            )
        }
        trackSelector.setParameters(builder)
    }

    private fun initStatisticsListeners() {

        player.addAnalyticsListener(streamAnalyticsListener)
        player.addListener(playerEventListener)
    }

    fun removeStatisticsListeners() {
        player.removeAnalyticsListener(streamAnalyticsListener)
        player.removeListener(playerEventListener)
    }

    private fun getSimpleExoplayer(): SimpleExoPlayer {
        val trackSelectorParameters = DefaultTrackSelector.ParametersBuilder().build()
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
        trackSelector.parameters = trackSelectorParameters
        return ExoPlayerFactory.newSimpleInstance(
            getApplication(),
            DefaultRenderersFactory(getApplication()),
            trackSelector
        )
    }


    //region Listeners
    val streamAnalyticsListener = object : AnalyticsListener {
        override fun onSeekProcessed(eventTime: AnalyticsListener.EventTime?) {

        }

        override fun onPlaybackParametersChanged(
            eventTime: AnalyticsListener.EventTime?,
            playbackParameters: PlaybackParameters?
        ) {

        }

        override fun onPlayerError(eventTime: AnalyticsListener.EventTime?, error: ExoPlaybackException?) {
        }

        override fun onSeekStarted(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onLoadingChanged(eventTime: AnalyticsListener.EventTime?, isLoading: Boolean) {

        }

        override fun onDownstreamFormatChanged(
            eventTime: AnalyticsListener.EventTime?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?
        ) {
        }

        override fun onDrmKeysLoaded(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onMediaPeriodCreated(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onRenderedFirstFrame(eventTime: AnalyticsListener.EventTime?, surface: Surface?) {
        }

        override fun onReadingStarted(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onBandwidthEstimate(
            eventTime: AnalyticsListener.EventTime?,
            totalLoadTimeMs: Int,
            totalBytesLoaded: Long,
            bitrateEstimate: Long
        ) {
            // Toast.makeText(getApplication(), "bitrate estimate = " + bitrateEstimate, Toast.LENGTH_SHORT).show()
        }


        override fun onPlayerStateChanged(
            eventTime: AnalyticsListener.EventTime?,
            playWhenReady: Boolean,
            playbackState: Int
        ) {
        }

        override fun onDrmKeysRestored(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onDecoderDisabled(
            eventTime: AnalyticsListener.EventTime?,
            trackType: Int,
            decoderCounters: DecoderCounters?
        ) {
        }

        override fun onShuffleModeChanged(eventTime: AnalyticsListener.EventTime?, shuffleModeEnabled: Boolean) {
        }

        override fun onDecoderInputFormatChanged(
            eventTime: AnalyticsListener.EventTime?,
            trackType: Int,
            format: Format?
        ) {
        }

        override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime?, audioSessionId: Int) {
        }

        override fun onDrmSessionManagerError(eventTime: AnalyticsListener.EventTime?, error: Exception?) {
        }

        override fun onLoadStarted(
            eventTime: AnalyticsListener.EventTime?,
            loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?
        ) {
        }

        override fun onTracksChanged(
            eventTime: AnalyticsListener.EventTime?,
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {

        }

        override fun onPositionDiscontinuity(eventTime: AnalyticsListener.EventTime?, reason: Int) {

        }

        override fun onRepeatModeChanged(eventTime: AnalyticsListener.EventTime?, repeatMode: Int) {
        }

        override fun onUpstreamDiscarded(
            eventTime: AnalyticsListener.EventTime?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?
        ) {
        }

        override fun onLoadCanceled(
            eventTime: AnalyticsListener.EventTime?,
            loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?
        ) {
        }

        override fun onMediaPeriodReleased(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onTimelineChanged(eventTime: AnalyticsListener.EventTime?, reason: Int) {
        }

        override fun onDecoderInitialized(
            eventTime: AnalyticsListener.EventTime?,
            trackType: Int,
            decoderName: String?,
            initializationDurationMs: Long
        ) {
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime?,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            // Toast.makeText(context,"Number of dropped frames ="+droppedFrames, Toast.LENGTH_SHORT ).show()
        }

        override fun onDecoderEnabled(
            eventTime: AnalyticsListener.EventTime?,
            trackType: Int,
            decoderCounters: DecoderCounters?
        ) {
        }

        override fun onVideoSizeChanged(
            eventTime: AnalyticsListener.EventTime?,
            width: Int,
            height: Int,
            unappliedRotationDegrees: Int,
            pixelWidthHeightRatio: Float
        ) {
        }

        override fun onAudioUnderrun(
            eventTime: AnalyticsListener.EventTime?,
            bufferSize: Int,
            bufferSizeMs: Long,
            elapsedSinceLastFeedMs: Long
        ) {
        }

        override fun onLoadCompleted(
            eventTime: AnalyticsListener.EventTime?,
            loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?
        ) {
        }

        override fun onDrmKeysRemoved(eventTime: AnalyticsListener.EventTime?) {
        }

        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime?,
            loadEventInfo: MediaSourceEventListener.LoadEventInfo?,
            mediaLoadData: MediaSourceEventListener.MediaLoadData?,
            error: IOException?,
            wasCanceled: Boolean
        ) {

        }

        override fun onMetadata(eventTime: AnalyticsListener.EventTime?, metadata: Metadata?) {
        }
    }

    val playerEventListener = object : Player.EventListener {
        override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {

        }

        override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {

        }

        override fun onLoadingChanged(isLoading: Boolean) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            onStreamStateChanged(playbackState)
            playbackStateLiveData.postValue(playbackState)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

        }

        override fun onPlayerError(err: ExoPlaybackException) {
            if (!AntourageActivity.isNetworkAvailable) {
                playbackPosition = player.currentPosition
                currentWindow = player.currentWindowIndex
            }
            if (err.cause is BehindLiveWindowException) {
                player.prepare(getMediaSource(streamUrl), false, true)
            }

            error.postValue(err.toString())
        }

        override fun onPositionDiscontinuity(reason: Int) {
            currentWindow = player.currentWindowIndex
            onVideoChanged()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

        }

        override fun onSeekProcessed() {

        }
    }
    //endregion


}