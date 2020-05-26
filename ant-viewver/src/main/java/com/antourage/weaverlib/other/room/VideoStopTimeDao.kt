package com.antourage.weaverlib.other.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.antourage.weaverlib.other.models.VideoStopTime

@Dao
internal interface VideoStopTimeDao {

    @Query("SELECT * FROM VideoStopTimes ORDER BY vodId DESC")
    suspend fun getAllStopTimeRecords(): List<VideoStopTime>

    @Query("SELECT stopTimeMillis FROM VideoStopTimes WHERE vodId == :vodId")
    suspend fun getStopTimeById(vodId: Int): Long

    @Query("UPDATE VideoStopTimes SET stopTimeMillis = :stopTimeMillis WHERE vodId == :vodId")
    fun updateStopTimeMillis(stopTimeMillis: Long, vodId:Int)

    /**
     * Insert a record in the database. If the record already exists, replace it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStopTime(videoStopTime: VideoStopTime): Long


    @Query("DELETE FROM VideoStopTimes WHERE startDate < :expirationDate")
    fun deleteByExpirationTime(expirationDate: Long) : Int
}