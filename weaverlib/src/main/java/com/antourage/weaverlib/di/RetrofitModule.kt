package com.antourage.weaverlib.di

import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.WebService
import com.antourage.weaverlib.other.networking.base.LiveDataCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton
import android.content.Context.MODE_PRIVATE
import android.app.Application
import android.content.SharedPreferences
import com.antourage.weaverlib.screens.list.VideoListViewModel


/**
 * A simple module that provides our service - a typical module in any app.
 */

@Module
object RetrofitModule {

    private val HEADER_LANGUAGE = "Accept-Language"

    @JvmStatic @Provides @Singleton
    fun provideApiClient(): WebService = Retrofit.Builder()
        .baseUrl(BASE_URL + "/api/v1/")
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

