package com.antourage.weaverlib.screens.base.player

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.graphics.Bitmap
import android.util.Log
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.source.BehindLiveWindowException
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector

abstract class BasePlayerViewModel(application: Application) : BaseViewModel(application) {
    private var playWhenReady = true
    protected var currentWindow = 0
    private var playbackPosition: Long = 0
    protected var streamUrl: String? = null
    private var playbackStateLiveData: MutableLiveData<Int> = MutableLiveData()

    protected lateinit var player: SimpleExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    init {
        currentWindow = 0
    }

    fun getPlaybackState(): LiveData<Int> = playbackStateLiveData

    abstract fun onStreamStateChanged(playbackState: Int)

    abstract fun getMediaSource(streamUrl: String?): MediaSource?

    abstract fun onVideoChanged()

    open fun onResume() {
        initStatisticsListeners()
        if (player.playbackState != Player.STATE_READY) {
            player.playWhenReady = true
        }
    }

    open fun onPause() {
        removeStatisticsListeners()
        player.playWhenReady = false
    }

    fun getExoPlayer(streamUrl: String): SimpleExoPlayer? {
        player = getSimpleExoPlayer()
        this.streamUrl = streamUrl
        player.playWhenReady = playWhenReady
        player.prepare(getMediaSource(streamUrl), true, false)
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

    fun isPlaybackPaused(): Boolean = !player.playWhenReady

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
        if (overrides.isNotEmpty()) {
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

    private fun getSimpleExoPlayer(): SimpleExoPlayer {
        val adaptiveTrackSelection = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(adaptiveTrackSelection)
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder().build()
        return ExoPlayerFactory.newSimpleInstance(
            getApplication(),
            DefaultRenderersFactory(getApplication()),
            trackSelector
        )
    }

    //region Listeners
    private val streamAnalyticsListener = object : AnalyticsListener {}

    private val playerEventListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            onStreamStateChanged(playbackState)
            playbackStateLiveData.postValue(playbackState)
        }

        override fun onPlayerError(err: ExoPlaybackException) {
            if (ConnectionStateMonitor.isNetworkAvailable(application.baseContext)) {
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
    }
    //endregion
}