package com.antourage.weaverlib.other.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
internal data class Video(
    @ColumnInfo(name = "id") @PrimaryKey var id: Int
)