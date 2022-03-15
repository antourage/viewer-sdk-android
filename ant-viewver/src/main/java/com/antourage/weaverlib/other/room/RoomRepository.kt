package com.antourage.weaverlib.other.room

import android.content.Context
import com.antourage.weaverlib.other.SingletonHolder
import com.antourage.weaverlib.other.models.Video

internal class RoomRepository private constructor(context: Context) {

    private var videoDao: ShownStreamsDao = AppDatabase.getInstance(context).shownStreamsDao()

    companion object : SingletonHolder<RoomRepository, Context>(::RoomRepository)

    suspend fun addToSeen(video: Video) {
        videoDao.addToSeen(video)
    }

    suspend fun isAlreadySeen(id: Int): Boolean = videoDao.isSeen(id)
}