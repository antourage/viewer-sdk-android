package com.antourage.weaverlib.other.networking.feed

import com.antourage.weaverlib.ConfigManager.FEED_URL
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object FeedClient{
    private const val VERSION_SUFFIX = "api/v1/"
    lateinit var feedService: FeedService
    private var retrofit: Retrofit? = null

    fun getWebClient(): FeedClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != FEED_URL + VERSION_SUFFIX)) {
            buildRetrofit()
        }
        return this
    }

    //region Private

    private fun buildRetrofit() {
        retrofit = Retrofit.Builder()
            .baseUrl(FEED_URL + VERSION_SUFFIX)
            .client(ApiClient.getHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        feedService = retrofit?.create(FeedService::class.java)!!
    }

    //endregion
}