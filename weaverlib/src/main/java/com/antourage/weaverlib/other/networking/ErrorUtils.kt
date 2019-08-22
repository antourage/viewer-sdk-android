package com.antourage.weaverlib.other.networking

import com.antourage.weaverlib.other.models.APIError
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
