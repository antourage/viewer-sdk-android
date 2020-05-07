package com.antourage.weaverlib.screens.base.chat

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Message
import com.antourage.weaverlib.other.models.MessageType
import com.antourage.weaverlib.screens.base.player.BasePlayerViewModel

internal abstract class ChatViewModel(application: Application) : BasePlayerViewModel(application) {
    internal var messagesLiveData: MutableLiveData<List<Message>> =
        MutableLiveData<List<Message>>().apply { postValue(listOf())}
    private var newUnseenCommentsLD: MutableLiveData<Int> =
        MutableLiveData<Int>().apply { postValue(0)}
    fun getNewUnseenComments() = newUnseenCommentsLD as LiveData<Int>

    private var startTime: Long? = null

    abstract fun checkIfMessageByUser(userID: String?): Boolean

    fun addUnseenComments(numOfNew: Int){
        newUnseenCommentsLD.postValue(getUnseenQuantity() + numOfNew)
    }

    fun setSeenComments(isAll: Boolean = false, numOfSeen: Int = 0){
        if (isAll || getUnseenQuantity() - numOfSeen <= 0){
            newUnseenCommentsLD.postValue(0)
        } else {
            newUnseenCommentsLD.postValue(getUnseenQuantity() - numOfSeen)
        }
    }

    fun getUnseenQuantity(): Int = newUnseenCommentsLD.value ?: 0
    internal fun setStartTime(startTime: Long) { this.startTime = startTime }
    internal fun getStartTime() =  this.startTime

    internal fun getMessagesLiveData(): LiveData<List<Message>> = messagesLiveData

    internal fun chatContainsNonStatusMsg(list: List<Message>): Boolean =
        list.any { it.type == MessageType.USER }
}