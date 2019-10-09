package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_DEV
import java.lang.ref.WeakReference
import java.util.*

class UserCache private constructor(context: Context) {
    private var contextRef: WeakReference<Context>? = null
    private var prefs: SharedPreferences? = null

    init {
        this.contextRef = WeakReference(context)
        this.prefs = contextRef?.get()?.getSharedPreferences(ANT_PREF, MODE_PRIVATE)
    }

    companion object {
        private const val ANT_PREF = "ant_pref"
        private const val SP_SEEN_VIDEOS = "sp_seen_videos"
        private const val SP_BE_CHOICE = "sp_be_choice"
        private const val SP_TOKEN = "sp_token"
        private val INSTANCE: UserCache? = null

        //TODO: delete
        const val API_KEY_1 = "a5f76ee9-bc76-4f76-a042-933b8993fc2c"
        const val API_KEY_2 = "4ec7cb01-a379-4362-a3a4-89699c17dc32"

        @Synchronized
        fun getInstance(context: Context): UserCache? {
            return INSTANCE ?: UserCache(context)
        }

        @Synchronized
        fun getInstance(): UserCache? {
            return INSTANCE
        }
    }

    fun saveVideoToSeen(seenVideoId: Int) {
        val str = StringBuilder()
        val alreadySeenVideos = getSeenVideos().toMutableSet()
        if (!alreadySeenVideos.contains(seenVideoId))
            alreadySeenVideos.add(seenVideoId)
        alreadySeenVideos.forEach {
            str.append(it).append(",")
        }
        prefs?.edit()?.putString(SP_SEEN_VIDEOS, str.toString())?.apply()
    }

    fun getSeenVideos(): Set<Int> {
        val savedString = prefs?.getString(SP_SEEN_VIDEOS, "")
        val st = StringTokenizer(savedString, ",")
        val savedList = MutableList(st.countTokens()) { 0 }
        var i = 0
        while (st.hasMoreTokens()) {
            savedList[i] = Integer.parseInt(st.nextToken())
            i++
        }
        return savedList.toHashSet()
    }

    fun getBeChoice(): String? {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(contextRef?.get())
        return sharedPref.getString(SP_BE_CHOICE, BASE_URL_DEV)
    }

    fun updateBEChoice(link: String) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(contextRef?.get())
        val editor = sharedPref.edit()
        editor.putString(SP_BE_CHOICE, link)
        editor.apply()
    }

    fun saveToken(token: String) {
        prefs?.edit()
            ?.putString(SP_TOKEN, token)
            ?.apply()
    }

    fun getToken(): String? {
        return prefs?.getString(SP_TOKEN, null)
    }
}