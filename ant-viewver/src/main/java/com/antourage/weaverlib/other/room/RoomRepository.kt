package com.antourage.weaverlib.other.room

import android.content.Context
import com.antourage.weaverlib.other.SingletonHolder
import com.antourage.weaverlib.other.models.Comment
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.Video
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

internal class RoomRepository private constructor(context: Context) {

    private var videoDao: VideoStopTimeDao = AppDatabase.getInstance(context).videoStopTimeDao()
    private var commentDao: CommentDao = AppDatabase.getInstance(context).commentDao()

    companion object : SingletonHolder<RoomRepository, Context>(::RoomRepository)

    init {
        deleteAllExpired()
    }

    fun addStopTime(video: Video) {
        GlobalScope.launch(Dispatchers.IO) { videoDao.upsertStopTime(video) }
    }

    suspend fun addVideo(video: Video) {
        videoDao.insertVideoIfNotExists(video)
    }

    //used blocking due to Room doesn't works on main thread
    fun getStopTimeById(vodId: Int): Long? {
        return runBlocking {
            return@runBlocking withContext(Dispatchers.IO) { videoDao.getStopTimeById(vodId) }
        }
    }

    suspend fun getVideoById(vodId: Int): Video? = videoDao.getVideoById(vodId)

    fun insertAllComments(video: Video, message: List<Message>) {
        GlobalScope.launch(Dispatchers.IO) {
            videoDao.insertVideoIfNotExists(video)
            val comments = transformMessages(message, video.id)
            commentDao.insertComments(*comments)
        }
    }

    suspend fun getFirebaseMessagesById(vodId: Int) = commentDao.getFirebaseMessagesByVideoId(vodId)

    private fun transformMessages(messages: List<Message>, vodId: Int): Array<Comment> {
        val listOfComments = ArrayList<Comment>()

        messages.forEach {
            listOfComments.add(
                Comment(
                    it.id ?: System.currentTimeMillis().toString(),
                    vodId,
                    it.avatarUrl,
                    it.email,
                    it.nickname,
                    it.text,
                    it.type,
                    it.userID,
                    it.pushTimeMills
                )
            )
        }
        return listOfComments.toTypedArray()
    }

    /**
     *  Deletes all records which startDate more than month ago;
     */
    private fun deleteAllExpired() {
        GlobalScope.launch(Dispatchers.IO) {
            val calendar: Calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -1)
            videoDao.deleteByExpirationTime(calendar.timeInMillis)
        }
    }
}
