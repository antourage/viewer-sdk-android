package com.antourage.weaverlib.other.networking

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.StreamResponse
import retrofit2.http.GET

interface WebService {
    @GET("channels/live")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>
}