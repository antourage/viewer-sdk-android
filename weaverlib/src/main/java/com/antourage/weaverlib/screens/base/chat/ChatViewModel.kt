package com.antourage.weaverlib.screens.base.chat

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.screens.base.streaming.StreamingViewModel

abstract class ChatViewModel(application: Application) : StreamingViewModel(application) {
    protected var messagesLiveData: MutableLiveData<List<Message>> = MutableLiveData()

    fun getMessagesLiveData(): LiveData<List<Message>> {
        return messagesLiveData
    }

    init {
        messagesLiveData.postValue(listOf())
    }
}