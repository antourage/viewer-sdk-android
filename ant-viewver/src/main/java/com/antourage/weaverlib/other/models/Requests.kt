package com.antourage.weaverlib.other.models

import com.google.gson.annotations.SerializedName

internal data class SubscribeToPushesRequest(
    @field:SerializedName("fcmKey") val fcmKey: String,
    @field:SerializedName("teamId") val teamId: Int,
    @field:SerializedName("os") val os: String = "a"
)
