package com.antourage.weaverlib.other.models

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

internal class APIError {
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

open class SimpleResponse {
    @SerializedName("error")
    @Expose
    var error: String? = null

    @SerializedName("success")
    @Expose
    var success: Boolean? = null
}

class NotificationSubscriptionResponse : SimpleResponse() {
    @SerializedName("topic")
    @Expose
    var topic: String? = null
}

data class LiveUpdatedResponse(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("viewerCount") val viewerCount: Long?
)

data class WebViewResponse(
    @SerializedName("type") var type : String,
    @SerializedName("message") var messageCode : String?
)

class ListOfStreams : ArrayList<StreamResponse>()

@Parcelize
data class StreamResponse(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("name") val streamTitle: String?,
    @field:SerializedName("videoName") val videoName: String?,
    @field:SerializedName("videoURL") val videoURL: String?,
    @field:SerializedName("expirationDate") val expirationDate: String?,
    @field:SerializedName("creatorFullName") val creatorFullName: String?,
    @field:SerializedName("creatorNickname") var creatorNickname: String?,
    @field:SerializedName("images") val images: List<String>?,
    @field:SerializedName("thumbnailUrl") val thumbnailUrl: String?,
    @field:SerializedName("startTime") val startTime: String?,
    @field:SerializedName("publishDate") val publishDate: String?,
    @field:SerializedName("url") val hlsUrl: String?,
    @field:SerializedName("teamId") val teamId: Int?,
    @field:SerializedName("organisationId") val organisationId: Int?,
    @field:SerializedName("channelId") val channelId: Int?,
    @field:SerializedName("duration") val duration: String?,
    @field:SerializedName("type") val type: StreamResponseType?,
    @field:SerializedName("creatorImageUrl") val creatorImageUrl: String?,
    @field:SerializedName("viewsCount") var viewsCount: Long?,
    @field:SerializedName("curtainRangeModels") var curtainRangeModels: List<CurtainRange>?,
    var isLive: Boolean = false,
    @field:SerializedName("viewersCount") var viewersCount: Long?,
    var isNew: Boolean = true,
    var stopTimeMillis: Long = 0,
    var lastMessage: String? = null,
    var lastMessageAuthor: String? = null,
    var isChatEnabled: Boolean? = null,
    var arePollsEnabled: Boolean? = null
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StreamResponse

        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id ?: 0
    }
}

enum class StreamResponseType() {
    @SerializedName("liveStream")
    VOD,
    @SerializedName("post")
    POST,
    @SerializedName("video")
    UPLOADED_VIDEO
}

data class ProfileResponse(
    @field:SerializedName("identityId") val id: String?,
    @field:SerializedName("imageUrl") val imageUrl: String?,
    @field:SerializedName("email") val email: String?,
    @field:SerializedName("nickname") val nickname: String?
)

data class User(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("token") val token: String?,
    @field:SerializedName("displayName") var displayName: String?,
    @field:SerializedName("imageUrl") var imageUrl: String?
)

@Parcelize
data class CurtainRange(
    @field:SerializedName("start") val start: String?,
    @field:SerializedName("end") val end: String?
) : Parcelable

@Parcelize
data class CurtainRangeMillis(
    @field:SerializedName("start") val start: Long,
    @field:SerializedName("end") val end: Long
) : Parcelable

data class UpdateImageResponse(
    @field:SerializedName("imageUrl") val imageUrl: String?,
    @field:SerializedName("success") val success: Boolean?
)

data class Viewers(
    @field:SerializedName("liveStreamId") val liveStreamId: Int,
    @field:SerializedName("viewers") val viewers: Long
)

data class FeedInfo(
    @field:SerializedName("imageUrl") val imageUrl: String?,
    @field:SerializedName("tagLine") val tagLine: String?
)

@Parcelize
data class AdBanner(
    @field:SerializedName("imageUrl") val imageUrl: String?,
    @field:SerializedName("externalUrl") val externalUrl: String?
) : Parcelable

