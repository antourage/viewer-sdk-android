package com.antourage.weaverlib.screens.list

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.UserCache
import com.antourage.weaverlib.other.Debouncer
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.models.User
import com.antourage.weaverlib.other.models.UserRequest
import com.antourage.weaverlib.other.networking.ApiClient.BASE_URL
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.antourage.weaverlib.screens.list.dev_settings.OnDevSettingsChangedListener
import com.antourage.weaverlib.ui.fab.AntourageFab
import com.antourage.weaverlib.ui.fab.UserAuthResult
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.schedule

internal class VideoListViewModel @Inject constructor(application: Application) :
    BaseViewModel(application), OnDevSettingsChangedListener,
    ReceivingVideosManager.ReceivingVideoCallback {
    private var pulledToRefresh: Boolean = false
    private var canRefresh: Boolean = false
    var listOfStreams: MutableLiveData<List<StreamResponse>> = MutableLiveData()
    var loaderLiveData: MutableLiveData<Boolean> = MutableLiveData()
    private var liveVideos: MutableList<StreamResponse>? = null
    private var vods: List<StreamResponse>? = null

    private var showCallResult = false

    private var liveVideosUpdated = false
    private var vodsUpdated = false

    companion object {
        private const val VODS_COUNT = 15
        private const val MIN_ANIM_SHOWING_TIME_MILLS = 1500L

        private const val BE_CHOICE_TIMEOUT = 4000L
        private const val BE_CHOICE_CLICKS = 4
    }

    fun subscribeToLiveStreams() {
        ReceivingVideosManager.setReceivingVideoCallback(this)
        ReceivingVideosManager.startReceivingLiveStreams()
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
                        updateVideosList(true)
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
                Repository.vods?.let {
                    list.addAll(it)
                }
                val newList = (resource.status.data)?.toMutableList()
                if (newList != null) {
                    list.addAll(list.size, newList)
                }
                Repository.vods = list.toMutableList()
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
                val newList = (resource.status.data)?.toMutableList()
                //TODO: delete (used for testing)
//                newList?.addAll(Repository.getMockedStreamsForTheTest())
                Repository.vods = newList?.toMutableList()
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

    private fun updateVideosList(updateLiveStreams: Boolean = false) {
        if (showCallResult || updateLiveStreams) {
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

    fun onNetworkGained() {
        refreshVODs(noLoadingPlaceholder = true)
    }

    fun handleUserAuthorization() {
        if (userAuthorized()) {
            subscribeToLiveStreams()
        } else {
            getCachedApiKey()?.let { apiKey ->
                authorizeUser(apiKey, getCachedUserRefId(), getCachedNickname(), null)
            }
        }
    }

    internal fun userAuthorized(): Boolean {
        return !(UserCache.getInstance(getApplication())?.getToken().isNullOrBlank())
    }

    private fun getCachedNickname(): String? {
        return UserCache.getInstance(getApplication())?.getUserNickName()
    }

    private fun getCachedUserRefId(): String? {
        return UserCache.getInstance(getApplication())?.getUserRefId()
    }

    private fun getCachedApiKey(): String? {
        return UserCache.getInstance(getApplication())?.getApiKey()
    }

    private fun authorizeUser(
        apiKey: String,
        refUserId: String? = null,
        nickname: String? = null,
        callback: ((result: UserAuthResult) -> Unit)? = null
    ) {
        Log.d(AntourageFab.TAG, "Trying to authorize ant user...")
        refUserId?.let { UserCache.getInstance(getApplication())?.saveUserRefId(it) }
        nickname?.let { UserCache.getInstance(getApplication())?.saveUserNickName(it) }
        UserCache.getInstance(getApplication())?.saveApiKey(apiKey)

        val response = Repository.generateUser(UserRequest(apiKey, refUserId, nickname))
        response.observeForever(object : Observer<Resource<User>> {
            override fun onChanged(it: Resource<User>?) {
                when (val responseStatus = it?.status) {
                    is Status.Success -> {
                        val user = responseStatus.data
                        Log.d(AntourageFab.TAG, "Ant authorization successful")
                        user?.apply {
                            if (token != null && id != null) {
                                Log.d(
                                    AntourageFab.TAG,
                                    "Ant token and ant userId != null, started live video timer"
                                )
                                UserCache.getInstance(getApplication())?.saveUserAuthInfo(token, id)
                                subscribeToLiveStreams()
                                refreshVODs()
                            }
                        }
                        callback?.invoke(UserAuthResult.Success)
                        response.removeObserver(this)
                    }
                    is Status.Failure -> {
                        Log.d(
                            AntourageFab.TAG,
                            "Ant authorization failed: ${responseStatus.errorMessage}"
                        )
                        callback?.invoke(UserAuthResult.Failure(responseStatus.errorMessage))
                        response.removeObserver(this)
                    }
                }
            }
        })
    }
}