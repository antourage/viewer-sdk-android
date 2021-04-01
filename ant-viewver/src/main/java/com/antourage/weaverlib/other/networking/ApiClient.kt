package com.antourage.weaverlib.other.networking

import com.antourage.weaverlib.other.networking.auth.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


internal object ApiClient {
    var BASE_URL = ""
    private const val VERSION_SUFFIX = "api/v1/widget/"

    lateinit var webService: WebService
    private var retrofit: Retrofit? = null
    private var httpClient: OkHttpClient? = null

    fun getWebClient(): ApiClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)) {
            rebuildRetrofit()
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

    private fun rebuildRetrofit() {
        httpClient = buildOkHttpClient()
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL + VERSION_SUFFIX)
            .client(httpClient!!)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        webService = retrofit?.create(WebService::class.java)!!
    }

    private fun buildOkHttpClient(): OkHttpClient {
        //TODO delete before release
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        val builder = OkHttpClient.Builder()
        builder
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
        return builder.build()
    }

    //endregion
}