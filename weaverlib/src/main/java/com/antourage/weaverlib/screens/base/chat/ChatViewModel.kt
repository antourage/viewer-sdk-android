package com.antourage.weaverlib.screens.base.chat

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.screens.base.player.BasePlayerViewModel

abstract class ChatViewModel(application: Application) : BasePlayerViewModel(application) {
    protected var messagesLiveData: MutableLiveData<List<Message>> = MutableLiveData()

    init {
        messagesLiveData.postValue(listOf())
    }

    fun getMessagesLiveData(): LiveData<List<Message>> = messagesLiveData

    protected fun chatContainsNonStatusMsg(list: List<Message>): Boolean =
        list.any { it.type == MessageType.USER }
}