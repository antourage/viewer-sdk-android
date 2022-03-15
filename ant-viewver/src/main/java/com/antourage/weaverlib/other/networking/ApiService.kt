package com.antourage.weaverlib.other.networking

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

internal interface ApiService {
    @GET("widget/{teamId}")
    fun getPortalState(@Path ("teamId") teamId : Int): LiveData<ApiResponse<PortalStateResponse>>
}
