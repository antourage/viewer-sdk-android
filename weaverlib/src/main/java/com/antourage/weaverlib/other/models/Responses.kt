package com.antourage.weaverlib.other.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

class APIError {
    @SerializedName("ErrorCode")
    @Expose
    private val statusCode: Int = 0
    @SerializedName("Error")
    @Expose
    private val message: String? = null

    fun statusCode(): Int {
        return statusCode
    }

    fun message(): String? {
        return message
    }
}

class SimpleResponse {
    @SerializedName("error")
    @Expose
    var error: String? = null
    @SerializedName("success")
    @Expose
    var success: Boolean? = null
}

@Parcelize
data class StreamResponse(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("streamId") val streamId: Int?,
    @field:SerializedName("name") val streamTitle: String?,
    @field:SerializedName("videoName") val videoName: String?,
    @field:SerializedName("videoURL") val videoURL: String?,
    @field:SerializedName("expirationDate") val expirationDate: String?,
    @field:SerializedName("creatorFullName") val creatorFullName: String?,
    @field:SerializedName("creatorNickname") var creatorNickname: String?,
    @field:SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @field:SerializedName("startTime") val startTime: String?,
    @field:SerializedName("url") val hlsUrl: Array<String>?,
    @field:SerializedName("teamId") val teamId: Int?,
    @field:SerializedName("organisationId") val organisationId: Int?,
    @field:SerializedName("channelId") val channelId: Int?,
    @field:SerializedName("duration") val duration: String?,
    @field:SerializedName("broadcasterPicUrl") val broadcasterPicUrl: String?,
    var isLive: Boolean = false,
    var viewerCounter: Int = 6385
) : Parcelable

data class User(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("token") val token: String?,
    @field:SerializedName("displayName") val displayName: String?
)

