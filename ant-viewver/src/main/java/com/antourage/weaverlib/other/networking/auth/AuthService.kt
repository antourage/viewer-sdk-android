package com.antourage.weaverlib.other.networking.auth

import retrofit2.Call
import retrofit2.http.*

internal interface AuthService {
    @POST("oauth2/token?grant_type=client_credentials")
    @Headers("Content-Type: application/x-www-form-urlencoded", "Accept: application/json")
    fun anonymousAuth(@Header("Authorization") basicToken: String): Call<AuthResponse>

    @POST("oauth2/token?grant_type=refresh_token")
    @Headers("Content-Type: application/x-www-form-urlencoded", "Accept: application/json")
    fun refreshToken(@Query("client_id") clientID: String,
                     @Query("refresh_token") refreshToken: String): Call<AuthResponse>
}