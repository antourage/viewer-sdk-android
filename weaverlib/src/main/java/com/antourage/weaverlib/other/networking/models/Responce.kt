package com.antourage.weaverlib.other.networking.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


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
