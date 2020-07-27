package com.antourage.weaverlib.other.firebase

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.FirestoreModel
import com.antourage.weaverlib.other.networking.Resource
import com.google.firebase.firestore.*

/**
 * This classes create abstraction over Firebase requests
 * Allow handling of errors in consistent way(not in current scope)
 *
 */
internal class QuerySnapshotLiveData<T : FirestoreModel>(
    private val query: Query,
    private val typeParameterClass: Class<T>? = null
) :
    LiveData<Resource<List<T>>>(),
    EventListener<QuerySnapshot> {

    private var registration: ListenerRegistration? = null

    override fun onEvent(snapshots: QuerySnapshot?, e: FirebaseFirestoreException?) {
        value = if (e != null) {
            Resource.failure(e.localizedMessage ?: "")
        } else {
            val data: MutableList<T> = mutableListOf()
            for (i in 0 until (snapshots?.documents?.size ?: 0)) {
                val document = snapshots?.documents?.get(i)
                document?.apply {
                    if (typeParameterClass != null){
                        this.toObject(typeParameterClass)?.let {
                            data.add(it)
                        }
                        data[i].id = id
                    }
                }
            }
            Resource.success(data.toList())
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

internal class QuerySnapshotValueLiveData<T>(
    private val query: DocumentReference,
    private val typeParameterClass: Class<T>? = null
) :
    LiveData<Resource<T>>(),
    EventListener<DocumentSnapshot> {

    private var registration: ListenerRegistration? = null

    override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
        value = if (e != null) {
            Resource.failure(e.localizedMessage ?: "")
        } else {
            val data: T? = typeParameterClass?.let { snapshot?.toObject(it) }
            Resource.success(data)
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