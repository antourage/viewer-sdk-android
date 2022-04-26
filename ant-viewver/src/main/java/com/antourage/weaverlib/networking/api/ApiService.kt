package com.antourage.weaverlib.networking.api

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.networking.ApiResponse
import com.antourage.weaverlib.other.models.*
import retrofit2.http.*

internal interface ApiService {
    @GET("widget/{teamId}")
    fun getPortalState(@Path ("teamId") teamId : Int): LiveData<ApiResponse<PortalStateResponse>>
}
