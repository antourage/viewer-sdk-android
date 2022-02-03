package com.antourage.weaverlib.other.firebase

import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_ANSWERED_USERS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_MESSAGES
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_POLLS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_STREAMS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.DOCUMENT_PATH_PROD
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

internal class FirestoreDatabase {

    private val db =
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName))

    private fun getMainDocumentPath(): DocumentReference {
        return db.collection(COLLECTION_PATH).document(UserCache.getInstance()?.getEnvChoice()?: DOCUMENT_PATH_PROD)
    }

    fun getStreamsCollection(): CollectionReference {
        return getMainDocumentPath().collection(COLLECTION_PATH_STREAMS)
    }

    fun getMessagesReferences(streamId: Int): CollectionReference {
        return getMainDocumentPath().collection(COLLECTION_PATH_STREAMS)
            .document(streamId.toString()).collection(COLLECTION_PATH_MESSAGES)
    }

    fun getPollsReferences(streamId: Int): CollectionReference {
        return getMainDocumentPath().collection(COLLECTION_PATH_STREAMS)
            .document(streamId.toString()).collection(COLLECTION_PATH_POLLS)
    }

    fun getAnsweredUsersReference(streamId: Int, pollId: String): CollectionReference {
        return getPollsReferences(streamId).document(pollId)
            .collection(COLLECTION_PATH_ANSWERED_USERS)
    }
}