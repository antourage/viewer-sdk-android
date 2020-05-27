package com.antourage.weaverlib.other.room

import androidx.room.*
import com.antourage.weaverlib.other.models.Comment
import com.antourage.weaverlib.other.models.CommentMinimal
import com.antourage.weaverlib.other.models.Message

@Dao
internal interface CommentDao {

    @Query("SELECT * FROM comments ORDER BY id DESC")
    suspend fun getAllComments(): List<Comment>

    @Query("SELECT id, avatarUrl, email, nickname, text, type, userID, pushTimeMills FROM comments WHERE vodId == :vodId")
    suspend fun getFirebaseMessagesByVideoId(vodId: Int): List<Message>

    @Query("SELECT nickname, text, type FROM (SELECT * from comments where vodId == :vodId and type == 1 ORDER BY pushTimeMills DESC LIMIT 1) UNION SELECT * FROM (SELECT nickname, text, type from comments where vodId == :vodId and type == 0  LIMIT 1)")
    suspend fun getLastFirebaseMessagesByVideoIdByBothTypes(vodId: Int): List<CommentMinimal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(vararg comments: Comment)
}