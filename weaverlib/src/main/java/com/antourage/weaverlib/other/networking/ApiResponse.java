package com.antourage.weaverlib.other.networking;

import com.antourage.weaverlib.other.models.APIError;
import retrofit2.Response;

import java.util.Objects;

public final class ApiResponse<D> {

    private final D data;

    private final Throwable error;

    private final String errorMessage;

    private final Integer statusCode;

    public ApiResponse(final D Data) {
        Objects.requireNonNull(Data);
        this.data = Data;
        this.error = null;
        this.errorMessage = null;
        this.statusCode = null;
    }

    public ApiResponse(Response<D> response) {
        this.data = response.body();
        this.error = null;
        if (!response.isSuccessful()) {
            APIError apiError = ErrorUtils.parseError(response);
            if (apiError != null) {
                this.statusCode = apiError.statusCode();
                //TODO 5/2/2019 handle error codes
                //if (AntourageApplication.getResourcesStatic().getStringArray(R.array.error_codes).length >= apiError.statusCode()) {

                //    this.errorMessage =
                //            AntourageApplication.getResourcesStatic().getStringArray(R.array.error_codes)[apiError.statusCode()];
                //} else {
                    this.errorMessage = response.code() + " " + response.message();
                //}
            } else {
                this.statusCode = response.code();
                this.errorMessage = response.code() + " " + response.message();
            }
        } else {
            this.statusCode = response.code();
            this.errorMessage = null;
        }
    }

    public ApiResponse(final Throwable error) {
        Objects.requireNonNull(error);

        this.data = null;
        this.error = error;
        this.errorMessage = null;
        this.statusCode = null;
    }

    boolean isSuccessful() {
        return data != null && error == null;
    }

    public D getData() {
        if (data == null) {
            throw new IllegalStateException("Data is null");
        }
        return data;
    }

    public Throwable getError() {
        if (error == null) {
            throw new IllegalStateException("Error is null");
        }
        return error;
    }

    String getErrorMessage() {
        return errorMessage;
    }

    Integer getStatusCode() {
        return statusCode;
    }
}

