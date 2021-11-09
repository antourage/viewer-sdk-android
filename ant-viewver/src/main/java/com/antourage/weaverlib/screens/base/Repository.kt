package com.antourage.weaverlib.screens.base

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import okhttp3.MultipartBody

internal class Repository {

    companion object {
        fun subscribeToPushNotifications(body: SubscribeToPushesRequest): LiveData<Resource<NotificationSubscriptionResponse>> =
            object : NetworkBoundResource<NotificationSubscriptionResponse>() {
                override fun createCall() =
                    ApiClient.getWebClient().webService.subscribeToPushNotifications(body)
            }.asLiveData()
    }
}