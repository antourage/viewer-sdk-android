package com.antourage.weaverlib.other.room

import android.content.Context
import com.antourage.weaverlib.other.SingletonHolder
import com.antourage.weaverlib.other.models.VideoStopTime
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

internal class RoomRepository private constructor(context: Context) {

    private val listOfExistingId = ArrayList<Int>()
    private var dao: VideoStopTimeDao = AppDatabase.getInstance(context).videoStopTimeDao()

    companion object : SingletonHolder<RoomRepository, Context>(::RoomRepository)

    init {
        deleteAllExpired()
    }

    fun addStopTime(videoStopTime: VideoStopTime) {
        GlobalScope.launch(Dispatchers.IO) {
            if (listOfExistingId.contains(videoStopTime.vodId)){
                dao.updateStopTimeMillis(videoStopTime.stopTimeMillis, videoStopTime.vodId)
            } else {
                dao.insertStopTime(videoStopTime)
                listOfExistingId.add(videoStopTime.vodId)
            }
        }
    }

    //used blocking due to Room doesn't works on main thread
    fun getStopTimeById(vodId: Int): Long? {
        return runBlocking{
            return@runBlocking withContext(Dispatchers.IO) { dao.getStopTimeById(vodId) }
        }
    }

    /**
    *  Deletes all records which startDate more than month ago;
    */
    private fun deleteAllExpired() {
        GlobalScope.launch(Dispatchers.IO) {
            val calendar: Calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            dao.deleteByExpirationTime(calendar.timeInMillis)
        }
    }
}
