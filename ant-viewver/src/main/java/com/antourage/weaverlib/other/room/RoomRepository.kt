package com.antourage.weaverlib.other.room

import com.antourage.weaverlib.other.models.VideoStopTime
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomRepository @Inject constructor(private var dao: VideoStopTimeDao){

    private val listOfExistingId = ArrayList<Int>()

    /*fun updateStopTimeMillis(stopTimeMillis: Long, vodId:Int) {
        GlobalScope.launch(Dispatchers.IO) {
            dao.updateStopTimeMillis(stopTimeMillis, vodId)
        }
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
}
