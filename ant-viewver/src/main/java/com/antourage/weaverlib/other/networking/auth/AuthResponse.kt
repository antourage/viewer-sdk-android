package com.antourage.weaverlib.other.networking.auth

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @field:SerializedName("access_token") val accessToken: String?,
    @field:SerializedName("error") val error: String?,
    @field:SerializedName("id_token") val idToken: String?
)