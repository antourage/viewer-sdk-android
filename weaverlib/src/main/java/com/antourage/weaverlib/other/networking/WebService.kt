package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.models.*
import retrofit2.http.*

interface WebService {
    @GET("streams")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("VODs")
    fun getVODs(): LiveData<ApiResponse<List<StreamResponse>>>

    @POST("users/generate")
    fun generateUser(@Body body: UserRequest): LiveData<ApiResponse<User>>

    @GET("users/{id}")
    fun getUser(@Path("id") id: Int, @Query("apiKey") apiKey: String): LiveData<ApiResponse<User>>

    @POST("users/name")
    fun updateDisplayName(@Body body: UpdateDisplayNameRequest): LiveData<ApiResponse<SimpleResponse>>
}