package com.antourage.weaverlib.other.firebase

import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_ANSWERED_USERS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_MESSAGES
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_POLLS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.COLLECTION_PATH_STREAMS
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.DOCUMENT_PATH_DEV
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.DOCUMENT_PATH_PROD
import com.antourage.weaverlib.other.firebase.FirebaseConfig.Companion.DOCUMENT_PATH_STAGING
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_DEV
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_PROD
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_STAGING
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

internal class FirestoreDatabase {

    private val db =
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName))

    private fun getMainDocumentPath(): DocumentReference {
        return when (BASE_URL) {
            BASE_URL_DEV -> db.collection(COLLECTION_PATH).document(DOCUMENT_PATH_DEV)
            BASE_URL_STAGING -> db.collection(COLLECTION_PATH).document(DOCUMENT_PATH_STAGING)
            BASE_URL_PROD -> db.collection(COLLECTION_PATH).document(DOCUMENT_PATH_PROD)
            else -> db.collection(COLLECTION_PATH).document(DOCUMENT_PATH_PROD)
        }
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