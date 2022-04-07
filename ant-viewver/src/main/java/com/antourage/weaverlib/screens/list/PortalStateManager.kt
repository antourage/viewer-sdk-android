package com.antourage.weaverlib.screens.list

import android.util.Log
import androidx.annotation.Keep
import androidx.lifecycle.Observer
import com.antourage.weaverlib.Global
import com.antourage.weaverlib.other.models.PortalStateResponse
import com.antourage.weaverlib.other.networking.PortalStateRepository
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.SocketConnector
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.AntourageFab.Companion.TAG

/**
 * Added so that AntourageFab and VideoListFragment received data from the same source
 */
@Keep
internal class PortalStateManager {

    companion object {
        private var callback: PortalStateCallback? = null
        private var fetched = false

        fun setReceivedCallback(callback: PortalStateCallback) {
            PortalStateManager.callback = callback
        }

        fun fetchPortalState(forceFetchAgain: Boolean = false) {
            if(!Global.networkAvailable) return
            if (forceFetchAgain || !fetched) {
                if(forceFetchAgain) SocketConnector.shouldCallApiRequest = false
                fetched = true
                val portalStateResponse = PortalStateRepository.getPortalState(AntourageFab.teamId)
                portalStateResponse.observeForever(object : Observer<Resource<PortalStateResponse>>{
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
                                    resource.status.data?.let { callback?.onPortalStateReceived(it) }
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

        fun onPause() {
            callback = null
            fetched = false
        }

        fun networkLost() {
            fetched = false
        }
    }

    @Keep
    internal interface PortalStateCallback {
        fun onPortalStateReceived(state: PortalStateResponse)
    }
}