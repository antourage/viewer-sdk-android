package com.antourage.weaverlib.other.networking

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.*
import okhttp3.MultipartBody
import retrofit2.http.*

internal interface WebService {
    @GET("live")
    fun getLiveStreams(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("live/{id}/viewers")
    fun getLiveViewers(@Path("id") id: Int): LiveData<ApiResponse<Viewers>>

    @GET("vod")
    fun getVODs(@Query("count") count: Int): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("vod/new")
    fun getVODsForFab(): LiveData<ApiResponse<List<StreamResponse>>>

    @GET("vod/new")
    fun getNewVODsCount(): LiveData<ApiResponse<Int>>

    @POST("users")
    fun generateUser(@Body body: UserRequest): LiveData<ApiResponse<User>>

    @GET("users")
    fun getUser(): LiveData<ApiResponse<User>>

    @PUT("users")
    fun updateDisplayName(@Body body: UpdateDisplayNameRequest): LiveData<ApiResponse<SimpleResponse>>

    @POST("statistic/livestream")
    fun statisticWatchLiveStream(@Body body: StatisticWatchVideoRequest): LiveData<ApiResponse<SimpleResponse>>

    @POST("statistic/vod")
    fun statisticWatchVOD(@Body body: StatisticWatchVideoRequest): LiveData<ApiResponse<SimpleResponse>>

    @Multipart
    @POST("users/uploadimage")
    fun uploadImage(@Part file: MultipartBody.Part): LiveData<ApiResponse<UpdateImageResponse>>

    @POST("notifications")
    fun subscribeToPushNotifications(@Body body: SubscribeToPushesRequest): LiveData<ApiResponse<NotificationSubscriptionResponse>>
}