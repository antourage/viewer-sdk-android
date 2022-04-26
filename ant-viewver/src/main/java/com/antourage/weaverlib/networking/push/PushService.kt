package com.antourage.weaverlib.networking.push

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.NotificationSubscriptionResponse
import com.antourage.weaverlib.other.models.SubscribeToPushesRequest
import com.antourage.weaverlib.networking.ApiResponse
import retrofit2.http.*

internal interface PushService {
    @POST("notifications")
    fun subscribeToPushNotifications(@Body body: SubscribeToPushesRequest): LiveData<ApiResponse<NotificationSubscriptionResponse>>

    @HTTP(method = "DELETE", path = "notifications",  hasBody = true)
    fun unsubscribeFromPushNotifications(@Body body: SubscribeToPushesRequest): LiveData<ApiResponse<NotificationSubscriptionResponse>>

}