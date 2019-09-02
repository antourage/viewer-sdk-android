package com.antourage.weaverlib.screens.list

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.Debouncer
import com.antourage.weaverlib.other.generateRandomViewerNumber
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.dev_settings.OnDevSettingsChangedListener
import javax.inject.Inject

class VideoListViewModel @Inject constructor(application: Application, val repository: Repository) :
    BaseViewModel(application), OnDevSettingsChangedListener,
    ReceivingVideosManager.ReceivingVideoCallback {
    var listOfStreams: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var liveVideos: MutableList<StreamResponse>? = null
    var vods: List<StreamResponse>? = null

    var liveVideosUpdated = false
    var vodsUpdated = false

    fun subscribeToLiveStreams() {
        ReceivingVideosManager.setReceivingVideoCallback(this)
        ReceivingVideosManager.startReceivingVideos()
        refreshVODs()
    }

    fun refreshVODs() {
        ReceivingVideosManager.loadVODs()
    }

    fun onStop() {
        showBeDialogLiveData.postValue(false)
        numberOfLogoClicks = 0
        ReceivingVideosManager.stopReceivingVideos()
    }

    override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
        when (resource.status) {
            is Status.Success -> {
                liveVideos = (resource.status.data)?.toMutableList()
                liveVideos?.let {
                    for (i in 0 until (liveVideos?.size ?: 0)) {
                        liveVideos?.get(i)?.isLive = true
                        liveVideos?.get(i)?.viewerCounter = generateRandomViewerNumber()
                    }
                    liveVideosUpdated = true
                    if (vodsUpdated) {
                        updateVideosList()
                    }
                }
            }
            is Status.Loading -> liveVideosUpdated = false
            is Status.Failure -> {
                liveVideosUpdated = true
                error.postValue(resource.status.errorMessage)
            }
        }
    }

    override fun onVODReceived(resource: Resource<List<StreamResponse>>) {
        when (resource.status) {
            is Status.Success -> {
                vods = (resource.status.data)?.toMutableList()
                vods?.let {
                    for (i in 0 until (vods?.size ?: 0)) {
                        vods?.get(i)?.viewerCounter = generateRandomViewerNumber()
                    }
                    vodsUpdated = true
                    if (liveVideosUpdated) {
                        updateVideosList()
                    }
                }
            }
            is Status.Loading -> vodsUpdated = false
            is Status.Failure -> {
                vodsUpdated = true
                error.postValue(resource.status.errorMessage)
            }
        }
    }

    private fun updateVideosList() {
        val resultList = mutableListOf<StreamResponse>()
        liveVideos?.let { resultList.addAll(it) }
        if (resultList.size > 0) {
            Log.d("VOD_TEST", "${liveVideos?.size} > 0, adding separator")
            resultList.add(StreamResponse(-1))
        }
        vods?.let { resultList.addAll(it) }
        listOfStreams.postValue(resultList)
    }

    //region backend choice
    companion object {
        private const val BE_CHOICE_TIMEOUT = 4000L
        private const val BE_CHOICE_CLICKS = 4
    }

    private val showBeDialogLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var numberOfLogoClicks: Int = 0
    private val beDebouncer: Debouncer =
        Debouncer(Runnable { numberOfLogoClicks = 0 }, BE_CHOICE_TIMEOUT)

    init {
        showBeDialogLiveData.postValue(false)
    }

    fun onLogoPressed() {
        if (numberOfLogoClicks >= BE_CHOICE_CLICKS) {
            showBeDialogLiveData.value = true
            numberOfLogoClicks = 0
            beDebouncer.cancel()
        } else {
            numberOfLogoClicks++
            if (numberOfLogoClicks == 1) {
                showBeDialogLiveData.postValue(false)
                beDebouncer.run()
            }
        }
    }

    fun getShowBeDialog() = showBeDialogLiveData as LiveData<Boolean>

    override fun onBeChanged(choice: String?) {
        choice?.let {
            UserCache.newInstance()
                .updateBEChoice(getApplication<Application>().applicationContext, choice)
            BASE_URL = choice
        }
    }
    //endregion
}