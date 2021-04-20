package com.antourage.weaverlib.other.networking.profile

import com.google.gson.annotations.SerializedName

data class ProfileResponse(
    @field:SerializedName("identityId") val identityId: String?,
    @field:SerializedName("imageUrl") val imageUrl: String?,
    @field:SerializedName("email") val email: String?,
    @field:SerializedName("nickname") val nickname: String?
)