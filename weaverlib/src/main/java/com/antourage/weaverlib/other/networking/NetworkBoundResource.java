package com.antourage.weaverlib.other.networking;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

public abstract class NetworkBoundResource<ResultType> {

    private MutableLiveData<Resource<ResultType>> result = new MutableLiveData<>();

    @MainThread
    public NetworkBoundResource() {
        //TODO 5/2/2019 ckeck network
        //if (AntourageApplication.isNetworkAvailable()) {
        result.postValue(Resource.loading(null));
        fetchFromNetwork();
        //} else {
        //    result.postValue(Resource.error(AntourageApplication.getResourcesStatic().getString(R.string.no_internet), null, null));
        //}
    }

    private void fetchFromNetwork() {
        LiveData<ApiResponse<ResultType>> apiResponse = createCall();
        apiResponse.observeForever(new Observer<ApiResponse<ResultType>>() {
            @Override
            public void onChanged(@Nullable ApiResponse<ResultType> resultTypeApiResponse) {
                if (resultTypeApiResponse != null && resultTypeApiResponse.isSuccessful()) {
                    AppExecutors.diskIO().execute(() -> {
                        saveCallResult(processResponse(resultTypeApiResponse));
                        AppExecutors.mainThread().execute(() ->
                                result.setValue(Resource.success(resultTypeApiResponse.getData())));
                    });
                } else {
                    if (resultTypeApiResponse != null && resultTypeApiResponse.getErrorMessage() != null) {
                        result.setValue(Resource.error(resultTypeApiResponse.getErrorMessage(), null, resultTypeApiResponse.getStatusCode()));
                    } else if (resultTypeApiResponse != null) {
                        result.setValue(Resource.error(resultTypeApiResponse.getError().getMessage(), null, resultTypeApiResponse.getStatusCode()));
                    }
                }
                apiResponse.removeObserver(this);
            }
        });
    }

    public LiveData<Resource<ResultType>> asLiveData() {
        return result;
    }

    @WorkerThread
    private ResultType processResponse(ApiResponse<ResultType> response) {
        return response.getData();
    }

    @WorkerThread
    protected abstract void saveCallResult(@NonNull ResultType item);

    @MainThread
    protected abstract LiveData<ApiResponse<ResultType>> createCall();
}
