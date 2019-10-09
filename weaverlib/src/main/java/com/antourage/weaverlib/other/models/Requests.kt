package com.antourage.weaverlib.other.models

import com.google.gson.annotations.SerializedName

data class UserRequest(
    @field:SerializedName("apiKey") val apiKey: String?,
    @field:SerializedName("refKey") val refKey: String?
)

data class UpdateDisplayNameRequest(
    @field:SerializedName("displayName") val displayName: String?
)