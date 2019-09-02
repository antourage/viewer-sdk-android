package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import com.antourage.weaverlib.ModuleResourcesProvider

abstract class NetworkBoundResource<ResultType>
@MainThread constructor() {

    private val result = MutableLiveData<Resource<ResultType>>()

    init {
        val context = ModuleResourcesProvider.getContext()
        result.value = Resource.loading()
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                fetchFromNetwork()
            } else {
                result.value =
                    Resource.failure("No internet")
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    private fun fetchFromNetwork() {
        val apiResponse = createCall()
        apiResponse.observeForever { response ->
            when (response) {
                is ApiSuccessResponse -> {
                    AppExecutors.diskIO().execute {
                        AppExecutors.mainThread().execute {
                            result.setValue(
                                Resource.success(response.body)
                            )
                        }
                    }
                }
                is ApiEmptyResponse -> {
                    AppExecutors.mainThread().execute {
                        //Got empty response
                    }
                }
                is ApiErrorResponse -> {
                    onFetchFailed()
                    result.setValue(
                        Resource.failure(
                            response.errorMessage,
                            response.errorCode
                        )
                    )
                }
            }
        }
    }

    protected open fun onFetchFailed() {}

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<ResultType>) = response.body

    @MainThread
    protected abstract fun createCall(): LiveData<ApiResponse<ResultType>>
}