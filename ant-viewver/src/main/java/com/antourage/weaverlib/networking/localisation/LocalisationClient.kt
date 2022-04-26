package com.antourage.weaverlib.networking.localisation

import com.antourage.weaverlib.dev_settings.ConfigManager.LOCALISATION_URL
import com.antourage.weaverlib.networking.api.ApiClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object LocalisationClient{
    private const val VERSION_SUFFIX = "localization/projects/door/languages/"
    lateinit var localisationService: LocalisationService
    private var retrofit: Retrofit? = null

    fun getWebClient(): LocalisationClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != LOCALISATION_URL + VERSION_SUFFIX)) {
            buildRetrofit()
        }
        return this
    }

    //region Private

    private fun buildRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl(LOCALISATION_URL + VERSION_SUFFIX)
            .client(ApiClient.getHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        localisationService = retrofit?.create(LocalisationService::class.java)!!
    }

    //endregion
}