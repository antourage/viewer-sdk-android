package com.antourage.weaverlib.dev_settings

import android.content.Context
import android.util.Log
import com.antourage.weaverlib.dev_settings.DevSettingsDialog.Companion.PROD
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

internal object ConfigManager {
    private const val TAG = "AntourageFabLogs"

    var BASE_URL = "https://developers.antourage.com/"
    var FEED_URL = "https://feed.antourage.com/"
    var LOCALISATION_URL = "https://web.antourage.com/"
    var TEAM_ID: Int? = null

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
                Log.e(TAG, "Dev file antourage_info.json not found")
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
                TEAM_ID = configFile.teamId
                LOCALISATION_URL = configFile.localisationUrl!!

            }
        } else if (isEnvOverrided() && UserCache.getInstance()
                ?.getEnvChoice() == configFile.name!!
        ) {
            BASE_URL = configFile.apiUrl!!
            FEED_URL = configFile.feedUrl!!
            TEAM_ID = configFile.teamId
            LOCALISATION_URL = configFile.localisationUrl!!
        } else {
            val env = configFile.environments?.filter {
                it.name == UserCache.getInstance()?.getEnvChoice()
            }
            if (!env.isNullOrEmpty()) {
                BASE_URL = env[0].apiUrl!!
                FEED_URL = env[0].feedUrl!!
                TEAM_ID = env[0].teamId
                LOCALISATION_URL = env[0].localisationUrl!!
            }
        }
    }

    private fun isEnvOverrided(): Boolean {
        return configFile.name != null && configFile.apiUrl != null && configFile.feedUrl != null
    }
}

data class ConfigFile(
    val teamId: Int?,
    val name: String?,
    val apiUrl: String?,
    val feedUrl: String?,
    val localisationUrl: String?,
    val environments: List<Environment>?
)

data class Environment(
    val teamId: Int?,
    val name: String?,
    val apiUrl: String?,
    val feedUrl: String?,
    val localisationUrl: String?
)