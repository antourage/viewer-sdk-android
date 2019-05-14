package com.antourage.weaverlib

import android.content.Context
import android.R.id.edit
import android.content.Context.MODE_PRIVATE
import java.util.*


class UserCache{
    companion object {

        const val SP_SEEN_VIDEOS = "sp_seen_videos"
        const val ANT_PREF = "ant_pref"

        fun newInstance():UserCache{
            return UserCache()
        }
    }

    public fun saveVideosIdToSeen(context: Context, seenVideoIds: List<Int> ){
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val str = StringBuilder()
        for (i in seenVideoIds.indices) {
            str.append(seenVideoIds[i]).append(",")
        }
        prefs.edit().putString(SP_SEEN_VIDEOS, str.toString()).apply()
    }
    public fun saveVideoToSeen(context: Context, seenVideoId: Int ){
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val str = StringBuilder()
        val alreadySeenVideos = getSeenVideos(context).toMutableList()
        alreadySeenVideos.add(seenVideoId)
        for (i in alreadySeenVideos.indices) {
            str.append(alreadySeenVideos[i]).append(",")
        }
        prefs.edit().putString(SP_SEEN_VIDEOS, str.toString()).apply()
    }
    public fun getSeenVideos(context: Context):List<Int>{
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val savedString = prefs.getString(SP_SEEN_VIDEOS, "")
        val st = StringTokenizer(savedString, ",")
        val savedList = MutableList(st.countTokens()){0}
        var i =0
        while (st.hasMoreTokens()){
            savedList[i] = Integer.parseInt(st.nextToken())
            i++
        }
        return savedList.toList()
    }

}