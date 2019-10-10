package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.models.*
import retrofit2.http.*

interface WebService {
    @GET("live")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("vod")
    fun getVODs(@Query("count") count: Int): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("vod/new")
    fun getNewVODsCount(): LiveData<ApiResponse<Int>>

    @POST("users/generate")
    fun generateUser(@Body body: UserRequest): LiveData<ApiResponse<User>>

    @GET("users/{id}")
    fun getUser(@Path("id") id: Int, @Query("apiKey") apiKey: String): LiveData<ApiResponse<User>>

    @PUT("users")
    fun updateDisplayName(@Body body: UpdateDisplayNameRequest): LiveData<ApiResponse<SimpleResponse>>

    @POST("statistic/livestream")
    fun statisticWatchLiveStream(@Body body: StatisticWatchVideoRequest): LiveData<ApiResponse<SimpleResponse>>

    @POST("statistic/vod")
    fun statisticWatchVOD(@Body body: StatisticWatchVideoRequest): LiveData<ApiResponse<SimpleResponse>>
}