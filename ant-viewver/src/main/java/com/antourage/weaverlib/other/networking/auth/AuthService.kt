package com.antourage.weaverlib.other.networking.auth

import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

internal interface AuthService {
    @POST("oauth2/token?grant_type=client_credentials")
    @Headers("Content-Type: application/x-www-form-urlencoded", "Accept: application/json")
    fun anonymousAuth(@Header("Authorization") basicToken: String): Call<AuthResponse>

    @POST("oauth2/token?grant_type=refresh_token&client_id={client_id}&refresh_token={refresh_token}")
    @Headers("Content-Type: application/x-www-form-urlencoded", "Accept: application/json")
    fun refreshToken(@Path("client_id") clientID: String,
                     @Path("refresh_token") refreshToken: String): Call<AuthResponse>
}