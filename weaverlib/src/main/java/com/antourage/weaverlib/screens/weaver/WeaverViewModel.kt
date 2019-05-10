package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.net.Uri
import android.view.Surface
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.decoder.DecoderCounters
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MediaSourceEventListener
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.FileNotFoundException
import java.io.IOException

class WeaverViewModel(application: Application) : BaseViewModel(application) {
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private lateinit var streamUrl: String
    public var wasStreamInitialized = false

    private lateinit var player: SimpleExoPlayer
    private var playbackStateLiveData: MutableLiveData<Int> = MutableLiveData()

    fun getPlaybackState(): LiveData<Int> {
        return playbackStateLiveData
    }

    fun getExoPlayer(streamUrl: String?): SimpleExoPlayer? {
        player = getSimpleExoplayer()
        this.streamUrl = streamUrl!!
        val mediaSource = getMediaSource(streamUrl)
        player.playWhenReady = playWhenReady
        player.seekTo(currentWindow, playbackPosition)
        player.prepare(mediaSource, true, false)
        initStatisticsListeners()
        return player
    }

    fun onResume() {
        initStatisticsListeners()
        if (player.playbackState != Player.STATE_READY) {
            player.playWhenReady = true
        }
    }

    fun onPause() {
        removeStatisticsListeners()
        if (player.playbackState == Player.STATE_READY)
            player.playWhenReady = false
    }

    fun releasePlayer() {
        playbackPosition = player.currentPosition
        currentWindow = player.currentWindowIndex
        playWhenReady = player.playWhenReady
        player.release()
    }

    fun initStatisticsListeners() {

        player.addAnalyticsListener(streamAnalyticsListener)
        player.addListener(playerEventListener)
    }

    fun removeStatisticsListeners() {
        player.removeAnalyticsListener(streamAnalyticsListener)
        player.removeListener(playerEventListener)
    }

    private fun getSimpleExoplayer(): SimpleExoPlayer {
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        return ExoPlayerFactory.newSimpleInstance(
            getApplication(),
            DefaultRenderersFactory(getApplication()),
            DefaultTrackSelector(adaptiveTrackSelection)
        )
    }

    private fun getMediaSource(streamUrl: String?): MediaSource? {
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
            val type = error?.type
   //         Toast.makeText(getApplication(), "playerError", Toast.LENGTH_LONG).show()
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
            Toast.makeText(getApplication(), "bitrate estimate = " + bitrateEstimate, Toast.LENGTH_SHORT).show()
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
            if(playbackState == Player.STATE_READY){
                wasStreamInitialized = true
            }
            playbackStateLiveData.postValue(playbackState)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {

        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if(error.cause is FileNotFoundException){

            }
            Toast.makeText(getApplication(), error.toString(), Toast.LENGTH_LONG).show()
        }

        override fun onPositionDiscontinuity(reason: Int) {

        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

        }

        override fun onSeekProcessed() {

        }
    }
    //endregion
}