package com.antourage.weaverlib.other.room

import androidx.room.*
import com.antourage.weaverlib.other.models.Video

@Dao
internal interface ShownStreamsDao {

    @Query("SELECT EXISTS(SELECT * FROM videos WHERE id = :id)")
    suspend fun isSeen(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addToSeen(video: Video): Long
}