package com.antourage.weaverlib.screens.vod

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import android.os.Handler
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.StreamResponse
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Util
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.base.chat.ChatViewModel
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.firebase.Timestamp
import okhttp3.OkHttpClient
import java.util.*
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject


class VideoViewModel @Inject constructor(application: Application, val repository: Repository) : ChatViewModel(application) {

    fun onVideoStarted(streamId:Int){
        val handler = Handler()

        val runnable = object : Runnable {
            override fun run() {
                    val pos = (player.currentPosition / 1000) - 1
                    val list = repository.getMessagesList(currentWindow + 1)
                    val listToDisplay = mutableListOf<Message>()
                    for (i in 0 until list.size) {
                        if ((list[i].timestamp - 1) <= pos) {
                            val msg = Message(
                                "", list[i].nickname,
                                list[i].nickname, list[i].text, MessageType.USER, Timestamp(Date())
                            )
                            msg.id = list[i].timestamp.toString()
                            listToDisplay.add(msg)
                        }
                    }
                    messagesLiveData.value = listToDisplay
                // Repeat every  seconds
                handler.postDelayed(this, 500)
            }
        }

        handler.post(runnable)
    }


    private val currentVideo: MutableLiveData<StreamResponse> = MutableLiveData()

    fun getCurrentVideo(): LiveData<StreamResponse> {
        return currentVideo
    }

    override fun onVideoChanged() {
        val list: List<StreamResponse> = repository.getListOfVideos()
        messagesLiveData.value = mutableListOf()
        currentVideo.postValue(list[currentWindow])
        player.playWhenReady = true
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

    /**
     * using this to create playlist. For now, was approved
     */
    override fun getMediaSource(streamUrl: String?): MediaSource? {
        val list: List<StreamResponse> = repository.getListOfVideos()
        val mediaSources = arrayOfNulls<MediaSource>(list.size)
        for (i in 0 until list.size) {
            mediaSources[i] = buildSimpleMediaSource(list[i].hlsUrl)
        }
        return ConcatenatingMediaSource(*mediaSources)
    }

    override fun onStreamStateChanged(playbackState: Int) {

    }

    /**
     * videos do not play on Android 5 without this additional header. IDK why
     */
    private fun buildSimpleMediaSource(uri: String): MediaSource {

        val okHttpClient = OkHttpClient.Builder ()
            .addNetworkInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("Connection", "close").build()
                chain.proceed(request)
            }
            .build()

        val dataSourceFactory = OkHttpDataSourceFactory(okHttpClient,Util.getUserAgent(getApplication(), "Exo2"))
        return HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(uri))
    }


}