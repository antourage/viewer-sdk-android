package com.antourage.weaverlib.other.networking.push

import com.antourage.weaverlib.ConfigManager.BASE_URL
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// TODO merge clients as URL is the same now

internal object PushClient{
    private const val VERSION_SUFFIX = "api/v3/"
    lateinit var pushService: PushService
    private var retrofit: Retrofit? = null

    fun getWebClient(): PushClient {
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
        pushService = retrofit?.create(PushService::class.java)!!
    }

    //endregion
}