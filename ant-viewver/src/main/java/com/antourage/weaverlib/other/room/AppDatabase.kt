package com.antourage.weaverlib.other.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antourage.weaverlib.other.SingletonHolder
import com.antourage.weaverlib.other.models.Video

@Database(entities = [Video::class], version = 1)
internal abstract class AppDatabase : RoomDatabase() {

    abstract fun shownStreamsDao(): ShownStreamsDao

    companion object : SingletonHolder<AppDatabase, Context>({
        Room.databaseBuilder(
            it.applicationContext,
            AppDatabase::class.java, "AntourageDatabase.db"
        )
            .build()
    })
}