package com.antourage.weaverlib.other.room

import com.antourage.weaverlib.other.models.VideoStopTime
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
class RoomRepository @Inject constructor(private var dao: VideoStopTimeDao){

    private val listOfExistingId = ArrayList<Int>()

    //todo: uncomment once RoomRepository will work actually like singleton
    /*init {
        deleteAllExpired()
    }*/

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
