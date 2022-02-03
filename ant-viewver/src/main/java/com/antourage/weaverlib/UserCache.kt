package com.antourage.weaverlib

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import com.antourage.weaverlib.screens.web.PreFeedFragment
import java.lang.ref.WeakReference

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
        private const val SP_ENV_CHOICE = "sp_env_choice"
        private const val SP_USER_ID = "sp_user_id"

        const val SP_DEVICE_ID = "sp_device_id"
        private const val SP_ACCESS_TOKEN = "sp_access_token"
        private const val SP_ID_TOKEN = "sp_id_token"
        private const val SP_REFRESH_TOKEN = "sp_refresh_token"

        private const val SP_LAST_VIEW_DATE = "sp_last_view_date"

        private const val SP_USER_NICKNAME = "sp_user_nickname"
        private const val SP_USER_IMAGE_URL = "sp_user_image_url"

        private const val SP_USER_LAST_FETCHED_VOD = "sp_last_fetched_vod"

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

    fun logout() {
        getInstance()?.saveAccessToken(null)
        getInstance()?.saveRefreshToken(null)
        getInstance()?.saveIdToken(null)
        getInstance()?.saveUserId("")
    }

    fun getEnvChoice(): String {
        return prefs?.getString(SP_ENV_CHOICE, null) ?: DevSettingsDialog.PROD
    }

    fun updateEnvChoice(env: String) {
        prefs?.edit()
            ?.putString(SP_ENV_CHOICE, env)
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

    fun getLastViewedTime(): String? {
        return prefs?.getString(SP_USER_LAST_FETCHED_VOD, null)
    }


    fun getDeviceId(): String? {
        return prefs?.getString(SP_DEVICE_ID, null)
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

    fun clearUserData() {
        saveUserInfo(-1, "")
    }


}