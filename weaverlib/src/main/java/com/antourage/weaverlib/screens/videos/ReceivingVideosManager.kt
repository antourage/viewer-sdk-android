package com.antourage.weaverlib.screens.videos

import android.os.Handler
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.Repository


class ReceivingVideosManager(val callback:ReceivingVideoCallback){

    companion object {
        const val STREAMS_REQUEST_DELAY = 5000L
    }

    val handlerCall = Handler()

    fun startReceivingVideos(){
        handlerCall.postDelayed(object : Runnable {
            override fun run() {
                val streamResponse = Repository().getListOfStreams()
                streamResponse.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                    override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                        if (resource != null) {
                            callback.onLiveBroadcastReceived(resource)
                            when(resource.state){
                                State.FAILURE,State.SUCCESS ->{
                                    streamResponse.removeObserver(this)
                                }
                                else -> {}
                            }
                        }
                    }
                })
                handlerCall.postDelayed(this, STREAMS_REQUEST_DELAY)
            }
        }, 0)
    }
    fun stopReceivingVideos(){
        handlerCall.removeCallbacksAndMessages(null)
    }
    interface ReceivingVideoCallback{
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived()
    }
}