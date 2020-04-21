package com.antourage.weaverlib.screens.base

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.MockedNetworkBoundResource
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.google.firebase.firestore.Query
import okhttp3.MultipartBody

internal class Repository {

    companion object {
        var vods: MutableList<StreamResponse>? = null

        fun getLiveVideos(): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = ApiClient.getWebClient().webService.getLiveStreams()
            }.asLiveData()

        fun getMockedLiveVideos(): LiveData<Resource<List<StreamResponse>>> {
            val mockedLiveVideos = List(10) { index ->
                StreamResponse(
                    index,
                    index,
                    "Stream $index",
                    "Stream $index",
                    null,
                    null,
                    "Creator $index",
                    "Creator $index",
                    "https://djyokoo.com/wp-content/uploads/2018/06/blog-placeholder-800x400.jpg",
                    null, null, null, null, null,
                    null, null, index, true, index, true, null
                )
            }
            return object : MockedNetworkBoundResource<List<StreamResponse>>(mockedLiveVideos) {
            }.asLiveData()
        }

        private val adaptiveBitrateUrl =
            "http://antourage.cluster.video/antourage/smil:team1.smil/playlist.m3u8"
        private val bitrate_720p_1800 =
            "http://antourage.cluster.video/antourage/team1_720p/playlist.m3u8"
        private val bitrate_480p_800 =
            "http://antourage.cluster.video/antourage/team1_480p/playlist.m3u8"
        private val bitrate_360p_500 =
            "http://antourage.cluster.video/antourage/team1_360p/playlist.m3u8"

        fun getMockedStreamsForTheTest(): List<StreamResponse> {
            val mockedStreams = mutableListOf<StreamResponse>()
            mockedStreams.add(
                StreamResponse(
                    1001, 1001, "adaptiveBitrateUrl", "adaptiveBitrateUrl",
                    adaptiveBitrateUrl, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, true, 0, false, null
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1002, 1002, "bitrate_720p_1800", "bitrate_720p_1800",
                    bitrate_720p_1800, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, false, 0, false, null
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1003, 1003, "bitrate_480p_800", "bitrate_480p_800",
                    bitrate_480p_800, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, false, 0, false, null
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1004, 1004, "bitrate_360p_500", "bitrate_360p_500",
                    bitrate_360p_500, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, false, 0, false, null
                )
            )
            return mockedStreams
        }

        fun getLiveVideoById(id: Int): LiveData<Resource<StreamResponse>> =
            object : NetworkBoundResource<StreamResponse>() {
                override fun createCall() =
                    ApiClient.getWebClient().webService.getLiveStreamById(id)
            }.asLiveData()

        fun getVODs(count: Int): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = ApiClient.getWebClient().webService.getVODs(count)
            }.asLiveData()

        fun getVODsForFab(): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = ApiClient.getWebClient(v2 = true).webService.getVODsForFab()
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
                override fun createCall() =
                    ApiClient.getWebClient(false).webService.generateUser(body)
            }.asLiveData()

        fun getUser(): LiveData<Resource<User>> =
            object : NetworkBoundResource<User>() {
                override fun createCall() = ApiClient.getWebClient().webService.getUser()
            }.asLiveData()

        fun updateDisplayName(body: UpdateDisplayNameRequest): LiveData<Resource<SimpleResponse>> =
            object : NetworkBoundResource<SimpleResponse>() {
                override fun createCall() =
                    ApiClient.getWebClient().webService.updateDisplayName(body)
            }.asLiveData()

        fun statisticWatchVOD(body: StatisticWatchVideoRequest): LiveData<Resource<SimpleResponse>> =
            object : NetworkBoundResource<SimpleResponse>() {
                override fun createCall() =
                    ApiClient.getWebClient().webService.statisticWatchVOD(body)
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

        internal fun getPollDetails(
            streamId: Int,
            pollId: String
        ): QuerySnapshotValueLiveData<Poll> {
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
            FirestoreDatabase().getAnsweredUsersReference(streamId, pollId).document(user.id)
                .set(user)
        }
        //endregion
    }
}