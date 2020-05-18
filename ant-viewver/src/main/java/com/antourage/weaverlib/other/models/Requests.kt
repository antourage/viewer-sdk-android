package com.antourage.weaverlib.other.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

internal data class UserRequest(
    @field:SerializedName("apiKey") val apiKey: String,
    @field:SerializedName("refKey") val refKey: String? = null,
    @field:SerializedName("displayName") val displayName: String? = null
)

internal data class UpdateDisplayNameRequest(
    @field:SerializedName("displayName") val displayName: String?
)

@Parcelize
internal data class StatisticWatchVideoRequest(
    @field:SerializedName("streamId") val streamId: Int,
    @field:SerializedName("actionId") val actionId: Int,
    @field:SerializedName("batteryLevel") val batteryLevel: Int,
    @field:SerializedName("timeStamp") val timeStamp: String,
    @field:SerializedName("span") val span: String
) : Parcelable


internal data class SubscribeToPushesRequest(
    @field:SerializedName("fcmKey") val fcmKey: String,
    @field:SerializedName("os") val os: String = "a"
)

internal data class VideoOpenedRequest(
    @field:SerializedName("vodId") val vodId: Int,
    @field:SerializedName("batteryLevel") val batteryLevel: Int,
    @field:SerializedName("timeStamp") val timeStamp: String
)

internal data class LiveOpenedRequest(
    @field:SerializedName("streamId") val streamId: Int,
    @field:SerializedName("batteryLevel") val batteryLevel: Int,
    @field:SerializedName("timeStamp") val timeStamp: String,

    //todo: should not be used according to back end, delete once corrected from BE side
    @field:SerializedName("span") val span: String = ""
)

internal data class VideoClosedRequest(
    @field:SerializedName("vodId") val vodId: Int,
    @field:SerializedName("stopTime") val stopTime: String,
    @field:SerializedName("batteryLevel") val batteryLevel: Int,
    @field:SerializedName("timeStamp") val timeStamp: String,
    @field:SerializedName("span") val span: String = ""
)

internal data class LiveClosedRequest(
    @field:SerializedName("streamId") val streamId: Int,
    @field:SerializedName("batteryLevel") val batteryLevel: Int,
    @field:SerializedName("timeStamp") val timeStamp: String,
    @field:SerializedName("span") val span: String = ""
)