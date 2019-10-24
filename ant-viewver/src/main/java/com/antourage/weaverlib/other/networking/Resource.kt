package com.antourage.weaverlib.other.networking

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
</T> */
internal data class Resource<T>(val status: Status<T>) {
    companion object {
        fun <T> success(data: T?): Resource<T> {
            return Resource(
                Status.Success(
                    data
                )
            )
        }

        fun <T> cachedData(data: T?): Resource<T> {
            return Resource(
                Status.CachedData(
                    data
                )
            )
        }

        fun <T> failure(msg: String, errorCode: Int? = null): Resource<T> {
            return Resource(
                Status.Failure(
                    msg,
                    errorCode
                )
            )
        }

        fun <T> loading(): Resource<T> {
            return Resource(Status.Loading())
        }
    }
}
