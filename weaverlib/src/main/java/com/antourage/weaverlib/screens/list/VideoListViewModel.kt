package com.antourage.weaverlib.screens.list

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.Debouncer
import com.antourage.weaverlib.other.generateRandomViewerNumber
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.dev_settings.OnDevSettingsChangedListener

class VideoListViewModel(application: Application) : BaseViewModel(application), OnDevSettingsChangedListener,
    ReceivingVideosManager.ReceivingVideoCallback {

    var listOfStreams: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    val receivingVideosManager = ReceivingVideosManager.newInstance(this)


    fun getStreams() {
            ReceivingVideosManager.startReceivingVideos()
    }
    fun getListOfVideos(){
        listOfStreams.postValue(Repository().getListOfVideos())
    }
    fun onStop() {
        showBeDialogLiveData.postValue(false)
        numberOfLogoClicks = 0
        ReceivingVideosManager.stopReceivingVideos()
    }

    override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
        when (resource.state) {
            State.LOADING -> {
            }
            State.SUCCESS -> {
                val list = (resource.data)?.toMutableList()
                list?.let {
                    for (i in 0 until list.size) {
                        list[i].isLive = true
                        list[i].viewerCounter = generateRandomViewerNumber()
                    }
                }
                if(list?.size != 0)
                    list?.add(StreamResponse(-1))
                list?.addAll(repository.getListOfVideos())
                listOfStreams.postValue(list)
            }
            State.FAILURE -> {
                error.postValue(resource.message)
            }
        }
    }

    override fun onVODReceived() {
    }

    //region backend choice
    private val BE_CHOICE_TIMEOUT = 4000L
    private val BE_CHOICE_NUMBEROFCLICKS = 4

    private val showBeDialogLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var numberOfLogoClicks: Int = 0
    private val beDebouncer: Debouncer = Debouncer(Runnable{ numberOfLogoClicks = 0 }, BE_CHOICE_TIMEOUT)

    init {
        showBeDialogLiveData.postValue(false)
    }

    fun onLogoPressed() {
        if (numberOfLogoClicks >= BE_CHOICE_NUMBEROFCLICKS) {
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
            UserCache.newInstance().updateBEChoice(getApplication<Application>().applicationContext, choice)
            BASE_URL = choice
        }
    }
    //endregion
}