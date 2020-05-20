package com.antourage.weaverlib.other.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "VideoStopTimes")
data class VideoStopTime(
    @PrimaryKey
    var vodId: Int,
    @ColumnInfo(name = "stopTimeMillis")
    var stopTimeMillis: Long,
    @ColumnInfo(name = "startDate")
    var expirationDate: Long
)