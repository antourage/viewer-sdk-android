package com.antourage.weaverlib.other.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

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

data class PortalStateSocketResponse(
    @SerializedName("data") var portalState: PortalStateResponse?
)


data class PortalStateResponse(
    @SerializedName("config") var config: WebConfig?,
    @SerializedName("team_domain") var fallbackUrl: String?,
    @SerializedName("item") var item: PortalState? = PortalState()
)

data class PortalState(
    @SerializedName("content_id") var contentId: Int? = null,
    @SerializedName("live") var live: Boolean? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("thumbnail_url") var thumbnailUrl: String? = null,
    @SerializedName("asset_type") var assetType: String? = null,
    @SerializedName("asset_url") var assetUrl: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("cta_label") var ctaLabel: String? = null,
    @SerializedName("cta_url") var ctaUrl: String? = null,
    @SerializedName("asset_skip_milliseconds") var curtainMilliseconds: Long? = null
)

data class PortalConfig(
    var colorWidgetBorder: Int,
    var colorCtaBg: Int,
    var colorCtaText: Int,
    var colorLive: Int,
    var colorTitleBg: Int,
    var colorNameBg: Int,
    var colorNameText: Int,
    var colorTitleText: Int
)


data class WebConfig(
    @SerializedName("color_widget_border") val colorWidgetBorder: String? = null,
    @SerializedName("color_cta_bg") val colorCtaBg: String? = null,
    @SerializedName("color_cta_text") val colorCtaText: String? = null,
    @SerializedName("color_live") val colorLive: String? = null,
    @SerializedName("color_title_bg") val colorTitleBg: String? = null,
    @SerializedName("color_name_bg") val colorNameBg: String? = null,
    @SerializedName("color_name_text") val colorNameText: String? = null,
    @SerializedName("color_title_text") val colorTitleText: String? = null
)
