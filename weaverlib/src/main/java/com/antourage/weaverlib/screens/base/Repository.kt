package com.antourage.weaverlib.screens.base

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.ApiResponse
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.models.StreamResponse

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