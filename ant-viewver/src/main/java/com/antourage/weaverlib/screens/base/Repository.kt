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

        internal fun fetchLastMessage(vodID: Int, startTime: Long, onFinish: (video: Video) -> Unit){
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

        private fun getLastMessage(
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