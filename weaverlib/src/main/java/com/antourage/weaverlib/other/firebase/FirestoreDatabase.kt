package com.antourage.weaverlib.other.firebase

import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_DEV
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_LOCAL
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_PROD
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_STAGING
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreDatabase{

    val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName))

    private fun getMainDocumentPath(): DocumentReference {
        if (BASE_URL == BASE_URL_LOCAL) {
            return db.collection("antourage").document("local")
        }
        if (BASE_URL == BASE_URL_DEV) {
            return db.collection("antourage").document("dev")
        }
        if (BASE_URL == BASE_URL_STAGING) {
            return db.collection("antourage").document("staging")
        }
        if (BASE_URL == BASE_URL_PROD) {
            return db.collection("antourage").document("prod")
        }
        return db.collection("antourage").document("dev")
    }


    fun getStreamsCollection():CollectionReference{
        return getMainDocumentPath().collection("streams")
    }

    fun getMessagesReferences(streamId:Int):CollectionReference{
        return getMainDocumentPath().collection("streams")
            .document(streamId.toString()).collection("messages")
    }
    fun getPollsReferences(streamId:Int):CollectionReference{
        return getMainDocumentPath().collection("streams")
            .document(streamId.toString()).collection("polls")
    }
    fun getAnsweredUsersReference(streamId: Int, pollId:String):CollectionReference{
        return getPollsReferences(streamId).document(pollId).collection("answeredUsers")
    }
}