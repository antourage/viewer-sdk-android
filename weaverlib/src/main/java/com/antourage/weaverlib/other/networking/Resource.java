package com.antourage.weaverlib.other.networking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

//a generic class that describes a data with a state
public class Resource<T> {
    @NonNull
    public final State state;
    @Nullable
    public final T data;
    @Nullable
    public final String message;
    @Nullable
    public final Integer statusCode;

    private Resource(@NonNull State state, @Nullable T data, @Nullable String message, @Nullable Integer statusCode) {
        this.state = state;
        this.data = data;
        this.message = message;
        this.statusCode = statusCode;
    }

    public static <T> Resource<T> success(@NonNull T data) {
        return new Resource<>(State.SUCCESS, data, null, null);
    }

    public static <T> Resource<T> error(String msg, @Nullable T data,@Nullable Integer statusCode) {
        return new Resource<>(State.FAILURE, data, msg, statusCode);
    }

    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(State.LOADING, data, null, null);
    }
}
