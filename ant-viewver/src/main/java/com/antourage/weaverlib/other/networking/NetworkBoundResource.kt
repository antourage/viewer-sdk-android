package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import com.antourage.weaverlib.ModuleResourcesProvider
import java.util.*
import kotlin.concurrent.schedule

internal abstract class NetworkBoundResource<ResultType>
@MainThread constructor() {

    private val result = MutableLiveData<Resource<ResultType>>()
    private var start: Long? = null
    private var end: Long? = null

    init {
        val context = ModuleResourcesProvider.getContext()
        start = Date().time
        end = Date().time
        result.postValue(Resource.loading())
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                fetchFromNetwork()
            } else {
                result.postValue(Resource.failure("No internet"))
            }
        }
    }

    @MainThread
    private fun setValue(newValue: Resource<ResultType>) {
        if (result.value != newValue) {
            result.postValue(newValue)
        }
    }

    private fun fetchFromNetwork() {
        val apiResponse = createCall()
        apiResponse.observeForever { response ->
            when (response) {
                is ApiSuccessResponse -> {
                    end = Date().time
                    AppExecutors.diskIO().execute {
                        AppExecutors.mainThread().execute {
                            result.postValue(
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
                    result.postValue(
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