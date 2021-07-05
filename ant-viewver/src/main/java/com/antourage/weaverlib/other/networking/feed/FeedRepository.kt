package com.antourage.weaverlib.other.networking.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.convertUtcToLocal
import com.antourage.weaverlib.other.getUtcTime
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.Viewers
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.other.parseToDate
import com.antourage.weaverlib.other.room.RoomRepository
import com.antourage.weaverlib.screens.base.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.anko.collections.forEachWithIndex
import java.util.*
import kotlin.collections.ArrayList

internal class FeedRepository {
    companion object {
        var vods: MutableList<StreamResponse>? = null

        fun getLiveVideos(): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = FeedClient.getWebClient().feedService.getLiveStreams()
            }.asLiveData()

        fun getLiveViewers(id: Int): LiveData<Resource<Viewers>> =
            object : NetworkBoundResource<Viewers>() {
                override fun createCall() = FeedClient.getWebClient().feedService.getLiveViewers(id)
            }.asLiveData()

        fun getVODsForFab(lastViewDate: String?): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() =
                    FeedClient.getWebClient().feedService.getVODsForFab(lastViewDate)
            }.asLiveData()

        private fun getVODs(count: Int): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = FeedClient.getWebClient().feedService.getVODs(count)
            }.asLiveData()

        fun getVODsWithLastCommentAndStopTime(
            count: Int,
            repository: RoomRepository
        ): LiveData<Resource<List<StreamResponse>>> {
            val response = getVODs(count)
            val streamResponseLD = MutableLiveData<Resource<List<StreamResponse>>>()

            response.observeForever(object : Observer<Resource<List<StreamResponse>>> {
                override fun onChanged(resource: Resource<List<StreamResponse>>) {
                    when (resource.status) {
                        is Status.Failure -> {
                            streamResponseLD.value = resource
                            response.removeObserver(this)
                        }
                        is Status.Success -> {
                            val videoList = resource.status.data
                            if (videoList != null) {
                                if (videoList.isEmpty()) {
                                    streamResponseLD.postValue(Resource(Status.Success(videoList)))
                                } else {
                                    updateListWithLastComments(videoList, repository) {
                                        streamResponseLD.postValue(Resource(Status.Success(it)))
                                    }
                                }
                            } else {
                                streamResponseLD.value = resource
                            }
                            response.removeObserver(this)
                        }
                        is Status.Loading -> {
                            streamResponseLD.value = resource
                        }
                        else -> {
                        }
                    }
                }
            })
            return streamResponseLD
        }

        private fun updateListWithLastComments(
            videoList: List<StreamResponse>,
            repository: RoomRepository,
            onFinish: (videoListNew: List<StreamResponse>) -> Unit
        ) {
            GlobalScope.launch(Dispatchers.IO) {
                val arrayOfUpdatedIds = ArrayList<Int>()
                videoList.forEachWithIndex { index, vod ->
                    vod.id?.let { vodId ->
                        val video = repository.getVideoById(vodId)
                        if (video?.nickname != null) { //due to no data for new vod
                            vod.lastMessage = video.text
                            vod.lastMessageAuthor = video.nickname
                            vod.stopTimeMillis = video.stopTimeMillis
                            arrayOfUpdatedIds.add(vodId)
                            if (index == videoList.size - 1) {
                                if (arrayOfUpdatedIds.size == videoList.size) onFinish(videoList)
                            }
                        } else {
                            Repository.fetchLastMessage(vodId, vod.startTime?.let { startTime ->
                                convertUtcToLocal(startTime)?.time
                            } ?: 0L) {
                                vod.lastMessage = it.text
                                vod.lastMessageAuthor = it.nickname
                                vod.stopTimeMillis = it.stopTimeMillis
                                arrayOfUpdatedIds.add(vodId)
                                if (arrayOfUpdatedIds.size == videoList.size) onFinish(videoList)
                                GlobalScope.launch(Dispatchers.Unconfined) { repository.addVideo(it) }
                            }
                        }
                    }
                }
            }
        }

        internal fun updateLastSeenVod() {
            if (vods == null || vods?.isEmpty()!!) return
            val vod = vods!![0]
            val lastViewedTime = UserCache.getInstance()?.getLastViewedTime()?.parseToDate()
            val newestVodTime = Date((vod.publishDate?.parseToDate()?.time ?: 0))
            if (lastViewedTime == null || newestVodTime.after(lastViewedTime)) {
                UserCache.getInstance()?.saveLastViewedTime(newestVodTime.time.getUtcTime())
            }
        }

        internal fun invalidateIsNewProperty(newList: List<StreamResponse>) {
            val lastViewedTime = UserCache.getInstance()?.getLastViewedTime()?.parseToDate()
            newList.forEach { vod ->
                if (!vod.isLive) {
                    if (lastViewedTime == null && (vods?.find {
                            it.id?.equals(
                                vod.id
                            )== true
                        }?.isNew == false)) {
                        vod.isNew = false
                    } else if (lastViewedTime == null) {
                        vod.isNew = true
                    } else {
                        val time = Date((vod.publishDate?.parseToDate()?.time ?: 0))
                        vod.isNew =
                            !(time.before(lastViewedTime) || time == lastViewedTime || (vods?.find {
                                it.id?.equals(
                                    vod.id
                                ) == true
                            }?.isNew == false))
                    }
                }
            }
        }
    }
}