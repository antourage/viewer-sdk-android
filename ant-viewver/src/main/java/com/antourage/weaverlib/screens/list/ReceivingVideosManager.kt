package com.antourage.weaverlib.screens.list

import android.os.Handler
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.ui.fab.AntourageFab

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
internal class ReceivingVideosManager {

    companion object {
        private var callback: ReceivingVideoCallback? = null
        const val STREAMS_REQUEST_DELAY = 5000L
        private var liveVideos: Resource<List<StreamResponse>>? = null
        private var vods: Resource<List<StreamResponse>>? = null

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
        }

        fun loadVODs(count: Int) {
            Log.d(AntourageFab.TAG, "Trying to load VODs")
            val response = Repository().getVODs(count)
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
                                if (count == 0) {
                                    callback?.onVODReceivedInitial(resource)
                                } else {
                                    callback?.onVODReceived(resource)
                                }
                                vods = resource
                                response.removeObserver(this)
                            }
                            is Status.Loading -> {
                                callback?.onVODReceivedInitial(resource)
                                callback?.onVODReceived(resource)
                            }
                        }
                    }
                }
            })
        }

        val handlerCall = Handler()

        fun startReceivingVideos() {
            Log.d(AntourageFab.TAG, "Started videos list timer")
            handlerCall.postDelayed(object : Runnable {
                override fun run() {
                    if (Global.networkAvailable) {
                        val streamResponse =
                            Repository().getLiveVideos()
                        streamResponse.observeForever(object :
                            Observer<Resource<List<StreamResponse>>> {
                            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                                if (resource != null) {
                                    when (resource.status) {
                                        is Status.Failure -> {
                                            callback?.onLiveBroadcastReceived(resource)
                                            Log.d(AntourageFab.TAG, "Get live video list request failed")
                                            streamResponse.removeObserver(this)
                                        }
                                        is Status.Success -> {
                                            Log.d(AntourageFab.TAG, "Successfully received live video list")
                                            callback?.onLiveBroadcastReceived(resource)
                                            liveVideos = resource
                                            streamResponse.removeObserver(this)
                                        }
                                    }
                                }
                            }
                        })
                    }
                    handlerCall.postDelayed(this, STREAMS_REQUEST_DELAY)
                }
            }, 0)
        }

        fun stopReceivingVideos() {
            Log.d(AntourageFab.TAG, "Cancelled videos list timer")
            handlerCall.removeCallbacksAndMessages(null)
            callback = null
        }

        fun getNewVODsCount() {
            val response = Repository().getNewVODsCount()
            response.observeForever(object :
                Observer<Resource<Int>> {
                override fun onChanged(resource: Resource<Int>?) {
                    if (resource != null) {
                        when (resource.status) {
                            is Status.Failure -> {
                                callback?.onNewVideosCount(resource)
                                response.removeObserver(this)
                            }
                            is Status.Success -> {
                                Log.d("VOD_COUNT_TAG", resource.toString())
                                callback?.onNewVideosCount(resource)
                                response.removeObserver(this)
                            }
                        }
                    }
                }
            })
        }
    }

    @Keep
    internal interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived(resource: Resource<List<StreamResponse>>) {}

        fun onVODReceivedInitial(resource: Resource<List<StreamResponse>>) {}

        fun onNewVideosCount(resource: Resource<Int>) {}
    }
}