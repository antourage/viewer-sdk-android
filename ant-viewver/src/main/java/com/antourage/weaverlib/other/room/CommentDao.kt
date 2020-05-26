package com.antourage.weaverlib.other.room

import androidx.room.*
import com.antourage.weaverlib.other.models.Comment
import com.antourage.weaverlib.other.models.Message

@Dao
internal interface CommentDao {

    @Query("SELECT * FROM comments ORDER BY id DESC")
    suspend fun getAllComments(): List<Comment>

    /*@Query("SELECT * FROM comments WHERE vodId == :vodId")
    suspend fun getCommentsByVideoId(vodId: Int): List<Comment>*/

    @Query("SELECT id, avatarUrl, email, nickname, text, type, userID, pushTimeMills FROM comments WHERE vodId == :vodId")
    suspend fun getFirebaseMessagesByVideoId(vodId: Int): List<Message>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(vararg comments: Comment)

    /*@Query("UPDATE videos SET stopTimeMillis = :stopTimeMillis WHERE id == :vodId")
    fun updateStopTimeMillis(stopTimeMillis: Long, vodId:Int)

    *//**
     * Insert a record in the database. If the record already exists, replace it.
     *//*
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStopTime(video: Video): Long


    @Query("DELETE FROM videos WHERE startDate < :expirationDate")
    fun deleteByExpirationTime(expirationDate: Long) : Int*/
}