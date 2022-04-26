package com.antourage.weaverlib.networking.localisation

import retrofit2.http.GET
import retrofit2.http.Path

internal interface LocalisationService {
    @GET("{locale}/web/{locale}.json")
    suspend fun getLocalisationJsonFile(@Path ("locale") localisation: String): okhttp3.ResponseBody?
}