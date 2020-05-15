package com.antourage.weaverlib.di

import android.app.Application
import androidx.room.Room
import com.antourage.weaverlib.other.room.AppDatabase
import com.antourage.weaverlib.other.room.VideoStopTimeDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class RoomModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, "AppDatabase.db").build()
    }

    @Singleton
    @Provides
    fun providesRecordDao(db: AppDatabase): VideoStopTimeDao {
        return db.videoStopTimeDao()
    }
}