package com.antourage.weaverlib.other.networking.base

import com.antourage.weaverlib.other.networking.base.ErrorUtils.parseError
import retrofit2.Response
import java.util.*

class ApiResponse<D> {

    private val data: D?

    private val error: Throwable?

    val errorMessage: String?

    val statusCode: Int?

    val isSuccessful: Boolean
        get() = data != null && error == null

    constructor(Data: D) {
        Objects.requireNonNull(Data)
        this.data = Data
        this.error = null
        this.errorMessage = null
        this.statusCode = null
    }

    constructor(response: Response<D>) {
        this.data = response.body()
        this.error = null

        //TODO 10/05/2018 handle errors differently
        if (!response.isSuccessful) {
            this.statusCode = response.code()
            this.errorMessage = parseError(response)!!.message()
        } else {
            this.statusCode = response.code()
            this.errorMessage = null
        }
    }

    constructor(error: Throwable) {
        Objects.requireNonNull(error)

        this.data = null
        this.error = error
        this.errorMessage = null
        this.statusCode = null
    }

    fun getData(): D {
        if (data == null) {
            throw IllegalStateException("Data is null")
        }
        return data
    }

    fun getError(): Throwable {
        if (error == null) {
            throw IllegalStateException("Error is null")
        }
        return error
    }

}

