package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import com.antourage.weaverlib.ModuleResourcesProvider

abstract class NetworkBoundResource<ResultType, RequestType>
@MainThread constructor() {

    private val result = MediatorLiveData<Resource<ResultType>>()

    init {
        val context = ModuleResourcesProvider.getContext()
        result.value = Resource.loading()
        val dbSource = loadFromDb()
        if (dbSource != null) {
            result.addSource(dbSource) { data ->
                result.removeSource(dbSource)
                result.value =
                    Resource.cachedData(data)
                if (shouldFetch(data)) {
                    context?.let {
                        if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                            fetchFromNetwork(dbSource)
                        } else {
                            result.value =
                                Resource.failure(
                                    "No internet"
                                )
                        }
                    }
                }
            }
        } else {
            if (shouldFetch(null)) {
                context?.let {
                    if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                        fetchFromNetwork(MutableLiveData<ResultType>())
                    } else {
                        result.value =
                            Resource.failure("No internet")
                    }
                }
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.value = newValue
        }
    }

    private fun fetchFromNetwork(dbSource: LiveData<ResultType>) {
        val apiResponse = createCall()
        // we re-attach dbSource as a new source, it will dispatch its latest value quickly
        result.addSource(dbSource) {
            setValue(Resource.loading())
        }
        result.addSource(apiResponse) { response ->
            result.removeSource(apiResponse)
            result.removeSource(dbSource)
            when (response) {
                is ApiSuccessResponse -> {
                    AppExecutors.diskIO().execute {
                        saveCallResult(processResponse(response))
                        AppExecutors.mainThread().execute {
                            setValue(
                                Resource.success(
                                    convertResponse(response)
                                )
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
                    result.addSource(dbSource) {
                        setValue(
                            Resource.failure(
                                response.errorMessage,
                                response.errorCode
                            )
                        )
                    }
                }
            }
        }
    }

    protected open fun onFetchFailed() {}

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<RequestType>) = response.body

    @WorkerThread
    protected open fun convertResponse(response: ApiSuccessResponse<RequestType>) =
        response.body as ResultType

    @WorkerThread
    protected open fun saveCallResult(item: RequestType) {
    }

    @MainThread
    protected open fun shouldFetch(data: ResultType?): Boolean = true

    @MainThread
    protected open fun loadFromDb(): LiveData<ResultType>? = null

    @MainThread
    protected abstract fun createCall(): LiveData<ApiResponse<RequestType>>
}