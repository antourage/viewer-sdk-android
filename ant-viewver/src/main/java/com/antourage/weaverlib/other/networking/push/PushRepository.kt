package com.antourage.weaverlib.other.networking.push

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.NotificationSubscriptionResponse
import com.antourage.weaverlib.other.models.SubscribeToPushesRequest
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource

internal class PushRepository {
    companion object {
        fun subscribeToPushNotifications(body: SubscribeToPushesRequest): LiveData<Resource<NotificationSubscriptionResponse>> =
            object : NetworkBoundResource<NotificationSubscriptionResponse>() {
                override fun createCall() =
                    PushClient.getWebClient().pushService.subscribeToPushNotifications(body)
            }.asLiveData()

        fun unsubscribeFromPushNotifications(body: SubscribeToPushesRequest): LiveData<Resource<NotificationSubscriptionResponse>> =
            object : NetworkBoundResource<NotificationSubscriptionResponse>() {
                override fun createCall() =
                    PushClient.getWebClient().pushService.unsubscribeFromPushNotifications(body)
            }.asLiveData()
    }
}