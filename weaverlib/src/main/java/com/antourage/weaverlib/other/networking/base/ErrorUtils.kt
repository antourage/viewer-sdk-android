package com.antourage.weaverlib.other.networking.base

import com.antourage.weaverlib.other.models.APIError
import com.antourage.weaverlib.other.networking.ApiClient
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Response

import java.io.IOException

object ErrorUtils {

    fun parseError(response: Response<*>): APIError? {
        val converter = ApiClient.getRetrofitInstance()
            .responseBodyConverter<APIError>(APIError::class.java, arrayOfNulls(0))

        val error: APIError?

        try {
            error = converter.convert(response.errorBody()!!)
        } catch (e: IOException) {
            return APIError()
        }

        return error
    }
}
