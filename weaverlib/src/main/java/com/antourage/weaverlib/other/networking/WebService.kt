package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.models.StreamResponse
import retrofit2.http.GET

interface WebService {
    @GET("streams")
    //TODO 06/08/2019 on old backend was this way
    //TODO new back end set up to dev
    //GET("channels/live")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("VODs")
    fun getVODs(): LiveData<ApiResponse<List<StreamResponse>>>
}