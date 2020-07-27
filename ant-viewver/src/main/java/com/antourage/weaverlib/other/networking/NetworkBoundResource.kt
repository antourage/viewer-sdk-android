package com.antourage.weaverlib.other.networking

import android.content.Intent
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.antourage.weaverlib.ModuleResourcesProvider

internal abstract class NetworkBoundResource<ResultType>
@MainThread constructor() {

    private val result = MutableLiveData<Resource<ResultType>>()

    init {
        val context = ModuleResourcesProvider.getContext()
        result.value = Resource.loading()
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                fetchFromNetwork()
            } else {
                val intent =
                    Intent(context.resources.getString(com.antourage.weaverlib.R.string.ant_no_internet_action))
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                result.value =
                    Resource.failure(context.resources.getString(com.antourage.weaverlib.R.string.ant_no_internet_action))
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
                        //TODO: handle empty response in scope of error handling implementation
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