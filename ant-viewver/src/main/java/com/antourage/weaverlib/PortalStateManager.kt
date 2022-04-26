package com.antourage.weaverlib

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.models.PortalStateResponse
import com.antourage.weaverlib.other.networking.PortalStateRepository
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.SocketConnector
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.networking.localisation.LocalisationRepository
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.TAG
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
internal class PortalStateManager {

    companion object {
        private var callback: PortalStateCallback? = null
        private var fetched = false
        internal const val NEW = "THE_DOOR__TX__NEW"
        internal const val LIVE = "THE_DOOR__TX__LIVE"
        private var coroutineJob: Job? = null
        internal var localisationJsonObject = JSONObject()

        fun setReceivedCallback(callback: PortalStateCallback) {
            Companion.callback = callback
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        fun fetchPortalState(forceFetchAgain: Boolean = false) {
            if (!Global.networkAvailable) return
            if (forceFetchAgain || !fetched) {
                if (forceFetchAgain) SocketConnector.shouldCallApiRequest = false
                fetched = true
                coroutineJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val localisationResponse =
                            LocalisationRepository.getLocalisationJsonFile(AntourageFab.localisation)
                        localisationResponse?.let {
                            val stringResponse = it.string()
                            val jsonResponse = JSONObject(stringResponse)
                            localisationJsonObject = jsonResponse
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        val portalStateResponse =
                            PortalStateRepository.getPortalState(AntourageFab.teamId)
                        portalStateResponse.observeForever(object :
                            Observer<Resource<PortalStateResponse>> {
                            override fun onChanged(resource: Resource<PortalStateResponse>?) {
                                if (resource != null) {
                                    when (resource.status) {
                                        is Status.Failure -> {
                                            Log.d(
                                                TAG,
                                                "Get portal state request failed"
                                            )
                                            portalStateResponse.removeObserver(this)
                                        }
                                        is Status.Success -> {
                                            Log.d(
                                                TAG,
                                                "Successfully received portal state"
                                            )
                                            resource.status.data?.let {
                                                callback?.onPortalStateReceived(
                                                    it
                                                )
                                            }
                                            portalStateResponse.removeObserver(this)
                                        }
                                        else -> {
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }

        fun onPause() {
            coroutineJob?.cancel()
            callback = null
            fetched = false
        }

        fun networkLost() {
            coroutineJob?.cancel()
            fetched = false
        }
    }

    @Keep
    internal interface PortalStateCallback {
        fun onPortalStateReceived(state: PortalStateResponse)
    }
}