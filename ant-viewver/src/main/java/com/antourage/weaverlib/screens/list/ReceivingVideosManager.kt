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
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.TAG

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
internal class ReceivingVideosManager {

    companion object {
        private var callback: ReceivingVideoCallback? = null
        const val LIVE_STREAMS_REQUEST_INTERVAL = 5_000L
        const val NEW_VODS_COUNT_REQUEST_INTERVAL = 61_000L
        private const val NEW_VODS_COUNT_REQUEST_DELAY = 1_000L
        internal var liveVideos: MutableList<StreamResponse> = mutableListOf()
        private var vods: MutableList<StreamResponse> = mutableListOf()
        private var vodForFab: StreamResponse? = null

        /**used to ensure that live list comes first*/
        var isFirstRequest = true
        var isFirstRequestVod = true

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
        }


        fun loadVODs(count: Int, roomRepository: RoomRepository) {
            Log.d(TAG, "Trying to load VODs")
            val response = FeedRepository.getVODsWithLastCommentAndStopTime(count, roomRepository)
            response.observeForever(object :
                Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                    if (resource != null) {
                        when (resource.status) {
                            is Status.Failure -> {
                                Log.d(
                                    TAG,
                                    "Failed to load VODs: ${resource.status.errorMessage}"
                                )
                                callback?.onVODReceived(resource)
                                response.removeObserver(this)
                            }
                            is Status.Success -> {
                                if (count == 0) {
                                    callback?.onVODReceivedInitial(resource)
                                } else {
                                    callback?.onVODReceived(resource)
                                }
                                vods = resource.status.data as MutableList<StreamResponse>
                                Log.d(TAG, "Successfully received VOD list")
                                response.removeObserver(this)
                            }
                            is Status.Loading -> {
                                callback?.onVODReceivedInitial(resource)
                                callback?.onVODReceived(resource)
                            }
                            else -> {}
                        }
                    }
                }
            })
        }

        val handlerLiveVideos = Handler(Looper.getMainLooper())
        private val handlerVODs = Handler(Looper.getMainLooper())

        fun checkShouldUseSockets(token: String) {
            if (SocketConnector.isConnected() || SocketConnector.isSocketUsed) {
                SocketConnector.connectToSockets(token)
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
                                                    startReceivingVODsForFab()
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

        fun startReceivingVODsForFab() {
            handlerVODs.postDelayed(object : Runnable {
                override fun run() {
                    if (Global.networkAvailable) {
                        val streamResponse =
                            FeedRepository.getVODsForFab(UserCache.getInstance()?.getLastViewedTime())
                        streamResponse.observeForever(object :
                            Observer<Resource<List<StreamResponse>>> {
                            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                                if (resource != null) {
                                    when (resource.status) {
                                        is Status.Failure -> {
                                            callback?.onVODForFabReceived(resource)
                                            Log.d(
                                                TAG,
                                                "Get vods list request failed for fab"
                                            )
                                            streamResponse.removeObserver(this)
                                        }
                                        is Status.Success -> {
                                            Log.d(
                                                TAG,
                                                "Successfully received vods list for fab"
                                            )
                                            callback?.onVODForFabReceived(resource)
                                            if(resource.status.data?.isNotEmpty() == true){
                                                vodForFab = resource.status.data[0]
                                            }
                                            streamResponse.removeObserver(this)
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        })
                    }
                    handlerVODs.postDelayed(this, NEW_VODS_COUNT_REQUEST_INTERVAL)
                }
            }, NEW_VODS_COUNT_REQUEST_DELAY)
        }

        fun stopReceivingVideos() {
            Log.d(TAG, "Cancelled videos list timer")
            Log.d(TAG, "Cancelled VODs count timer")
            Log.d(TAG, "Cancelled VODs list timer")
            handlerLiveVideos.removeCallbacksAndMessages(null)
            handlerVODs.removeCallbacksAndMessages(null)
            callback = null
            isFirstRequest = true
            isFirstRequestVod = true
        }

        fun pauseWhileNoNetwork() {
            handlerLiveVideos.removeCallbacksAndMessages(null)
            handlerVODs.removeCallbacksAndMessages(null)
            isFirstRequest = true
            isFirstRequestVod = true
        }

        fun pauseReceivingVideos() {
            handlerLiveVideos.removeCallbacksAndMessages(null)
            handlerVODs.removeCallbacksAndMessages(null)
            isFirstRequest = true
        }

    }

    @Keep
    internal interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived(resource: Resource<List<StreamResponse>>) {}

        fun onVODForFabReceived(resource: Resource<List<StreamResponse>>) {}

        fun onVODReceivedInitial(resource: Resource<List<StreamResponse>>) {}
    }
}