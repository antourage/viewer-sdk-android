package com.antourage.weaverlib.other.networking.profile
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


internal object ProfileClient {
    private const val VERSION_SUFFIX = "api/v1/"
    private val propertyHelper = PropertyManager.getInstance()
    lateinit var profileService: ProfileService
    private var retrofit: Retrofit? = null

    private var BASE_URL = when(ApiClient.BASE_URL){
        DevSettingsDialog.BASE_URL_DEV ->  propertyHelper?.getProperty(PropertyManager.PROFILE_URL)
        DevSettingsDialog.BASE_URL_LOAD -> propertyHelper?.getProperty(PropertyManager.PROFILE_URL)
        DevSettingsDialog.BASE_URL_STAGING -> propertyHelper?.getProperty(PropertyManager.PROFILE_URL)
        DevSettingsDialog.BASE_URL_PROD -> propertyHelper?.getProperty(PropertyManager.PROFILE_URL)
        else -> propertyHelper?.getProperty(PropertyManager.PROFILE_URL)
    }

    fun getWebClient(): ProfileClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)) {
            buildRetrofit()
        }
        return this
    }

    //region Private

    private fun buildRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL + VERSION_SUFFIX)
            .client(ApiClient.getHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        profileService = retrofit?.create(ProfileService::class.java)!!
    }

    //endregion
}