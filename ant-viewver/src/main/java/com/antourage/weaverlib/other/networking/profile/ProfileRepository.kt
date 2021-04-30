package com.antourage.weaverlib.other.networking.profile

import androidx.lifecycle.LiveData
import com.antourage.weaverlib.other.models.ProfileResponse
import com.antourage.weaverlib.other.networking.NetworkBoundResource
import com.antourage.weaverlib.other.networking.Resource

internal class ProfileRepository {
    companion object {
        fun getProfile(): LiveData<Resource<ProfileResponse>> =
            object : NetworkBoundResource<ProfileResponse>() {
                override fun createCall() = ProfileClient.getWebClient().profileService.getProfile()
            }.asLiveData()
    }
}