package com.antourage.weaverlib.other.models

import androidx.room.*
import androidx.room.ForeignKey.CASCADE

@Entity(tableName = "videos")
internal data class Video(
    @ColumnInfo(name = "id") @PrimaryKey var id: Int
)