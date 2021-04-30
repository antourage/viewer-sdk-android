package com.antourage.weaverlib.other.networking.feed

import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import com.antourage.weaverlib.screens.list.dev_settings.EnvironmentManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object FeedClient{
    private const val VERSION_SUFFIX = "api/v1/"
    lateinit var feedService: FeedService
    private var retrofit: Retrofit? = null
    private val propertyHelper = PropertyManager.getInstance()

    private var BASE_URL = EnvironmentManager.generateUrl(propertyHelper?.getProperty(PropertyManager.FEED_BASE_URL))

    fun getWebClient(): FeedClient {
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
        feedService = retrofit?.create(FeedService::class.java)!!
    }

    //endregion
}