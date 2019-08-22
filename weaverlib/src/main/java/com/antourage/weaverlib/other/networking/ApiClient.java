package com.antourage.weaverlib.other.networking;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static String BASE_URL = "";
    private static final String HEADER_LANGUAGE = "Accept-Language";

    private static WebService webService;
    private static Retrofit retrofit;
    private static ApiClient apiClient;

    private ApiClient() {
        OkHttpClient client = defaultClient();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + "/api/v1/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(new LiveDataCallAdapterFactory())
                .build();
        webService = retrofit.create(WebService.class);
    }

    public static ApiClient getClient() {
        if (apiClient == null) {
            apiClient = new ApiClient();
        }
        return apiClient;
    }

    public static Retrofit getRetrofitInstance() {
        return retrofit;
    }

    public WebService getWebService() {
        return webService;
    }

    private OkHttpClient defaultClient() {
        return new OkHttpClient.Builder().addInterceptor(chain -> {
            Request request = chain.request().newBuilder()
                    .addHeader(HEADER_LANGUAGE, "en")
                    .build();
            return chain.proceed(request);
        }).build();
    }
}