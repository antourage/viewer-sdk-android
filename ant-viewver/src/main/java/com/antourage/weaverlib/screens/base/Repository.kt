package com.antourage.weaverlib.screens.base

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.convertUtcToLocal
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.*
import com.antourage.weaverlib.other.room.RoomRepository
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.jetbrains.anko.collections.forEachWithIndex

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
                    "Stream $index",
                    "Stream $index",
                    null,
                    null,
                    "Creator $index",
                    "Creator $index",
                    "https://djyokoo.com/wp-content/uploads/2018/06/blog-placeholder-800x400.jpg",
                    null, null, null, null, null,
                    null, null, index.toLong(), null,true, index.toLong(), true
                )
            }
            return object : MockedNetworkBoundResource<List<StreamResponse>>(mockedLiveVideos) {
            }.asLiveData()
        }

        private const val adaptiveBitrateUrl =
            "http://antourage.cluster.video/antourage/smil:team1.smil/playlist.m3u8"
        private const val bitrate_720p_1800 =
            "http://antourage.cluster.video/antourage/team1_720p/playlist.m3u8"
        private const val bitrate_480p_800 =
            "http://antourage.cluster.video/antourage/team1_480p/playlist.m3u8"
        private const val bitrate_360p_500 =
            "http://antourage.cluster.video/antourage/team1_360p/playlist.m3u8"

        fun getMockedStreamsForTheTest(): List<StreamResponse> {
            val mockedStreams = mutableListOf<StreamResponse>()
            mockedStreams.add(
                StreamResponse(
                    1001, "adaptiveBitrateUrl", "adaptiveBitrateUrl",
                    adaptiveBitrateUrl, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, null,true, 0, false
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1002, "bitrate_720p_1800", "bitrate_720p_1800",
                    bitrate_720p_1800, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, null,false, 0, false
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1003, "bitrate_480p_800", "bitrate_480p_800",
                    bitrate_480p_800, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, null,false, 0, false
                )
            )
            mockedStreams.add(
                StreamResponse(
                    1004, "bitrate_360p_500", "bitrate_360p_500",
                    bitrate_360p_500, null, null, null, null,
                    null, null, 1, null, null, null,
                    null, 0, null,false, 0, false
                )
            )
            return mockedStreams
        }

        fun getLiveViewers(id: Int): LiveData<Resource<Viewers>> =
            object : NetworkBoundResource<Viewers>() {
                override fun createCall() = ApiClient.getWebClient().webService.getLiveViewers(id)
            }.asLiveData()

        fun postVideoOpened(body: VideoOpenedRequest): LiveData<Resource<SimpleResponse>> =
            object : NetworkBoundResource<SimpleResponse>() {
                override fun createCall() = ApiClient.getWebClient().webService.postVODOpen(body)
            }.asLiveData()

        fun postVideoClosed(body: VideoClosedRequest): LiveData<Resource<SimpleResponse>> =
            object : NetworkBoundResource<SimpleResponse>() {
                override fun createCall() = ApiClient.getWebClient().webService.postVODClose(body)
            }.asLiveData()

        fun postLiveOpened(body: LiveOpenedRequest): LiveData<Resource<AdBanner>> =
            object : NetworkBoundResource<AdBanner>() {
                override fun createCall() = ApiClient.getWebClient().webService.postLiveOpen(body)
            }.asLiveData()

        fun postLiveClosed(body: LiveClosedRequest): LiveData<Resource<SimpleResponse>> =
            object : NetworkBoundResource<SimpleResponse>() {
                override fun createCall() = ApiClient.getWebClient().webService.postLiveClose(body)
            }.asLiveData()

        private fun getVODs(count: Int): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = ApiClient.getWebClient().webService.getVODs(count)
            }.asLiveData()

        fun getVODsWithLastCommentAndStopTime(
            count: Int,
            repository: RoomRepository
        ): LiveData<Resource<List<StreamResponse>>> {
            val response = getVODs(count)
            val streamResponseLD = MutableLiveData<Resource<List<StreamResponse>>>()

            response.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>) {
                    when (resource.status) {
                        is Status.Failure -> {
                            streamResponseLD.value = resource
                            response.removeObserver(this)
                        }
                        is Status.Success -> {
                            val videoList = resource.status.data
                            if (videoList != null) {
                                if(videoList.isEmpty()){
                                    streamResponseLD.postValue(Resource(Status.Success(videoList)))
                                }else{
                                    updateListWithLastComments(videoList, repository) {
                                        streamResponseLD.postValue(Resource(Status.Success(it)))
                                    }
                                }
                            } else {
                                streamResponseLD.value = resource
                            }
                            response.removeObserver(this)
                        }
                        is Status.Loading -> {
                            streamResponseLD.value = resource
                        }
                        else -> {}
                    }
                }
            })
            return streamResponseLD
        }

        private fun updateListWithLastComments(
            videoList: List<StreamResponse>,
            repository: RoomRepository,
            onFinish: (videoListNew: List<StreamResponse>) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val arrayOfUpdatedIds = ArrayList<Int>()
                videoList.forEachWithIndex { index, vod ->
                    vod.id?.let { vodId ->
                        val video = repository.getVideoById(vodId)
                        if (video?.nickname != null) { //due to no data for new vod
                            vod.lastMessage = video.text
                            vod.lastMessageAuthor = video.nickname
                            vod.stopTimeMillis = video.stopTimeMillis
                            arrayOfUpdatedIds.add(vodId)
                            if (index == videoList.size - 1) {
                                if (arrayOfUpdatedIds.size == videoList.size) onFinish(videoList)
                            }
                        } else {
                            fetchLastMessage(vodId, vod.startTime?.let { startTime ->
                                convertUtcToLocal(startTime)?.time } ?: 0L) {
                                vod.lastMessage = it.text
                                vod.lastMessageAuthor = it.nickname
                                vod.stopTimeMillis = it.stopTimeMillis
                                arrayOfUpdatedIds.add(vodId)
                                if (arrayOfUpdatedIds.size == videoList.size) onFinish(videoList)
                                GlobalScope.launch(Dispatchers.Unconfined) { repository.addVideo(it) }
                            }
                        }
                    }
                }
            }
        }

        private fun fetchLastMessage(vodID: Int, startTime: Long, onFinish: (video: Video) -> Unit){
            val video = Video(vodID, 0L, startTime, "", "")

            val success = OnSuccessListener<QuerySnapshot> { data ->
                val messageList = data?.toObjects(Message::class.java)
                var message: Message? = null
                if (!messageList.isNullOrEmpty()) {
                    val userMessages = messageList.filter { it.type == 1 }
                    if (userMessages.isNotEmpty()) {
                        message = userMessages[0]
                    }
                }

                video.text = message?.text ?: ""
                if(message?.userID ==  UserCache.getInstance()?.getUserId().toString()){
                    video.nickname = UserCache.getInstance()?.getUserNickName() ?: message.nickname
                            ?: ""
                }else{
                    video.nickname = message?.nickname ?: ""
                }
                onFinish(video)
            }

            val failure = OnFailureListener { onFinish(video) }
            getLastMessage(vodID, success, failure)
        }

        fun getVODsForFab(): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() =
                    ApiClient.getWebClient().webService.getVODsForFab()
            }.asLiveData()

        fun getNewVODsCount(): LiveData<Resource<Int>> =
            object : NetworkBoundResource<Int>() {
                override fun createCall() = ApiClient.getWebClient().webService.getNewVODsCount()
            }.asLiveData()


        fun getUser(): LiveData<Resource<User>> =
            object : NetworkBoundResource<User>() {
                override fun createCall() = ApiClient.getWebClient().webService.getUser()
            }.asLiveData()

        fun getFeedInfo(): LiveData<Resource<FeedInfo>> =
            object : NetworkBoundResource<FeedInfo>() {
                override fun createCall() = ApiClient.getWebClient().webService.getFeedInfo()
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

        internal fun getMessagesVOD(
            streamId: Int,
            successListener: OnSuccessListener<QuerySnapshot>
        ) {
            FirestoreDatabase().getMessagesReferences(streamId).orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            ).get(Source.SERVER).addOnSuccessListener(successListener)
        }

        internal fun getLastMessage(
            streamId: Int,
            successListener: OnSuccessListener<QuerySnapshot>,
            failureListener: OnFailureListener
        ) {
            FirestoreDatabase().getMessagesReferences(streamId).whereEqualTo("type", 1).limit(1)
                .orderBy(
                    "timestamp",
                    Query.Direction.DESCENDING
                ).get().addOnSuccessListener(successListener).addOnFailureListener(failureListener)

        }

        internal fun getChatPollInfoFromLiveStream(
            streamId: Int,
            callback: LiveChatPollInfoCallback
        ) {
            var chatEnabled: Boolean? = null
            var pollEnabled: Boolean? = null
            var message: Message? = null

            FirestoreDatabase().getStreamsCollection().document(streamId.toString()).get()
                .addOnSuccessListener { documentSnapshot ->
                    documentSnapshot.toObject(Stream::class.java).let {
                        chatEnabled = if(it?.isChatActive!=null){
                            it.isChatActive
                        }else false
                        if (pollEnabled != null && message != null) {
                            callback.onSuccess(chatEnabled!!, pollEnabled!!, message!!)
                        }
                    }
                }.addOnFailureListener { callback.onFailure() }

            FirestoreDatabase().getMessagesReferences(streamId).whereEqualTo("type", 1).limit(1)
                .orderBy(
                    "timestamp",
                    Query.Direction.DESCENDING
                ).get().addOnSuccessListener { snapshot ->
                    val messageList = snapshot?.toObjects(Message::class.java)
                    message = if (!messageList.isNullOrEmpty()) {
                        val userMessages = messageList.filter { it.type == 1 }
                        if (userMessages.isNotEmpty()) {
                            userMessages[0]
                        } else {
                            Message()
                        }
                    } else {
                        Message()
                    }

                    if (chatEnabled != null && pollEnabled != null) {
                        callback.onSuccess(chatEnabled!!, pollEnabled!!, message!!)
                    }

                }.addOnFailureListener {
                    callback.onFailure()
                }

            FirestoreDatabase().getPollsReferences(streamId).whereEqualTo("isActive", true).get()
                .addOnSuccessListener { documentSnapshot ->
                    documentSnapshot.toObjects(Poll::class.java).let {
                        pollEnabled = !it.isNullOrEmpty()
                        if (chatEnabled != null && message != null) {
                            callback.onSuccess(chatEnabled!!, pollEnabled!!, message!!)
                        }
                    }
                }.addOnFailureListener {
                    callback.onFailure()
                }
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
            FirestoreDatabase().getAnsweredUsersReference(streamId, pollId).document(user.id!!)
                .set(user)
        }
        //endregion
    }

    interface LiveChatPollInfoCallback {
        fun onSuccess(chatEnabled: Boolean, pollEnabled: Boolean, message: Message)
        fun onFailure()
    }

}