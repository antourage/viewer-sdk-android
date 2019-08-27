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
    @field:SerializedName("id") val streamId: Int,
    @field:SerializedName("name") val streamTitle: String = "",
    @field:SerializedName("creatorFullName") val creatorFullName: String = "",
    @field:SerializedName("creatorNickname") var creatorNickname: String = "",
    @field:SerializedName("thumbnailUrl") val thumbnailUrl: String = "",
    @field:SerializedName("startTime") val startTime: String = "",
    @field:SerializedName("url") val hlsUrl: Array<String> = arrayOf(),
    val duration: Int = -1,
    var isLive: Boolean = false,
    var viewerCounter: Int = 6385
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StreamResponse

        if (streamId != other.streamId) return false
        if (streamTitle != other.streamTitle) return false
        if (creatorFullName != other.creatorFullName) return false
        if (creatorNickname != other.creatorNickname) return false
        if (thumbnailUrl != other.thumbnailUrl) return false
        if (startTime != other.startTime) return false
        if (!hlsUrl.contentEquals(other.hlsUrl)) return false
        if (duration != other.duration) return false
        if (isLive != other.isLive) return false
        if (viewerCounter != other.viewerCounter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = streamId
        result = 31 * result + streamTitle.hashCode()
        result = 31 * result + creatorFullName.hashCode()
        result = 31 * result + creatorNickname.hashCode()
        result = 31 * result + thumbnailUrl.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + hlsUrl.contentHashCode()
        result = 31 * result + duration
        result = 31 * result + isLive.hashCode()
        result = 31 * result + viewerCounter
        return result
    }
}