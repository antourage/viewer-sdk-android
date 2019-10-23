package com.antourage.weaverlib.screens.base.chat

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.screens.base.player.BasePlayerViewModel

internal abstract class ChatViewModel(application: Application) : BasePlayerViewModel(application) {
    internal var messagesLiveData: MutableLiveData<List<Message>> = MutableLiveData()

    init {
        messagesLiveData.postValue(listOf())
    }

    internal fun getMessagesLiveData(): LiveData<List<Message>> = messagesLiveData

    internal fun chatContainsNonStatusMsg(list: List<Message>): Boolean =
        list.any { it.type == MessageType.USER }
}