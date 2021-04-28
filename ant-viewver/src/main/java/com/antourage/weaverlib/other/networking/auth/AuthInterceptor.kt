package com.antourage.weaverlib.other.networking.auth

import android.content.res.Resources
import android.os.Build
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.ApiClient
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import java.io.IOException
import java.util.*

class AuthInterceptor : Interceptor {
    companion object {
        private const val HEADER_TOKEN = "Authorization"
        private const val HEADER_LANGUAGE = "Accept-Language"
        private const val HEADER_LOCALIZATION = "antourage-localization"
        private const val HEADER_OS = "antourage-widget"
        private const val HEADER_VERSION = "antourage-widgetVersion"
        private const val HEADER_OS_VERSION = "antourage-platform"
        private const val HEADER_DEVICE_ID = "antourage-deviceId"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
        var request = chain.request()
        val builder = request.newBuilder()
            .addHeader(HEADER_LANGUAGE, "en")
            .addHeader(HEADER_DEVICE_ID, UserCache.getInstance()?.getDeviceId().toString())

        val idToken = UserCache.getInstance()?.getIdToken()
        val accessToken = UserCache.getInstance()?.getAccessToken()
        if (idToken != null) {
            builder.addHeader(HEADER_TOKEN, "Bearer $idToken")
        } else if (accessToken != null) {
            builder.addHeader(HEADER_TOKEN, "Bearer $accessToken")
        }

        if (request.url.toString().contains("/open") || request.url.toString().contains("/close")) {
            builder.addHeader(HEADER_OS, "android")
            builder.addHeader(HEADER_VERSION, BuildConfig.VERSION_NAME)
            builder.addHeader(HEADER_OS_VERSION, Build.VERSION.RELEASE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.addHeader(
                    HEADER_LOCALIZATION,
                    Resources.getSystem().configuration.locales[0].language
                )
            } else {
                builder.addHeader(HEADER_LOCALIZATION, Locale.getDefault().language)

            }
        }

        request = builder.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            synchronized(ApiClient.getHttpClient()) {
                val currentIdToken = UserCache.getInstance()?.getIdToken()
                val currentAccessToken = UserCache.getInstance()?.getAccessToken()
                if ((currentIdToken != null && currentIdToken == idToken) || currentAccessToken == null || currentAccessToken == accessToken) {
                    val code: Int = AuthClient.getAuthClient().authenticateUser().code() / 100
                    if (code != 2) {
                        return response
                    }
                }

                if (UserCache.getInstance()?.getIdToken() != null) {
                    builder.header(HEADER_TOKEN, "Bearer ${UserCache.getInstance()?.getIdToken()}")
                } else if (UserCache.getInstance()?.getAccessToken() != null) {
                    builder.header(
                        HEADER_TOKEN,
                        "Bearer ${UserCache.getInstance()?.getAccessToken()}"
                    )
                }
                request = builder.build()
                return chain.proceed(request)
            }
        }
        return response
    }
}