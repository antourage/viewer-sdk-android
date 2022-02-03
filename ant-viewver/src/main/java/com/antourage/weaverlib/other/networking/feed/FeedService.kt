package com.antourage.weaverlib.other.networking.feed

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.Viewers
import com.antourage.weaverlib.other.networking.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

internal interface FeedService {
    @GET("livestreams")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("feed")
    fun getVODs(@Query("count") count: Int): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("feed/new")
    fun getVODsForFab(@Query("lastViewDate") lastViewDate: String): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("feed/new")
    fun getVODsForFab(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("livestreams/{id}/viewers")
    fun getLiveViewers(@Path("id") id: Int): LiveData<ApiResponse<Viewers>>
}