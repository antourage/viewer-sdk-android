package com.antourage.weaverlib.networking.localisation

internal class LocalisationRepository {
    companion object {
        suspend fun getLocalisationJsonFile(localisation: String): okhttp3.ResponseBody? =
                    LocalisationClient.getWebClient().localisationService.getLocalisationJsonFile(localisation)
    }
}