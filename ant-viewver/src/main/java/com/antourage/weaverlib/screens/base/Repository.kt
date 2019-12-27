package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.google.firebase.firestore.Query
import okhttp3.MultipartBody

internal class Repository {

    companion object {
        internal var vods: List<StreamResponse>? = null

        internal fun generateUser(body: UserRequest): LiveData<Resource<User>> =
            object : NetworkBoundResource<User>() {
                override fun createCall() = ApiClient.getWebClient(false).webService.generateUser(body)
            }.asLiveData()
    }

    fun getLiveVideos(): LiveData<Resource<List<StreamResponse>>> =
        object : NetworkBoundResource<List<StreamResponse>>() {
            override fun createCall() = ApiClient.getWebClient().webService.getLiveStreams()
        }.asLiveData()

    fun getLiveVideoById(id: Int): LiveData<Resource<StreamResponse>> =
        object : NetworkBoundResource<StreamResponse>() {
            override fun createCall() = ApiClient.getWebClient().webService.getLiveStreamById(id)
        }.asLiveData()

    fun getVODs(count: Int): LiveData<Resource<List<StreamResponse>>> =
        object : NetworkBoundResource<List<StreamResponse>>() {
            override fun createCall() = ApiClient.getWebClient().webService.getVODs(count)
        }.asLiveData()

    fun getVODById(id: Int): LiveData<Resource<StreamResponse>> =
        object : NetworkBoundResource<StreamResponse>() {
            override fun createCall() = ApiClient.getWebClient().webService.getVODById(id)
        }.asLiveData()

    fun getNewVODsCount(): LiveData<Resource<Int>> =
        object : NetworkBoundResource<Int>() {
            override fun createCall() = ApiClient.getWebClient().webService.getNewVODsCount()
        }.asLiveData()

    fun generateUser(body: UserRequest): LiveData<Resource<User>> =
        object : NetworkBoundResource<User>() {
            override fun createCall() = ApiClient.getWebClient(false).webService.generateUser(body)
        }.asLiveData()

    fun getUser(id: Int, apiKey: String): LiveData<Resource<User>> =
        object : NetworkBoundResource<User>() {
            override fun createCall() = ApiClient.getWebClient().webService.getUser(id, apiKey)
        }.asLiveData()

    fun updateDisplayName(body: UpdateDisplayNameRequest): LiveData<Resource<SimpleResponse>> =
        object : NetworkBoundResource<SimpleResponse>() {
            override fun createCall() = ApiClient.getWebClient().webService.updateDisplayName(body)
        }.asLiveData()

    fun statisticWatchVOD(body: StatisticWatchVideoRequest): LiveData<Resource<SimpleResponse>> =
        object : NetworkBoundResource<SimpleResponse>() {
            override fun createCall() = ApiClient.getWebClient().webService.statisticWatchVOD(body)
        }.asLiveData()

    fun statisticWatchLiveStream(body: StatisticWatchVideoRequest): LiveData<Resource<SimpleResponse>> =
        object : NetworkBoundResource<SimpleResponse>() {
            override fun createCall() =
                ApiClient.getWebClient().webService.statisticWatchLiveStream(body)
        }.asLiveData()

    fun stopWatchingVOD(body: StopWatchVodRequest): LiveData<Resource<SimpleResponse>> =
        object : NetworkBoundResource<SimpleResponse>() {
            override fun createCall() =
                ApiClient.getWebClient().webService.stopWatchingVOD(body)
        }.asLiveData()

    fun uploadImage(image: MultipartBody.Part): LiveData<Resource<UpdateImageResponse>> =
        object : NetworkBoundResource<UpdateImageResponse>() {
            override fun createCall() =
                ApiClient.getWebClient().webService.uploadImage(image)
        }.asLiveData()

    fun subscribeToPushNotifications(body: SubscribeToPushesRequest): LiveData<Resource<NotificationSubscriptionResponse>> =
        object : NetworkBoundResource<NotificationSubscriptionResponse>() {
            override fun createCall() =
                ApiClient.getWebClient().webService.subscribeToPushNotifications(body)
        }.asLiveData()

    //region Firebase
    internal fun addMessage(message: Message, streamId: Int) {
        FirestoreDatabase().getMessagesReferences(streamId).document().set(message)
    }

    internal fun getMessages(streamId: Int): QuerySnapshotLiveData<Message> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getMessagesReferences(streamId).orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            ), Message::class.java
        )
    }

    internal fun getStream(streamId: Int): QuerySnapshotValueLiveData<Stream> {
        val docRef = FirestoreDatabase().getStreamsCollection().document(streamId.toString())
        return QuerySnapshotValueLiveData(docRef, Stream::class.java)
    }

    internal fun getPoll(streamId: Int): QuerySnapshotLiveData<Poll> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getPollsReferences(streamId).whereEqualTo("isActive", true),
            Poll::class.java
        )
    }

    internal fun getPollDetails(streamId: Int, pollId: String): QuerySnapshotValueLiveData<Poll> {
        return QuerySnapshotValueLiveData(
            FirestoreDatabase().getPollsReferences(streamId).document(
                pollId
            ), Poll::class.java
        )
    }

    internal fun getAnsweredUsers(
        streamId: Int,
        pollId: String
    ): QuerySnapshotLiveData<AnsweredUser> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getAnsweredUsersReference(
                streamId,
                pollId
            ), AnsweredUser::class.java
        )
    }

    internal fun vote(streamId: Int, pollId: String, user: AnsweredUser) {
        FirestoreDatabase().getAnsweredUsersReference(streamId, pollId).document(user.id).set(user)
    }
    //endregion
}