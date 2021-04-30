package com.antourage.weaverlib.screens.list.dev_settings

import android.content.Context
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.isEmptyTrimmed
import com.antourage.weaverlib.other.networking.ApiClient

enum class Environments {
    DEV, LOAD_STAGING, STAGING, DEMO, PROD
}


object EnvironmentManager {
    var currentEnv: Environments = Environments.PROD

    fun setBaseUrl(context: Context){
        if (ApiClient.BASE_URL.isEmptyTrimmed()){
            currentEnv = if(UserCache.getInstance(context)?.getEnvChoice() == null){
                Environments.PROD
            }else{
                Environments.valueOf(UserCache.getInstance(context)?.getEnvChoice()!!)
            }
            ApiClient.BASE_URL = generateUrl(
                PropertyManager.getInstance()?.getProperty(
                    PropertyManager.BASE_URL))
        }
    }

    fun getUrlForEnv(url: String?, env: Environments): String{
        url?.let {
            return if(env != Environments.PROD){
                url.replace("{env}", env.name)
            }else{
                url.replace("{env}.","")
            }
        }
        return ""
    }

    fun generateUrl(url: String?): String{
        url?.let {
            return if(currentEnv != Environments.PROD){
                url.replace("{env}", currentEnv.name)
            }else{
                url.replace("{env}.","")
            }
        }
        return ""
    }
}