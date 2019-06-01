package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import android.os.Handler
import android.provider.ContactsContract
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.screens.base.streaming.StreamingViewModel
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import android.support.v4.os.HandlerCompat.postDelayed



class VideoViewModel(application: Application) : StreamingViewModel(application) {

    private var messagesLiveData: MutableLiveData<List<Message>> = MutableLiveData()

    fun getMessagesLiveData(): LiveData<List<Message>> {
        return messagesLiveData
    }

    init {
        messagesLiveData.postValue(listOf())
    }

    fun onVideoStarted(streamId:Int){
        val handler = Handler()

// Define the code block to be executed
        val runnable = object : Runnable {
            override fun run() {
                // Insert custom code here
                val pos = player.currentPosition/1000
                val list = repository.getMessagesList(currentWindow+1)
                val listToDisplay = mutableListOf<Message>()
                for (i in 0 until list.size){
                    if((list[i].timestamp-1)<=pos){
                        listToDisplay.add(Message(list[i].timestamp.toString(),"",list[i].nickname,
                            list[i].nickname,list[i].text))
                    }
                }
                messagesLiveData.value = listToDisplay
                // Repeat every  seconds
                handler.postDelayed(this, 500)
            }
        }

// Start the Runnable immediately
        handler.post(runnable)
    }

    fun addMessage(text: String,nickname: String) {
        if (!text.isEmpty() && !text.isBlank()) {
            val temp: MutableList<Message> = (messagesLiveData.value)!!.toMutableList()
            temp.add(
                Message(
                    (temp.size + 1).toString(), null,
                    "osoluk@leobit.co", nickname, text, null
                )
            )
            messagesLiveData.postValue(temp as List<Message>)
        }
    }

    private val currentVideo: MutableLiveData<StreamResponse> = MutableLiveData()

    fun getCurrentVideo(): LiveData<StreamResponse> {
        return currentVideo
    }

    override fun onVideoChanged() {
        val list: List<StreamResponse> = repository.getListOfVideos()
        messagesLiveData.value = mutableListOf()
        currentVideo.postValue(list[currentWindow])
    }

    fun setCurrentPlayerPosition(videoId: Int) {
        currentWindow = findVideoPositionById(videoId)
    }

    private fun findVideoPositionById(videoId: Int): Int {
        val list: List<StreamResponse> = repository.getListOfVideos()
        for (i in 0 until list.size) {
            if (list[i].streamId == videoId) {
                currentVideo.postValue(list[i])
                return i
            }
        }
        return -1
    }

    override fun getMediaSource(streamUrl: String?): MediaSource? {
        val list: List<StreamResponse> = repository.getListOfVideos()
        val mediaSources = arrayOfNulls<MediaSource>(list.size)
        for (i in 0 until list.size) {
            mediaSources[i] = buildSimpleMediaSource(list[i].hlsUrl)
        }
        val mediaSource = ConcatenatingMediaSource(*mediaSources)
        return mediaSource
    }

    override fun onStreamStateChanged(playbackState: Int) {

    }

    private fun buildSimpleMediaSource(uri: String): MediaSource {
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(
            getApplication(),
            Util.getUserAgent(getApplication(), "Exo2"), defaultBandwidthMeter
        )
        //TODO 10/5/2018 choose one
        //hls
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(uri))
    }


}