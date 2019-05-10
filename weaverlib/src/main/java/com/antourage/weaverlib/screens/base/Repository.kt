package com.antourage.weaverlib.screens.base

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.base.ApiResponse
import com.antourage.weaverlib.other.networking.base.NetworkBoundResource
import com.antourage.weaverlib.other.networking.base.Resource

class Repository{
    fun getListOfStreams(): LiveData<Resource<List<StreamResponse>>> {
        return object : NetworkBoundResource<List<StreamResponse>>() {
            override fun saveCallResult(item: List<StreamResponse>) {
            }

            override fun createCall(): LiveData<ApiResponse<List<StreamResponse>>> {
                return ApiClient.getInitialClient().webService.getLiveStreams()
            }
        }.asLiveData()
    }
}