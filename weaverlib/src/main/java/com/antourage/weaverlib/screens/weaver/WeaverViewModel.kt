package com.antourage.weaverlib.screens.weaver

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.screens.base.streaming.StreamingViewModel
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.rtmp.RtmpDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class WeaverViewModel(application: Application) : StreamingViewModel(application) {

    private val pollLiveData: MutableLiveData<Poll> =  MutableLiveData()
    fun getPollLiveData(): LiveData<Poll> {
        return pollLiveData
    }
    init {
        pollLiveData.postValue(repository.getCurrentPoll())
    }


    var wasStreamInitialized = false

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

}