package com.antourage.weaverlib.screens.list

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.StreamResponse
import com.antourage.weaverlib.other.networking.base.Resource
import com.antourage.weaverlib.other.networking.base.State
import com.antourage.weaverlib.screens.base.BaseViewModel

class VideoListViewModel(application: Application):BaseViewModel(application),ReceivingVideosManager.ReceivingVideoCallback{

    var listOfStreams:MutableLiveData<List<StreamResponse>> = MutableLiveData()
    val receivingVideosManager = ReceivingVideosManager(this)


    fun getStreams(){
        receivingVideosManager.startReceivingVideos()
    }
    fun onStop(){
        receivingVideosManager.stopReceivingVideos()
    }
    override fun onLiveBroadcastReceived(resource: Resource<List<StreamResponse>>) {
        when(resource.state){
            State.LOADING->{}
            State.SUCCESS->{
                val list =(resource.data)?.toMutableList()
                list?.let {
                    for (i in 0 until list.size) {
                        list[i].isLive = true
                    }
                }
                list?.addAll(repository.getListOfVideos())
                listOfStreams.postValue(list)
            }
            State.FAILURE->{
                error.postValue(resource.message)
            }
        }
    }

    override fun onVODReceived() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}