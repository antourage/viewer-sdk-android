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
        private var liveVideos: Resource<List<StreamResponse>>? = null
        private var vods: Resource<List<StreamResponse>>? = null

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
        }

        fun loadVODs() {
            val response = Repository().getVODs()
            response.observeForever(object :
                Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                    if (resource != null) {
                        when (resource.status) {
                            is Status.Failure -> {
                                callback?.onVODReceived(resource)
                                response.removeObserver(this)
                            }
                            is Status.Success -> {
                                callback?.onVODReceived(resource)
                                vods = resource
                                response.removeObserver(this)
                            }
                            is Status.Loading -> {
                                callback?.onVODReceived(resource)
                            }
                        }
                    }
                }
            })
        }

        val handlerCall = Handler()

        fun startReceivingVideos() {
            handlerCall.postDelayed(object : Runnable {
                override fun run() {
                    val streamResponse =
                        Repository().getLiveVideos()
                    streamResponse.observeForever(object :
                        Observer<Resource<List<StreamResponse>>> {
                        override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                            if (resource != null) {
                                when (resource.status) {
                                    is Status.Failure -> {
                                        callback?.onLiveBroadcastReceived(resource)
                                        streamResponse.removeObserver(this)
                                    }
                                    is Status.Success -> {
                                        callback?.onLiveBroadcastReceived(resource)
                                        liveVideos = resource
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

        fun onVODReceived(resource: Resource<List<StreamResponse>>) {}
    }
}