package com.antourage.weaverlib.other.networking.feed

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.getUtcTime
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.parseToDate
import java.util.*

internal class FeedRepository {
    companion object {
        var vods: MutableList<StreamResponse>? = null

        fun getLiveVideos(): LiveData<Resource<List<StreamResponse>>> =
            object : NetworkBoundResource<List<StreamResponse>>() {
                override fun createCall() = FeedClient.getWebClient().feedService.getLiveStreams()
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