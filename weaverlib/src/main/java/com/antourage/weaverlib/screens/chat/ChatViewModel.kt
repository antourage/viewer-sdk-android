package com.antourage.weaverlib.screens.chat

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.screens.base.BaseViewModel

class ChatViewModel(application: Application):BaseViewModel(application){


    private var messagesLiveData: MutableLiveData<List<Message>> = MutableLiveData()

    fun getMessagesLiveData(): LiveData<List<Message>> {
        return messagesLiveData
    }

    init {
        messagesLiveData.postValue(listOf())
    }

    fun addMessage(text: String) {
        if (!text.isEmpty() && !text.isBlank()) {
            val temp: MutableList<Message> = (messagesLiveData.value)!!.toMutableList()
            temp.add(
                Message(
                    (temp.size + 1).toString(), null,
                    "osoluk@leobit.co", "ooollleeennnaaaa", text, null
                )
            )
            messagesLiveData.postValue(temp as List<Message>)
        }
    }
}