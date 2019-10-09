package com.antourage.weaverlib.screens.base

import android.arch.lifecycle.LiveData
import com.antourage.weaverlib.other.firebase.FirestoreDatabase
import com.antourage.weaverlib.other.firebase.QuerySnapshotLiveData
import com.antourage.weaverlib.other.firebase.QuerySnapshotValueLiveData
import com.antourage.weaverlib.other.models.*
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.WebService
import com.google.firebase.firestore.Query
import javax.inject.Inject

class Repository @Inject constructor(val api: WebService) {

    companion object {
        var vods: List<StreamResponse>? = null
    }

    fun getLiveVideos(): LiveData<Resource<List<StreamResponse>>> =
        object : NetworkBoundResource<List<StreamResponse>>() {
            override fun createCall() = api.getLiveStreams()
        }.asLiveData()


    fun getVODs(): LiveData<Resource<List<StreamResponse>>> =
        object : NetworkBoundResource<List<StreamResponse>>() {
            override fun createCall() = api.getVODs()
        }.asLiveData()

    fun generateUser(body: UserRequest): LiveData<Resource<User>> =
        object : NetworkBoundResource<User>() {
            override fun createCall() = api.generateUser(body)
        }.asLiveData()

    fun getUser(id: Int, apiKey: String): LiveData<Resource<User>> =
        object : NetworkBoundResource<User>() {
            override fun createCall() = api.getUser(id, apiKey)
        }.asLiveData()

    fun updateDisplayName(body: UpdateDisplayNameRequest): LiveData<Resource<SimpleResponse>> =
        object : NetworkBoundResource<SimpleResponse>() {
            override fun createCall() = api.updateDisplayName(body)
        }.asLiveData()

    //region Firebase
    fun addMessage(message: Message, streamId: Int) {
        FirestoreDatabase().getMessagesReferences(streamId).document().set(message)
    }

    fun getMessages(streamId: Int): QuerySnapshotLiveData<Message> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getMessagesReferences(streamId).orderBy(
                "timestamp",
                Query.Direction.ASCENDING
            ), Message::class.java
        )
    }

    fun getStream(streamId: Int): QuerySnapshotValueLiveData<Stream> {
        val docRef = FirestoreDatabase().getStreamsCollection().document(streamId.toString())
        return QuerySnapshotValueLiveData(docRef, Stream::class.java)
    }

    fun getPoll(streamId: Int): QuerySnapshotLiveData<Poll> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getPollsReferences(streamId).whereEqualTo("isActive", true),
            Poll::class.java
        )
    }

    fun getPollDetails(streamId: Int, pollId: String): QuerySnapshotValueLiveData<Poll> {
        return QuerySnapshotValueLiveData(
            FirestoreDatabase().getPollsReferences(streamId).document(
                pollId
            ), Poll::class.java
        )
    }

    fun getAnsweredUsers(streamId: Int, pollId: String): QuerySnapshotLiveData<AnsweredUser> {
        return QuerySnapshotLiveData(
            FirestoreDatabase().getAnsweredUsersReference(
                streamId,
                pollId
            ), AnsweredUser::class.java
        )
    }

    fun vote(streamId: Int, pollId: String, user: AnsweredUser) {
        FirestoreDatabase().getAnsweredUsersReference(streamId, pollId).document(user.id).set(user)
    }
    //endregion
}