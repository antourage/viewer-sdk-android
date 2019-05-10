package com.antourage.weaverlib.other.networking.base

//a generic class that describes a data with a state
class Resource<T> private constructor(val state: State, val data: T?, val message: String?, val statusCode: Int?) {
    companion object {

        fun <T> success(data: T): Resource<T> {
            return Resource(
                State.SUCCESS,
                data,
                null,
                null
            )
        }

        fun <T> error(msg: String?, data: T?, statusCode: Int?): Resource<T> {
            return Resource(
                State.FAILURE,
                data,
                msg,
                statusCode
            )
        }

        fun <T> loading(data: T?): Resource<T> {
            return Resource(
                State.LOADING,
                data,
                null,
                null
            )
        }
    }
}
