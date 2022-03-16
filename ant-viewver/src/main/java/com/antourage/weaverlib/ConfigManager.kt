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

    var BASE_URL = "https://developers.dev3.antourage.com/"
    var FEED_URL = "https://feed.dev3.antourage.com/"

    lateinit var configFile: ConfigFile

    fun init(context: Context) {
        context.assets?.let {
            val gson = Gson()
            val i: InputStream?
            try {
                i = it.open("antourage_info.json")
                val br = BufferedReader(InputStreamReader(i))
                configFile = gson.fromJson(br, ConfigFile::class.java)
                setupUrls()
            } catch (e: Exception) {
                Log.e(TAG, "File antourage_info.json not found")
            }
        }
    }

    internal fun isConfigInitialized(): Boolean {
        return this::configFile.isInitialized
    }

    private fun setupUrls() {
        if (UserCache.getInstance()?.getEnvChoice() == PROD) {
            if (isEnvOverrided()) {
                UserCache.getInstance()?.updateEnvChoice(configFile.name!!)
                BASE_URL = configFile.apiUrl!!
                FEED_URL = configFile.feedUrl!!
            }
        } else if (isEnvOverrided() && UserCache.getInstance()
                ?.getEnvChoice() == configFile.name!!
        ) {
            BASE_URL = configFile.apiUrl!!
            FEED_URL = configFile.feedUrl!!
        } else {
            val env = configFile.environments?.filter {
                it.name == UserCache.getInstance()?.getEnvChoice()
            }
            if (!env.isNullOrEmpty()) {
                BASE_URL = env[0].apiUrl!!
                FEED_URL = env[0].feedUrl!!
            }
        }
    }

    fun isEnvOverrided(): Boolean {
        return configFile.name != null && configFile.apiUrl != null && configFile.feedUrl != null
    }
}

data class ConfigFile(
    val name: String?,
    val apiUrl: String?,
    val feedUrl: String?,
    val environments: List<Environment>?
)

data class Environment(
    val name: String?,
    val apiUrl: String?,
    val feedUrl: String?
)