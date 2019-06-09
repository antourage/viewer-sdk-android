package com.antourage.weaverlib.other.firebase

import android.arch.lifecycle.LiveData
import android.util.Log
import com.antourage.weaverlib.other.models.FirestoreModel
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.google.firebase.firestore.*


class QuerySnapshotLiveData<T:FirestoreModel>(private val query: Query, val typeParameterClass: Class<T>? = null) :
    LiveData<Resource<List<T>>>(),
    EventListener<QuerySnapshot> {


    private var registration: ListenerRegistration? = null

    override fun onEvent(snapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
        value = if (e != null) {
            Resource(State.FAILURE, null, e.localizedMessage)
        } else {
            val data:MutableList<T> = mutableListOf()
            for (i in 0 until snapshots!!.documents.size) {
                if(snapshots.documents[i].toObject(typeParameterClass!!)!=null) {
                    data.add(snapshots.documents[i].toObject(typeParameterClass)!!)
                    data[i].id = snapshots.documents[i].id
                }
            }
            Resource(State.SUCCESS, data.toList())
        }
    }

    override fun onActive() {
        super.onActive()
        registration = query.addSnapshotListener(this)
    }

    override fun onInactive() {
        super.onInactive()

        registration?.also {
            it.remove()
            registration = null
        }
    }
}

class QuerySnapshotValueLiveData<T>(private val query: DocumentReference, val typeParameterClass: Class<T>? = null) :
    LiveData<Resource<T>>(),
    EventListener<DocumentSnapshot> {


    private var registration: ListenerRegistration? = null

    override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
        value = if (e != null) {
            Resource(State.FAILURE, null, e.localizedMessage)
        } else {
            val data:T? = snapshot?.toObject(typeParameterClass!!)
            Resource(State.SUCCESS, data)
        }
    }

    override fun onActive() {
        super.onActive()
        registration = query.addSnapshotListener(this)
    }

    override fun onInactive() {
        super.onInactive()

        registration?.also {
            it.remove()
            registration = null
        }
    }
}