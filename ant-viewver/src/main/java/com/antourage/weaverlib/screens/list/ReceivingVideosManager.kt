package com.antourage.weaverlib.screens.list

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.SocketConnector
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.networking.feed.FeedRepository
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.TAG

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
internal class ReceivingVideosManager {

    companion object {
        private var callback: ReceivingVideoCallback? = null
        const val LIVE_STREAMS_REQUEST_INTERVAL = 5_000L
        internal var liveVideos: MutableList<StreamResponse> = mutableListOf()
        private var vods: MutableList<StreamResponse> = mutableListOf()

        /**used to ensure that live list comes first*/
        var isFirstRequest = true

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
        }

        val handlerLiveVideos = Handler(Looper.getMainLooper())

        fun checkShouldUseSockets() {
            if (SocketConnector.isConnected() || SocketConnector.isSocketUsed) {
                SocketConnector.connectToSockets()
            } else {
                startReceivingLiveStreams()
            }
        }

        val shouldUseSockets: Boolean =
            SocketConnector.isConnected() || SocketConnector.isSocketUsed

        fun startReceivingLiveStreams(isForFab: Boolean = false) {
            handlerLiveVideos.removeCallbacksAndMessages(null)
            handlerLiveVideos.postDelayed(object : Runnable {
                override fun run() {
                    if (isForFab || Global.networkAvailable) {
                        val streamResponse =
                            FeedRepository.getLiveVideos()
                        streamResponse.observeForever(object :
                            Observer<Resource<List<StreamResponse>>> {
                            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                                if (resource != null) {
                                    when (resource.status) {
                                        is Status.Failure -> {
                                            handlerLiveVideos.postDelayed({
                                                if (isFirstRequest && Global.networkAvailable) {
                                                    startReceivingLiveStreams(isForFab)
                                                }
                                            }, 2000)
                                            callback?.onLiveBroadcastReceived(resource)
                                            Log.d(
                                                TAG,
                                                "Get live video list request failed"
                                            )
                                            streamResponse.removeObserver(this)
                                        }
                                        is Status.Success -> {
                                            Log.d(
                                                TAG,
                                                "Successfully received live video list"
                                            )
                                            if (isFirstRequest && isForFab && Global.networkAvailable) {
                                                isFirstRequest = false
                                                Handler(Looper.getMainLooper()).postDelayed({
//                                                    startReceivingVODsForFab()
                                                }, 1200)
                                            }
                                            callback?.onLiveBroadcastReceived(resource)
                                            if(resource.status.data !=null){
                                                liveVideos = resource.status.data as MutableList<StreamResponse>
                                            }
                                            streamResponse.removeObserver(this)
                                        }
                                        else -> {
                                        }
                                    }
                                }
                            }
                        })
                    }
                    handlerLiveVideos.postDelayed(this, LIVE_STREAMS_REQUEST_INTERVAL)
                }
            }, 0)
        }

        fun stopReceivingVideos() {
            Log.d(TAG, "Cancelled config fetch timer")
            Log.d(TAG, "Cancelled VODs count timer")
            Log.d(TAG, "Cancelled VODs list timer")
            handlerLiveVideos.removeCallbacksAndMessages(null)
            callback = null
            isFirstRequest = true
        }

        fun pauseWhileNoNetwork() {
            handlerLiveVideos.removeCallbacksAndMessages(null)
            isFirstRequest = true
        }

        fun pauseReceivingVideos() {
            handlerLiveVideos.removeCallbacksAndMessages(null)
            isFirstRequest = true
        }

    }

    @Keep
    internal interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODForFabReceived(resource: Resource<List<StreamResponse>>) {}
    }
}