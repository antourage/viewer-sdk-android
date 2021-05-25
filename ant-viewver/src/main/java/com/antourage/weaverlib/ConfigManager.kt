package com.antourage.weaverlib

import android.content.Context
import android.util.Log
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.PROD
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

internal object ConfigManager {
    internal const val TAG = "AntourageFabLogs"

    var BASE_URL = "https://api.antourage.com/"
    var PROFILE_URL = "https://profile.antourage.com/"
    var WEB_PROFILE_URL = "https://widget.antourage.com/"
    var FEED_URL = "https://feed.antourage.com/"
    var AUTH_URL = "https://identity.antourage.com"
    var CLIENT_ID = ""
    var ANONYMOUS_CLIENT_ID = ""
    var ANONYMOUS_SECRET = ""
    lateinit var configFile: ConfigFile

    fun init(context: Context) {
        context.assets?.let {
            val gson = Gson()
            var i: InputStream? = null
            try {
                i = it.open("antourage_info.json")
                val br = BufferedReader(InputStreamReader(i))
                configFile = gson.fromJson(br, ConfigFile::class.java)
                setupSecrets()
                setupUrls()
            } catch (e: Exception) {
                Log.e(TAG, "File antourage_info.json not found")
            }
        }
    }

    internal fun isConfigInitialized(): Boolean{
        return this::configFile.isInitialized
    }

    private fun setupSecrets() {
        CLIENT_ID = configFile.appClientId ?: ""
        ANONYMOUS_CLIENT_ID = configFile.anonymousAppClientId ?: ""
        ANONYMOUS_SECRET = configFile.anonymousAppClientSecret ?: ""
    }

    private fun setupUrls() {
        if (UserCache.getInstance()?.getEnvChoice() == PROD) {
            if (isEnvOverrided()) {
                UserCache.getInstance()?.updateEnvChoice(configFile.name!!)
                BASE_URL = configFile.apiUrl!!
                PROFILE_URL = configFile.profileUrl!!
                WEB_PROFILE_URL = configFile.webWidgetUrl!!
                FEED_URL = configFile.feedUrl!!
                AUTH_URL = configFile.authUrl!!
            }
        } else if (isEnvOverrided() && UserCache.getInstance()
                ?.getEnvChoice() == configFile.name!!
        ) {
            BASE_URL = configFile.apiUrl!!
            PROFILE_URL = configFile.profileUrl!!
            WEB_PROFILE_URL = configFile.webWidgetUrl!!
            FEED_URL = configFile.feedUrl!!
            AUTH_URL = configFile.authUrl!!
        } else {
            val env = configFile.environments?.filter {
                it.name == UserCache.getInstance()?.getEnvChoice()
            }
            if (!env.isNullOrEmpty()) {
                BASE_URL = env[0].apiUrl!!
                PROFILE_URL = env[0].profileUrl!!
                WEB_PROFILE_URL = env[0].webWidgetUrl!!
                FEED_URL = env[0].feedUrl!!
                AUTH_URL = env[0].authUrl!!
                CLIENT_ID = env[0].appClientId!!
                ANONYMOUS_CLIENT_ID = env[0].anonymousAppClientId!!
                ANONYMOUS_SECRET = env[0].anonymousAppClientSecret!!
            }
        }
    }

    fun isEnvOverrided(): Boolean {
        return configFile.name != null && configFile.apiUrl != null && configFile.profileUrl != null && configFile.webWidgetUrl != null && configFile.feedUrl != null && configFile.authUrl != null
    }
}

data class ConfigFile(
    val appClientId: String?,
    val anonymousAppClientId: String?,
    val anonymousAppClientSecret: String?,
    val redirectSignIn: String?,
    val redirectSignOut: String?,
    val name: String?,
    val apiUrl: String?,
    val profileUrl: String?,
    val webWidgetUrl: String?,
    val feedUrl: String?,
    val authUrl: String?,
    val environments: List<Environment>?
)

data class Environment(
    val name: String?,
    val appClientId: String?,
    val anonymousAppClientId: String?,
    val anonymousAppClientSecret: String?,
    val apiUrl: String?,
    val profileUrl: String?,
    val webWidgetUrl: String?,
    val feedUrl: String?,
    val authUrl: String?
)