package com.antourage.weaverlib.screens.list

import android.os.Handler
import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.ui.fab.AntourageFab

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
        private var liveVideos: Resource<List<StreamResponse>>? = null
        private var vods: Resource<List<StreamResponse>>? = null
        private var vodsForFab: Resource<List<StreamResponse>>? = null
        /**used to ensure that live list comes first*/
        private var isFirstRequest = true

        fun setReceivingVideoCallback(callback: ReceivingVideoCallback) {
            ReceivingVideosManager.callback = callback
        }

        fun loadVODs(count: Int,  roomRepository: RoomRepository) {
            Log.d(AntourageFab.TAG, "Trying to load VODs")
            val response = Repository.getVODsWithLastCommentAndStopTime(count,roomRepository)
            response.observeForever(object :
                Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                    if (resource != null) {
                        when (resource.status) {
                            is Status.Failure -> {
                                Log.d(AntourageFab.TAG, "Failed to load VODs: ${resource.status.errorMessage}")
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
                                Log.d(AntourageFab.TAG, "Successfully received VOD list")
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

        val handlerLiveVideos = Handler()
        private val handlerVODs = Handler()

        fun startReceivingLiveStreams(isForFab: Boolean = false) {
            Log.d(AntourageFab.TAG, "Started videos list timer")
            handlerLiveVideos.postDelayed(object : Runnable {
                override fun run() {
                    if (isForFab || Global.networkAvailable) {
                        val streamResponse =
                            Repository.getLiveVideos()
                        streamResponse.observeForever(object :
                            Observer<Resource<List<StreamResponse>>> {
                            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                                if (resource != null) {
                                    if(isFirstRequest && isForFab) {
                                        isFirstRequest = false
                                        Handler().postDelayed({
                                            startReceivingVODsForFab()
                                        }, 2000)
                                    }
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
                    handlerLiveVideos.postDelayed(this, LIVE_STREAMS_REQUEST_INTERVAL)
                }
            }, 0)
        }

        fun startReceivingVODsForFab(){
            handlerVODs.postDelayed(object : Runnable {
                override fun run() {
                    if (Global.networkAvailable) {
                        val streamResponse =
                            Repository.getVODsForFab()
                        streamResponse.observeForever(object :
                            Observer<Resource<List<StreamResponse>>> {
                            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                                if (resource != null) {
                                    when (resource.status) {
                                        is Status.Failure -> {
                                            callback?.onVODForFabReceived(resource)
                                            Log.d(AntourageFab.TAG, "Get vods list request failed")
                                            streamResponse.removeObserver(this)
                                        }
                                        is Status.Success -> {
                                            Log.d(AntourageFab.TAG, "Successfully received vods list")
                                            callback?.onVODForFabReceived(resource)
                                            vodsForFab = resource
                                            streamResponse.removeObserver(this)
                                        }
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
            Log.d(AntourageFab.TAG, "Cancelled videos list timer")
            Log.d(AntourageFab.TAG, "Cancelled VODs count timer")
            Log.d(AntourageFab.TAG, "Cancelled VODs list timer")
            handlerLiveVideos.removeCallbacksAndMessages(null)
            handlerVODs.removeCallbacksAndMessages(null)
            callback = null
            isFirstRequest = true
        }
    }

    @Keep
    internal interface ReceivingVideoCallback {
        fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>)

        fun onVODReceived(resource: Resource<List<StreamResponse>>) {}

        fun onVODForFabReceived(resource: Resource<List<StreamResponse>>) {}

        fun onVODReceivedInitial(resource: Resource<List<StreamResponse>>) {}

        fun onNewVideosCount(resource: Resource<Int>) {}
    }
}