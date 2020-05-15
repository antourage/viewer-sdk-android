package com.antourage.weaverlib.other.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.antourage.weaverlib.other.models.VideoStopTime

@Database(entities = [VideoStopTime::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoStopTimeDao(): VideoStopTimeDao
}