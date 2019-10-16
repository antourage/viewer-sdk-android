package com.antourage.weaverlib.screens.list

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.Debouncer
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.dev_settings.OnDevSettingsChangedListener
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

class VideoListViewModel @Inject constructor(application: Application) :
    BaseViewModel(application), OnDevSettingsChangedListener,
    ReceivingVideosManager.ReceivingVideoCallback {
    private var pulledToRefresh: Boolean = false
    private var canRefresh: Boolean = false
    var listOfStreams: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var loaderLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var liveVideos: MutableList<StreamResponse>? = null
    private var vods: List<StreamResponse>? = null

    private var showCallResult = false

    var liveVideosUpdated = false
    var vodsUpdated = false

    private val VODS_COUNT = 15
    private val MIN_ANIM_SHOWING_TIME_MILLS = 1500L

    fun subscribeToLiveStreams() {
        ReceivingVideosManager.setReceivingVideoCallback(this)
        ReceivingVideosManager.startReceivingVideos()
        Log.d("REFRESH_VODS", "subscribeToLiveStreams: count = ${vods?.size ?: 0}")
        refreshVODs()
    }

    fun refreshVODs(
        count: Int = (vods?.size?.minus(1)) ?: 0,
        noLoadingPlaceholder: Boolean = false
    ) {
        var vodsCount = count
        if (vodsCount < VODS_COUNT) {
            vodsCount = 0
        }
        this.pulledToRefresh = noLoadingPlaceholder
        ReceivingVideosManager.loadVODs(vodsCount)
    }

    fun refreshVODsLocally() {
        if (canRefresh) {
            val resultList = mutableListOf<StreamResponse>()
            liveVideos?.let { resultList.addAll(it) }
            if (resultList.size > 0) {
                resultList.add(
                    getStreamDividerPlaceholder()
                )
            }

            var addBottomLoader = false
            if (vods?.find { it.streamId == -2 } != null) {
                addBottomLoader = true
            }

            vods = mutableListOf()
            Repository.vods?.let { (vods as MutableList<StreamResponse>).addAll(it) }
            if (addBottomLoader) {
                (vods as MutableList<StreamResponse>).add(getStreamLoaderPlaceholder())
            }

            vods?.let { resultList.addAll(it.toList()) }
            listOfStreams.postValue(resultList.toList())
        }
        canRefresh = true
    }

    fun onPause() {
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
                    }
                    liveVideosUpdated = true
                    if (vodsUpdated) {
                        updateVideosList()
                    }
                }
            }
            is Status.Loading -> {
                liveVideosUpdated = false
            }
            is Status.Failure -> {
                liveVideosUpdated = true
                error.postValue(resource.status.errorMessage)
            }
        }
    }

    override fun onVODReceived(resource: Resource<List<StreamResponse>>) {
        when (resource.status) {
            is Status.Success -> {
                val list = mutableListOf<StreamResponse>()
                Repository.vods?.let { list.addAll(it) }
                val newList = (resource.status.data)?.toMutableList()
                if (newList != null) {
                    list.addAll(list.size, newList)
                }
                Repository.vods = list.toList()
                if (newList?.size == VODS_COUNT)
                    list.add(
                        list.size, getStreamLoaderPlaceholder()
                    )
                vods = list
                vodsUpdated = true
                if (liveVideosUpdated) {
                    updateVideosList()
                }
            }
            is Status.Loading -> {
                vodsUpdated = false
            }
            is Status.Failure -> {
                vodsUpdated = true
                loaderLiveData.postValue(false)
                error.postValue(resource.status.errorMessage)
            }
        }
    }

    override fun onVODReceivedInitial(resource: Resource<List<StreamResponse>>) {
        when (resource.status) {
            is Status.Success -> {
                var newList = (resource.status.data)?.toMutableList()
                Repository.vods = newList?.toList()
                if (newList?.size == VODS_COUNT)
                    newList.add(
                        newList.size, getStreamLoaderPlaceholder()
                    )
                vods = newList
                vodsUpdated = true
                if (liveVideosUpdated) {
                    updateVideosList()
                }
            }
            is Status.Loading -> {
                vodsUpdated = false
                if (!pulledToRefresh) {
                    loaderLiveData.postValue(true)
                }
                Timer().schedule(MIN_ANIM_SHOWING_TIME_MILLS) {
                    showCallResult = true
                    if (liveVideosUpdated && vodsUpdated) {
                        updateVideosList()
                    }
                }
            }
            is Status.Failure -> {
                vodsUpdated = true
                loaderLiveData.postValue(false)
                error.postValue(resource.status.errorMessage)
            }
        }
    }

    private fun updateVideosList() {
        if (showCallResult) {
            val resultList = mutableListOf<StreamResponse>()
            liveVideos?.let { resultList.addAll(it) }
            if (resultList.size > 0) {
                Log.d("VOD_TEST", "${liveVideos?.size} > 0, adding separator")
                resultList.add(
                    getStreamDividerPlaceholder()
                )
            }
            vods?.let { resultList.addAll(it.toList()) }
            loaderLiveData.postValue(false)
            listOfStreams.postValue(resultList.toList())
            showCallResult = false
        }
    }

    private fun getStreamDividerPlaceholder(): StreamResponse {
        return StreamResponse(
            -1, -1, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, false, null, false, null
        )
    }

    private fun getStreamLoaderPlaceholder(): StreamResponse {
        return StreamResponse(
            -2, -2, null, null,
            null, null, null, null,
            null, null, null, null, null,
            null, null, null, null, false, null, false, null
        )
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
            UserCache.getInstance(getApplication<Application>().applicationContext)
                ?.updateBEChoice(choice)
            BASE_URL = choice
        }
    }
    //endregion
}