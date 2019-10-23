package com.antourage.weaverlib.di

import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import com.antourage.weaverlib.other.networking.WebService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * A simple module that provides our service - a typical module in any app.
 */

@Module
internal object RetrofitModule {

    private const val HEADER_LANGUAGE = "Accept-Language"

    @JvmStatic
    @Provides
    @Singleton
    fun provideApiClient(): WebService = Retrofit.Builder()
        .baseUrl("$BASE_URL/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader(HEADER_LANGUAGE, "en")
                .build()
            chain.proceed(request)
        }.build())
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(LiveDataCallAdapterFactory())
        .build()
        .create(WebService::class.java)
}

