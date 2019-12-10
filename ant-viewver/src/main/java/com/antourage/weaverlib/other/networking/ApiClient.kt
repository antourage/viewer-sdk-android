package com.antourage.weaverlib.other.networking

import android.util.Log
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog.Companion.BASE_URL_STAGING
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

internal object ApiClient {

    //    var BASE_URL = ""
    var BASE_URL = BASE_URL_STAGING
    private const val HEADER_TOKEN = "token"
    private const val HEADER_LANGUAGE = "Accept-Language"
    private const val VERSION_SUFFIX = "api/v1/"

    lateinit var webService: WebService
    private var retrofit: Retrofit? = null
    private var usingAuth = false

    fun getWebClient(useAuth: Boolean = true): ApiClient {
        if (retrofit == null
            || usingAuth != useAuth
            || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)
        ) {
            rebuildRetrofit(useAuth)
        }
        return this
    }

    //region Private

    private fun getTokenForRequest(): String {
        return UserCache.getInstance()?.let { userCache ->
            userCache.getToken() ?: ""
        } ?: ""
    }

    private fun rebuildRetrofit(useAuth: Boolean) {
        val client = buildOkHttpClient(useAuth)
        Log.d("AntApiClient", "rebuildRetrofit $BASE_URL")
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL + VERSION_SUFFIX)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        webService = retrofit?.create(WebService::class.java)!!
    }

    private fun buildOkHttpClient(useAuth: Boolean): OkHttpClient {
        usingAuth = useAuth
        val builder = OkHttpClient.Builder()
        if (useAuth) {
            addTokenInterceptor(builder)
        } else {
            addDefaultInterceptor(builder)
        }
        return builder.build()
    }

    private fun addDefaultInterceptor(builder: OkHttpClient.Builder) {
        // This automatically adds the accessToken for any requests if the @useAuth parameter is true and if there' any accessToken
        val defaultInterceptor = Interceptor { chain ->
            val request = chain.request()
            var newRequest = request
            newRequest = newRequest.newBuilder()
                .addHeader(HEADER_LANGUAGE, "en")
                .build()
            chain.proceed(newRequest)
        }
        builder.addInterceptor(defaultInterceptor)
    }

    private fun addTokenInterceptor(builder: OkHttpClient.Builder) {
        // This automatically adds the accessToken for any requests if the @useAuth parameter is true and if there' any accessToken
        val tokenInterceptor = Interceptor { chain ->
            val request = chain.request()
            var newRequest = request
            newRequest = newRequest.newBuilder()
                .addHeader(HEADER_TOKEN, getTokenForRequest())
                .addHeader(HEADER_LANGUAGE, "en")
                .build()
            chain.proceed(newRequest)
        }
        builder.addInterceptor(tokenInterceptor)
    }
    //endregion
}