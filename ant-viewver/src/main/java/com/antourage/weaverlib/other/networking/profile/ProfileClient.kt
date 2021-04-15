package com.antourage.weaverlib.other.networking.profile
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


internal object ProfileClient {
    var BASE_URL = ""
    private const val VERSION_SUFFIX = "api/v1/"

    lateinit var profileService: ProfileService
    private var retrofit: Retrofit? = null

    fun getWebClient(): ProfileClient {
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
        profileService = retrofit?.create(ProfileService::class.java)!!
    }

    //endregion
}