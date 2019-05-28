package com.antourage.weaverlib.screens.poll


import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.models.PollAnswers
import com.antourage.weaverlib.screens.base.BaseViewModel

class PollDetailsViewModel(application: Application) : BaseViewModel(application) {

    val pollLiveData = MutableLiveData<Poll>()

    init {
        pollLiveData.value = repository.getCurrentPoll()
    }

    fun onAnswerChosen(pos: Int) {
        val currentPoll = repository.getCurrentPoll()
        val answers = currentPoll.pollAnswers
        val users = answers!![pos].andweredUsers
        users!!.add("olena")
        answers[pos].andweredUsers = users
        currentPoll.pollAnswers = answers
        currentPoll.isAnswered = true
        repository.setCurrentPoll(currentPoll)
        pollLiveData.postValue(currentPoll)
    }
}
