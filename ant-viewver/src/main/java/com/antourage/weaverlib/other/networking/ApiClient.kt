package com.antourage.weaverlib.other.networking

import com.antourage.weaverlib.ConfigManager.BASE_URL
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object ApiClient {
    private const val VERSION_SUFFIX = "api/v3/"

    lateinit var apiService: ApiService
    private var retrofit: Retrofit? = null
    private var httpClient: OkHttpClient? = null

    fun getWebClient(): ApiClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)) {
            buildRetrofit()
        }
        return this
    }

    fun getHttpClient(): OkHttpClient {
        if (httpClient == null) {
            httpClient = buildOkHttpClient()
        }
        return httpClient as OkHttpClient
    }

    //region Private

    private fun buildRetrofit() {
        httpClient = buildOkHttpClient()
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL + VERSION_SUFFIX)
            .client(httpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        apiService = retrofit?.create(ApiService::class.java)!!
    }

    private fun buildOkHttpClient(): OkHttpClient {
        //TODO delete before release
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val builder = OkHttpClient.Builder()
        builder
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
        return builder.build()
    }

    //endregion
}