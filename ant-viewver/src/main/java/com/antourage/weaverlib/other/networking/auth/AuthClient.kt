package com.antourage.weaverlib.other.networking.auth

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.antourage.weaverlib.PropertyManager
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.ApiClient
import com.antourage.weaverlib.other.networking.LiveDataCallAdapterFactory
import com.antourage.weaverlib.screens.list.dev_settings.DevSettingsDialog
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal object AuthClient {

    private val propertyHelper = PropertyManager.getInstance()

    val CLIENT_ID = propertyHelper?.getProperty(PropertyManager.CLIENT_ID)
    val ANONYMOUS_CLIENT_ID = propertyHelper?.getProperty(PropertyManager.ANONYMOUS_CLIENT_ID)
    val ANONYMOUS_SECRET = propertyHelper?.getProperty(PropertyManager.ANONYMOUS_SECRET)

    private var BASE_URL = when(ApiClient.BASE_URL){
        DevSettingsDialog.BASE_URL_DEV -> propertyHelper?.getProperty(PropertyManager.COGNITO_URL_DEV)
        DevSettingsDialog.BASE_URL_LOAD -> propertyHelper?.getProperty(PropertyManager.COGNITO_URL_LOAD)
        DevSettingsDialog.BASE_URL_STAGING -> propertyHelper?.getProperty(PropertyManager.COGNITO_URL_STAGING)
        DevSettingsDialog.BASE_URL_PROD -> propertyHelper?.getProperty(PropertyManager.COGNITO_URL_PROD)
        else -> propertyHelper?.getProperty(PropertyManager.COGNITO_URL_PROD)
    }

    internal const val TAG = "AntourageAuthClientLogs"

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

            val refreshTokenResponse = getAuthClient().authService.refreshToken(CLIENT_ID!!, refreshToken).execute()

            if (refreshTokenResponse.code() != 401) {
                refreshTokenResponse.body()?.accessToken?.let {
                    UserCache.getInstance()?.saveAccessToken(it)
                }
                refreshTokenResponse.body()?.idToken?.let {
                    Log.d(TAG, "Successfully refreshed token")
                    UserCache.getInstance()?.saveIdToken(it)
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

    internal fun handleSignIn(data: Uri) {
            val accessToken = data.toString().substringAfter("token=").substringBefore("&idToken")
            val idToken = data.toString().substringAfter("idToken=").substringBefore("&refreshToken")
            val refreshToken = data.toString().substringAfter("&refreshToken=")

            if(accessToken.isNotBlank() && idToken.isNotBlank() && refreshToken.isNotBlank()){
                Log.d(TAG, "Saving new tokens" )
                UserCache.getInstance()?.saveRefreshToken(refreshToken)
                UserCache.getInstance()?.saveIdToken(idToken)
                UserCache.getInstance()?.saveAccessToken(accessToken)
            }
        }
}