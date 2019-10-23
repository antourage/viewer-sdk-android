package com.antourage.weaverlib.other.networking

internal sealed class Status<T> {
    class Loading<T> : Status<T>()
    data class Failure<T>(val errorMessage: String, val errorCode: Int?) : Status<T>()
    data class CachedData<T>(val data: T?) : Status<T>()
    data class Success<T>(val data: T?) : Status<T>()
}
