package com.antourage.weaverlib.other.networking.auth

import android.util.Base64
import android.util.Log
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object AuthClient {
    // TODO: fetch from json
    internal const val CLIENT_ID = "5out1bdgjk0370gjnbbsoj83o0"
    internal const val ANONYMOUS_CLIENT_ID = "41qqu6923tmcmiq15grggvv839"
    internal const val ANONYMOUS_SECRET = "ZTFoOXRoM2k5dWEzY21jZXMxNmVqbHVlcTllOWE2YXNrbDVkb2xuc2F0YXBkaTU0cGky"

    var BASE_URL = "https://antourage-dev.auth.eu-central-1.amazoncognito.com"

    internal const val TAG = "AuthClientLogs"

    lateinit var authService: AuthService
    private var retrofit: Retrofit? = null

    fun getAuthClient(): AuthClient {
        if (retrofit == null || retrofit?.baseUrl().toString() != BASE_URL) {
            buildRetrofit()
        }
        return this
    }

    private fun buildRetrofit() {
        val client = buildOkHttpClient()
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .build()
        authService = retrofit?.create(AuthService::class.java)!!
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

    fun authenticateUser(): Response<AuthResponse> {
        Log.d(TAG, "Trying to authenticate user...")

        UserCache.getInstance()?.getRefreshToken()?.let { refreshToken ->
            Log.d(TAG, "Refreshing token")

            val refreshTokenResponse = getAuthClient().authService.refreshToken(CLIENT_ID, refreshToken).execute()

            if (refreshTokenResponse.code() != 401) {
                refreshTokenResponse.body()?.accessToken?.let {
                    Log.d(TAG, "Successfully refreshed token")

                    UserCache.getInstance()?.saveAccessToken(it)

                    return refreshTokenResponse
                }
            }

            Log.d(TAG, "Some error occurred")
        }

        Log.d(TAG, "Authenticating anonymously")

        val decodedSecret = String(Base64.decode(ANONYMOUS_SECRET, Base64.NO_WRAP), Charsets.UTF_8)
        val tokenBase64 = String(Base64.encode("$ANONYMOUS_CLIENT_ID:$decodedSecret".toByteArray(), Base64.NO_WRAP))
        val basicToken = "Basic $tokenBase64"

        val anonymousAuthResponse = getAuthClient().authService.anonymousAuth(basicToken).execute()

        anonymousAuthResponse.body()?.accessToken?.let {
            Log.d(TAG, "Successfully authenticated anonymously")

            UserCache.getInstance()?.saveAccessToken(it)
        }

        return anonymousAuthResponse
    }
}