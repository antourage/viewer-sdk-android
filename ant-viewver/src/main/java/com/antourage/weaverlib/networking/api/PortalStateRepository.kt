package com.antourage.weaverlib.networking.api

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.networking.NetworkBoundResource
import com.antourage.weaverlib.networking.Resource
import com.antourage.weaverlib.other.models.PortalStateResponse

internal class PortalStateRepository {
    companion object {
        fun getPortalState(teamId: Int): LiveData<Resource<PortalStateResponse>> =
            object : NetworkBoundResource<PortalStateResponse>() {
                override fun createCall() = ApiClient.getWebClient().apiService.getPortalState(teamId)
            }.asLiveData()
    }
}