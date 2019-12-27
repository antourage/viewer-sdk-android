package com.antourage.weaverlib.other.networking

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Intent
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import android.support.v4.content.LocalBroadcastManager
import com.antourage.weaverlib.ModuleResourcesProvider
import com.antourage.weaverlib.R
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
//        Timer().schedule(1000) {
//            if ((end ?: 0) - (start ?: 0) > 1000)
//                result.value = Resource.loading()
//        }
        result.value = Resource.loading()
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                fetchFromNetwork()
            } else {
                val intent =
                    Intent(context.resources.getString(com.antourage.weaverlib.R.string.ant_no_internet_action))
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
                result.value =
                    Resource.failure(context.resources.getString(com.antourage.weaverlib.R.string.ant_no_internet))
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