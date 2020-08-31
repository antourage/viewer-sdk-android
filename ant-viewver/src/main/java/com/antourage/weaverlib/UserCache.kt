package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.antourage.weaverlib.other.models.AdBanner
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.DEFAULT_URL
import java.lang.ref.WeakReference
import java.util.*

internal class UserCache private constructor(context: Context) {
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
        private const val SP_BANNER_IMAGE = "sp_banner_image"
        private const val SP_BANNER_URL = "sp_banner_url"
        private const val SP_USER_ID = "sp_user_id"
        private const val SP_COLLAPSED_POLL = "sp_collapsed_poll"
        private const val SP_API_KEY = "sp_api_key"
        private const val SP_TAG_LINE = "sp_tag_line"
        private const val SP_FEED_IMAGE_URL = "sp_feed_image"
        private const val SP_USER_REF_ID = "sp_user_ref_id"
        private const val SP_USER_NICKNAME = "sp_user_nickname"
        private var INSTANCE: UserCache? = null

        @Synchronized
        fun getInstance(context: Context): UserCache? {
            if (INSTANCE == null) {
                INSTANCE = UserCache(context)
            }
            return INSTANCE
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

    fun saveBanner(ad: AdBanner?){
        prefs?.edit()
            ?.putString(SP_BANNER_IMAGE, ad?.imageUrl)
            ?.putString(SP_BANNER_URL, ad?.externalUrl)
            ?.apply()
    }

    fun getBanner(): AdBanner? {
        return AdBanner(
            prefs?.getString(SP_BANNER_IMAGE, null),
            prefs?.getString(SP_BANNER_URL, null)
        )
    }

    fun getBeChoice(): String? {
        return prefs?.getString(SP_BE_CHOICE, DEFAULT_URL)
    }

    fun updateBEChoice(link: String) {
        prefs?.edit()
            ?.putString(SP_BE_CHOICE, link)
            ?.apply()
            ?.apply {
                clearUserData()
            }
    }

    fun saveUserAuthInfo(token: String, userId: Int) {
        prefs?.edit()
            ?.putString(SP_TOKEN, token)
            ?.putInt(SP_USER_ID, userId)
            ?.apply()
    }

    fun saveUserRefId(userRefId: String) {
        prefs?.edit()
            ?.putString(SP_USER_REF_ID, userRefId)
            ?.apply()
    }

    fun saveUserNickName(userNickname: String) {
        prefs?.edit()
            ?.putString(SP_USER_NICKNAME, userNickname)
            ?.apply()
    }

    fun saveCollapsedPoll(pollId: String) {
        prefs?.edit()
            ?.putString(SP_COLLAPSED_POLL, pollId)
            ?.apply()
    }

    fun saveApiKey(apiKey: String) {
        prefs?.edit()
            ?.putString(SP_API_KEY, apiKey)
            ?.apply()
    }

    fun saveTagLine(tagline: String) {
        prefs?.edit()
            ?.putString(SP_TAG_LINE, tagline)
            ?.apply()
    }

    fun saveFeedImageUrl(url: String) {
        prefs?.edit()
            ?.putString(SP_FEED_IMAGE_URL, url)
            ?.apply()
    }

    fun getCollapsedPollId(): String? {
        return prefs?.getString(SP_COLLAPSED_POLL, null)
    }

    fun getToken(): String? {
        return prefs?.getString(SP_TOKEN, null)
    }

    fun getUserId(): Int? {
        return prefs?.getInt(SP_USER_ID, -1)
    }

    fun getApiKey(): String? {
        return prefs?.getString(SP_API_KEY, null)
    }

    fun getTagLine(): String? {
        return prefs?.getString(SP_TAG_LINE, null)
    }

    fun getFeedImageUrl(): String? {
        return prefs?.getString(SP_FEED_IMAGE_URL, null)
    }

    fun getUserRefId(): String? {
        return prefs?.getString(SP_USER_REF_ID, null)
    }

    fun getUserNickName(): String? {
        return prefs?.getString(SP_USER_NICKNAME, null)
    }

    private fun clearUserData() {
        saveUserAuthInfo("", -1)
    }

}