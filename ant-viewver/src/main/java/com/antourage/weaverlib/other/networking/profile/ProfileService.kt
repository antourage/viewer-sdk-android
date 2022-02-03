package com.antourage.weaverlib.other.networking.profile

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.ProfileResponse
import com.antourage.weaverlib.other.networking.ApiResponse
import retrofit2.http.GET

internal interface ProfileService {
    @GET("profiles")
    fun getProfile(): LiveData<ApiResponse<ProfileResponse>>
}