package com.antourage.weaverlib.screens.videos

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.State
import com.antourage.weaverlib.other.networking.models.StreamResponse
import com.antourage.weaverlib.screens.base.BaseViewModel

class VideosViewModel(application: Application):BaseViewModel(application){
    var streamResponse: LiveData<Resource<List<StreamResponse>>> = MutableLiveData()
    var listOfStreams:MutableLiveData<List<StreamResponse>> = MutableLiveData()


    fun getStreams(){
        streamResponse = repository.getListOfStreams()
        streamResponse.observeForever(object : Observer<Resource<List<StreamResponse>>> {
            override fun onChanged(resource: Resource<List<StreamResponse>>?) {
                if(resource != null){
                    when(resource.state){
                        State.SUCCESS->{
                            listOfStreams.postValue(resource.data)
                            streamResponse.removeObserver(this)
                        }
                        State.FAILURE->{
                            error.postValue(resource.message)
                            streamResponse.removeObserver(this)
                        }
                    }
                }
            }
        })
    }
}