package com.antourage.weaverlib.other.firebase

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreDatabase{
    val db = FirebaseFirestore.getInstance()

    fun getStreamsCollection():CollectionReference{
        return db.collection("antourage").document("dev").collection("streams")
    }

    fun getMessagesReferences(streamId:Int):CollectionReference{
        return db.collection("antourage").document("dev").collection("streams")
            .document(streamId.toString()).collection("messages")
    }
    fun getPollsReferences(streamId:Int):CollectionReference{
        return db.collection("antourage").document("dev").collection("streams")
            .document(streamId.toString()).collection("polls")
    }
}