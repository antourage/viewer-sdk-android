package com.antourage.weaverlib.other.networking.auth

import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.networking.ApiClient
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Response
import java.io.IOException

class AuthInterceptor: Interceptor {
    companion object {
        private const val HEADER_TOKEN = "Authorization"
        private const val HEADER_LANGUAGE = "Accept-Language"
        private const val HEADER_DEVICE_ID = "antourage-deviceId"
    }

    @Throws(IOException::class)
    override fun intercept(chain: Chain): Response {
        var request = chain.request()

        val builder = request.newBuilder()
            .addHeader(HEADER_LANGUAGE, "en")
            .addHeader(HEADER_DEVICE_ID, "D12BEB59-6259-4FA1-A733-ADCD523D72DC") // TODO T: change to real device id

        val accessToken = UserCache.getInstance()?.getAccessToken()
        if (accessToken != null) {
            builder.addHeader(HEADER_TOKEN, "Bearer ${accessToken}")
        }

        request = builder.build()

        val response = chain.proceed(request)

        if (response.code == 401) {
            synchronized(ApiClient.getHttpClient()) {
                val currentAccessToken = UserCache.getInstance()?.getAccessToken()
                if(currentAccessToken == null || currentAccessToken == accessToken) {
                    val code: Int = AuthClient.getAuthClient().authenticateUser().code() / 100
                    if (code != 2) {
                        return response
                    }
                }

                UserCache.getInstance()?.getAccessToken()?.let {
                    builder.header(HEADER_TOKEN, "Bearer ${it}")
                    request = builder.build()
                    return chain.proceed(request)
                }
            }
        }
        return response
    }
}