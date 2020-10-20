package com.antourage.weaverlib.other.networking

import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.models.LiveClosedRequest
import com.antourage.weaverlib.other.models.SimpleResponse
import com.antourage.weaverlib.other.models.VideoClosedRequest
import com.antourage.weaverlib.screens.base.Repository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VideoCloseBackUp {
    companion object {
        private lateinit var handler: Handler
        private lateinit var prefs: SharedPreferences
        private var justCalled: Boolean = false

        fun init(prefs: SharedPreferences) {
            this.prefs = prefs
            handler = Handler()
        }

        internal fun sendBackUps() {
            if (justCalled) return
            justCalled = true
            handler.postDelayed({
                justCalled = false
            }, 5000)

            fetchVodBackUpList().forEach {
                val response = Repository.postVideoClosed(it)
                backUpVodStopInfo(it, justRemove = true)
                response.observeForever(object : Observer<Resource<SimpleResponse>> {
                    override fun onChanged(resource: Resource<SimpleResponse>?) {
                        if (resource != null) {
                            when (resource.status) {
                                is Status.Failure -> {
                                    Log.d(
                                        "STAT_CLOSE",
                                        "Failed to send backup vod/close: ${resource.status.errorMessage}"
                                    )
                                    backUpVodStopInfo(it)
                                    response.removeObserver(this)
                                }
                                is Status.Success -> {
                                    Log.d(
                                        "STAT_CLOSE",
                                        "Successfully sent backup vod/close: ${it.span}"
                                    )
                                    response.removeObserver(this)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                })
            }

            fetchLiveBackUpList().forEach {
                val response = Repository.postLiveClosed(it)
                backUpLiveStopInfo(it, justRemove = true)
                response.observeForever(object : Observer<Resource<SimpleResponse>> {
                    override fun onChanged(resource: Resource<SimpleResponse>?) {
                        if (resource != null) {
                            when (resource.status) {
                                is Status.Failure -> {
                                    Log.d(
                                        "STAT_CLOSE",
                                        "Failed to send backup vod/close: ${resource.status.errorMessage}"
                                    )
                                    backUpLiveStopInfo(it)
                                    response.removeObserver(this)
                                }
                                is Status.Success -> {
                                    Log.d(
                                        "STAT_CLOSE",
                                        "Successfully sent backup vod/close: ${it.span}"
                                    )
                                    response.removeObserver(this)
                                }
                                else -> {
                                }
                            }
                        }
                    }
                })
            }
        }

        internal fun backUpVodStopInfo(obj: VideoClosedRequest, justRemove: Boolean = false) {
            val gson = Gson()
            val list = fetchVodBackUpList()
            list.removeAll { it.vodId == obj.vodId }
            if (!justRemove) list.add(obj)
            val json = gson.toJson(list)
            backUpToPrefs(json, "vodCloseList")
        }

        internal fun backUpLiveStopInfo(obj: LiveClosedRequest, justRemove: Boolean = false) {
            val gson = Gson()
            val list = fetchLiveBackUpList()
            list.removeAll { it.streamId == obj.streamId }
            if (!justRemove) list.add(obj)
            val json = gson.toJson(list)
            backUpToPrefs(json, "liveCloseList")
        }

        private fun backUpToPrefs(jsonObj: String, key: String) {
            val prefsEditor = prefs.edit()
            prefsEditor.putString(key, jsonObj)
            prefsEditor.apply()
        }

        private fun fetchVodBackUpList(): ArrayList<VideoClosedRequest> {
            val gson = Gson()
            val yourArrayList: ArrayList<VideoClosedRequest>
            val json = prefs.getString("vodCloseList", "")

            yourArrayList = when {
                json.isNullOrEmpty() -> ArrayList()
                else -> gson.fromJson(json, object : TypeToken<List<VideoClosedRequest>>() {}.type)
            }
            return yourArrayList
        }

        private fun fetchLiveBackUpList(): ArrayList<LiveClosedRequest> {
            val gson = Gson()
            val yourArrayList: ArrayList<LiveClosedRequest>
            val json = prefs.getString("liveCloseList", "")

            yourArrayList = when {
                json.isNullOrEmpty() -> ArrayList()
                else -> gson.fromJson(json, object : TypeToken<List<LiveClosedRequest>>() {}.type)
            }
            return yourArrayList
        }
    }
}