package com.antourage.weaverlib.other.room

import android.provider.ContactsContract
import androidx.room.*
import com.antourage.weaverlib.other.models.Video


@Dao
internal interface VideoStopTimeDao {

    @Query("SELECT * FROM videos ORDER BY id DESC")
    suspend fun getAllStopTimeRecords(): List<Video>

    @Query("SELECT stopTimeMillis FROM videos WHERE id == :vodId")
    suspend fun getStopTimeById(vodId: Int): Long

    @Query("SELECT * FROM videos WHERE id == :vodId")
    suspend fun getVideoById(vodId: Int): Video?

    /**
     * Used to check whether table has respective vod
     */
    @Query("SELECT COUNT(*) FROM videos WHERE id == :vodId")
    suspend fun getCountOfStopTimeById(vodId: Int): Int

    @Query("DELETE FROM videos WHERE startDate < :expirationDate")
    suspend fun deleteByExpirationTime(expirationDate: Long) : Int

    @Query("UPDATE videos SET stopTimeMillis = :stopTimeMillis WHERE id == :vodId")
    suspend fun updateStopTimeMillis(stopTimeMillis: Long, vodId:Int)

    @Query("UPDATE videos SET nickname = :nickname, text = :text WHERE id == :vodId")
    suspend fun updateLastMessage(vodId:Int, nickname: String, text: String)

    @Query("DELETE FROM videos")
    suspend fun clearVideos()

    /**
     * Insert a record in the database.
     *  @return The SQLite row id
     * If the record already exists returns -1 and does nothing
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideoIfNotExists(video: Video): Long

    @Transaction
    suspend fun upsertStopTime(video: Video) {
        val resultCode = insertVideoIfNotExists(video)
        if (resultCode == -1L) {
            updateStopTimeMillis(video.stopTimeMillis, video.id)
        }
    }

    @Transaction
    suspend fun upsertLastMessage(video: Video) {
        val resultCode = insertVideoIfNotExists(video)
        if (resultCode == -1L) {
            updateStopTimeMillis(video.stopTimeMillis, video.id)
        }
    }
}