package com.antourage.weaverlib.other.networking.profile
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import com.antourage.weaverlib.other.networking.auth.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


internal object ProfileClient {
    var BASE_URL = ""
    private const val VERSION_SUFFIX = "api/v1/"

    lateinit var profileService: ProfileService
    private var retrofit: Retrofit? = null
    private var httpClient: OkHttpClient? = null

    fun getWebClient(): ProfileClient {
        if (retrofit == null || (retrofit?.baseUrl().toString() != BASE_URL + VERSION_SUFFIX)) {
            buildRetrofit()
        }
        return this
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
        profileService = retrofit?.create(ProfileService::class.java)!!
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