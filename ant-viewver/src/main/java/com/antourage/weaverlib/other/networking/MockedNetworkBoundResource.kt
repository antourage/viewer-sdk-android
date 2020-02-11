package com.antourage.weaverlib.other.networking

import android.content.Intent
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.antourage.weaverlib.ModuleResourcesProvider
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

internal abstract class MockedNetworkBoundResource<ResultType>
@MainThread constructor(mockResponse: ResultType) {

    private val result = MutableLiveData<Resource<ResultType>>()

    init {
        val context = ModuleResourcesProvider.getContext()
        result.value = Resource.loading()
        context?.let {
            if (ConnectionStateMonitor.isNetworkAvailable(it)) {
                fetchFromNetwork(mockResponse)
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
            result.value = newValue
        }
    }

    private fun fetchFromNetwork(mockResponse: ResultType) {
        doAsync {
            SystemClock.sleep(2000)
            uiThread {
                result.value = Resource.success(mockResponse)
            }
        }
    }

    protected open fun onFetchFailed() {}

    fun asLiveData() = result as LiveData<Resource<ResultType>>

    @WorkerThread
    protected open fun processResponse(response: ApiSuccessResponse<ResultType>) = response.body
}