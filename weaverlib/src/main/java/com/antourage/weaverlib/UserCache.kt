package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_PROD
import java.util.*


class UserCache{
    companion object {

        private const val SP_SEEN_VIDEOS = "sp_seen_videos"
        private const val ANT_PREF = "ant_pref"
        private const val SP_BE_CHOICE = "sp_be_choice"

        fun newInstance():UserCache{
            return UserCache()
        }
    }

    fun saveVideosIdToSeen(context: Context, seenVideoIds: Set<Int> ){
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val str = StringBuilder()
        seenVideoIds.forEach{
            str.append(it).append(",")
        }
        prefs.edit().putString(SP_SEEN_VIDEOS, str.toString()).apply()
    }
    fun saveVideoToSeen(context: Context, seenVideoId: Int ){
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val str = StringBuilder()
        val alreadySeenVideos = getSeenVideos(context).toMutableSet()
        if(!alreadySeenVideos.contains(seenVideoId))
            alreadySeenVideos.add(seenVideoId)
        alreadySeenVideos.forEach{
            str.append(it).append(",")
        }
        prefs.edit().putString(SP_SEEN_VIDEOS, str.toString()).apply()
    }
    fun getSeenVideos(context: Context):Set<Int>{
        val prefs = context.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
        val savedString = prefs.getString(SP_SEEN_VIDEOS, "")
        val st = StringTokenizer(savedString, ",")
        val savedList = MutableList(st.countTokens()){0}
        var i =0
        while (st.hasMoreTokens()){
            savedList[i] = Integer.parseInt(st.nextToken())
            i++
        }
        return savedList.toHashSet()
    }
    fun getBeChoice(context: Context): String? {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPref.getString(SP_BE_CHOICE, BASE_URL_PROD)
    }

    fun updateBEChoice(context: Context, link: String) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putString(SP_BE_CHOICE, link)
        editor.apply()
    }

}