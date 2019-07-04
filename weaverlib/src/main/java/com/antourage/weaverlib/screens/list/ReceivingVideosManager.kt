package com.antourage.weaverlib.screens.list

import android.arch.lifecycle.Observer
import android.os.Handler
import android.support.annotation.Keep
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.Repository

@Keep
class ReceivingVideosManager() {


    companion object {
        lateinit var callback: ReceivingVideoCallback
        const val STREAMS_REQUEST_DELAY = 5000L
        private var lastReceivedData: Resource<List<StreamResponse>>? = null


        fun newInstance(callback: ReceivingVideoCallback){
            ReceivingVideosManager.callback = callback
            if(lastReceivedData != null)
                ReceivingVideosManager.callback.onLiveBroadcastReceived(lastReceivedData!!)
        }
        val handlerCall = Handler()

        fun startReceivingVideos() {
            handlerCall.postDelayed(object : Runnable {
                override fun run() {
                    //TODO 4/7/2019 change through DI
                    val streamResponse = Repository(ApiClient.getInitialClient().webService).getListOfStreams()
                    streamResponse.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                        override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                            if (resource != null) {
                                callback.onLiveBroadcastReceived(resource)
                                when (resource.state) {
                                    State.FAILURE, State.SUCCESS -> {
                                        lastReceivedData = resource
                                        streamResponse.removeObserver(this)
                                    }
                                    else -> {
                                    }
                                }
                            }
                        }
                    })
                    handlerCall.postDelayed(this, STREAMS_REQUEST_DELAY)
                }
            }, 0)
        }

        fun stopReceivingVideos() {
            handlerCall.removeCallbacksAndMessages(null)
        }
    }

    @Keep
    interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived()
    }
}