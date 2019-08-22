package com.antourage.weaverlib.screens.list

import android.arch.lifecycle.Observer
import android.os.Handler
import android.support.annotation.Keep
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.Repository

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
class ReceivingVideosManager {

    companion object {
        private var callback: ReceivingVideoCallback? = null
        const val STREAMS_REQUEST_DELAY = 5000L
        private var lastReceivedData: Resource<List<StreamResponse>>? = null

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
            if (lastReceivedData != null)
                ReceivingVideosManager.callback?.onLiveBroadcastReceived(lastReceivedData!!)
        }

        val handlerCall = Handler()

        fun startReceivingVideos() {
            handlerCall.postDelayed(object : Runnable {
                override fun run() {
                    val streamResponse =
                        Repository(ApiClient.getClient().webService).getListOfStreams()
                    streamResponse.observeForever(object :
                        Observer<Resource<List<StreamResponse>>> {
                        override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                            if (resource != null) {
                                callback?.onLiveBroadcastReceived(resource)
                                when (resource.status) {
                                    is Status.Failure, is Status.Success -> {
                                        lastReceivedData = resource
                                        streamResponse.removeObserver(this)
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
            callback = null
        }
    }

    @Keep
    interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived()
    }
}