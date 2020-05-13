package com.antourage.weaverlib.other.networking

import com.antourage.weaverlib.UserCache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


internal object ApiClient {

    var BASE_URL = ""
    private const val HEADER_TOKEN = "Authorization"
    private const val HEADER_LANGUAGE = "Accept-Language"
    private const val VERSION_SUFFIX = "api/v1/widget/"
//    private const val VERSION_SUFFIX = "api/v1/"
    private const val VERSION_2_SUFFIX = "api/v2/widget/"
//    private const val VERSION_2_SUFFIX = "api/v2/"

    lateinit var webService: WebService
    private var retrofit: Retrofit? = null
    private var usingAuth = false

    private var shouldRebuild = false

    fun getWebClient(useAuth: Boolean = true, v2: Boolean = false): ApiClient {
        if (retrofit == null
            || usingAuth != useAuth
            || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)
            || v2
            || shouldRebuild
        ) {
            rebuildRetrofit(useAuth, v2)
        }
        return this
    }

    //region Private

    private fun getTokenForRequest(): String {
        return UserCache.getInstance()?.let { userCache ->
            userCache.getToken() ?: ""
        } ?: ""
    }

    private fun rebuildRetrofit(useAuth: Boolean, v2: Boolean = false) {
        val client = buildOkHttpClient(useAuth)
        retrofit = Retrofit.Builder()
            .baseUrl(if (v2){ (BASE_URL + VERSION_2_SUFFIX)} else (BASE_URL + VERSION_SUFFIX) )
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        webService = retrofit?.create(WebService::class.java)!!
        if(v2) shouldRebuild = true
    }

    private fun buildOkHttpClient(useAuth: Boolean): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)

        usingAuth = useAuth
        val builder = OkHttpClient.Builder()
        if (useAuth) {
            addTokenInterceptor(builder)
        } else {
            addDefaultInterceptor(builder)
        }
        builder
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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
                .addHeader(HEADER_TOKEN, "Bearer ${getTokenForRequest()}")
                .addHeader(HEADER_LANGUAGE, "en")
                .build()
            chain.proceed(newRequest)
        }
        builder.addInterceptor(tokenInterceptor)
    }
    //endregion
}