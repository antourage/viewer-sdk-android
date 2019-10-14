package com.antourage.weaverlib.other.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class UserRequest(
    @field:SerializedName("apiKey") val apiKey: String?,
    @field:SerializedName("refKey") val refKey: String? = null,
    @field:SerializedName("displayName") val displayName: String? = null
)

data class UpdateDisplayNameRequest(
    @field:SerializedName("displayName") val displayName: String?
)

@Parcelize
data class StatisticWatchVideoRequest(
    @field:SerializedName("streamId") val streamId: Int?,
    @field:SerializedName("actionId") val actionId: Int?,
    @field:SerializedName("batteryLevel") val batteryLevel: Int?,
    @field:SerializedName("timeStamp") val timeStamp: String?,
    @field:SerializedName("span") val span: String?
) : Parcelable

data class StopWatchVodRequest(
    @field:SerializedName("vodId") val vodId: Int,
    @field:SerializedName("stopTime") val stopTime: String?
)