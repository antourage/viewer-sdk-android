package com.antourage.weaverlib.other.room

import androidx.room.*
import com.antourage.weaverlib.other.models.Comment
import com.antourage.weaverlib.other.models.Message

@Dao
internal interface CommentDao {

    @Query("SELECT id, avatarUrl, email, nickname, text, type, userID, pushTimeMills FROM comments WHERE vodId == :vodId")
    suspend fun getFirebaseMessagesByVideoId(vodId: Int): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(vararg comments: Comment)
}