package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.antourage.weaverlib.other.models.AdBanner
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import java.lang.ref.WeakReference
import java.util.*

//TODO make internal
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
        private const val SP_ENV_CHOICE = "sp_env_choice"
        private const val SP_BANNER_IMAGE = "sp_banner_image"
        private const val SP_BANNER_URL = "sp_banner_url"
        private const val SP_USER_ID = "sp_user_id"
        private const val SP_COLLAPSED_POLL = "sp_collapsed_poll"
        private const val SP_TAG_LINE = "sp_tag_line"
        private const val SP_FEED_IMAGE_URL = "sp_feed_image"

        const val SP_DEVICE_ID = "sp_device_id"
        private const val SP_ACCESS_TOKEN = "sp_access_token"
        private const val SP_ID_TOKEN = "sp_id_token"
        private const val SP_REFRESH_TOKEN = "sp_refresh_token"

        private const val SP_USER_NICKNAME = "sp_user_nickname"
        private const val SP_USER_IMAGE_URL = "sp_user_image_url"

        private const val SP_USER_LAST_FETCHED_VOD = "sp_last_fetched_vod"

        private const val SP_ONBOARDING_SEEN = "sp_onboarding_seen"

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

    fun logout(){
        getInstance()?.saveAccessToken(null)
        getInstance()?.saveRefreshToken(null)
        getInstance()?.saveIdToken(null)
        getInstance()?.saveUserNickName("")
        getInstance()?.saveUserImage("")
        getInstance()?.saveUserId("")
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

    private fun getSeenVideos(): Set<Int> {
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

    fun getEnvChoice(): String {
        return prefs?.getString(SP_ENV_CHOICE, null)?: DevSettingsDialog.PROD
    }

    fun updateEnvChoice(env: String) {
        prefs?.edit()
            ?.putString(SP_ENV_CHOICE, env)
            ?.apply()
    }

    fun saveCollapsedPoll(pollId: String) {
        prefs?.edit()
            ?.putString(SP_COLLAPSED_POLL, pollId)
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

    fun saveAccessToken(token: String?) {
        prefs?.edit()
            ?.putString(SP_ACCESS_TOKEN, token)
            ?.apply()
    }

    fun saveIdToken(token: String?) {
        prefs?.edit()
            ?.putString(SP_ID_TOKEN, token)
            ?.apply()
    }

    fun saveRefreshToken(token: String?) {
        prefs?.edit()
            ?.putString(SP_REFRESH_TOKEN, token)
            ?.apply()
    }

    fun saveUserNickName(userNickname: String) {
        prefs?.edit()
            ?.putString(SP_USER_NICKNAME, userNickname)
            ?.apply()
    }

    fun saveUserId(userId: String) {
        prefs?.edit()
            ?.putString(SP_USER_ID, userId)
            ?.apply()
    }

    fun saveUserImage(imageUrl: String) {
        prefs?.edit()
            ?.putString(SP_USER_IMAGE_URL, imageUrl)
            ?.apply()
    }

    private fun saveUserInfo(userId: Int, userNickname: String) {
        prefs?.edit()
            ?.putString(SP_USER_ID, userId.toString())
            ?.putString(SP_USER_NICKNAME, userNickname)
            ?.apply()
    }

    fun saveDeviceId(deviceId: String) {
        prefs?.edit()
            ?.putString(SP_DEVICE_ID, deviceId)
            ?.apply()
    }

    fun saveLastViewedTime(lastVodTime: String) {
        prefs?.edit()
            ?.putString(SP_USER_LAST_FETCHED_VOD, lastVodTime)
            ?.apply()
    }

    fun setOnboardingSeen() {
        prefs?.edit()
            ?.putBoolean(SP_ONBOARDING_SEEN, true)
            ?.apply()
    }

    fun isOnboardingSeen(): Boolean? {
        return prefs?.getBoolean(SP_ONBOARDING_SEEN, false)
    }

    fun getLastViewedTime(): String? {
        return prefs?.getString(SP_USER_LAST_FETCHED_VOD, null)
    }

    fun getDeviceId(): String? {
        return prefs?.getString(SP_DEVICE_ID, null)
    }

    fun getCollapsedPollId(): String? {
        return prefs?.getString(SP_COLLAPSED_POLL, null)
    }

    fun getTagLine(): String? {
        return prefs?.getString(SP_TAG_LINE, null)
    }

    fun getFeedImageUrl(): String? {
        return prefs?.getString(SP_FEED_IMAGE_URL, null)
    }

    fun getIdToken(): String? {
        return prefs?.getString(SP_ID_TOKEN, null)
    }

    fun getAccessToken(): String? {
        return prefs?.getString(SP_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return prefs?.getString(SP_REFRESH_TOKEN, null)
    }

    fun getUserId(): String? {
        return prefs?.getString(SP_USER_ID, null)
    }

    fun getUserNickName(): String? {
        return prefs?.getString(SP_USER_NICKNAME, null)
    }

    fun getUserImageUrl(): String? {
        return prefs?.getString(SP_USER_IMAGE_URL, null)
    }

    fun clearUserData() {
        saveUserInfo(-1, "")
    }


}