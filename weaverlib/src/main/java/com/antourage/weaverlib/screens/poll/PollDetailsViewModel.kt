package com.antourage.weaverlib.screens.poll


import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.other.models.AnsweredUser
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.google.firebase.FirebaseApp
import javax.inject.Inject


class PollDetailsViewModel @Inject constructor(application: Application, val repository: Repository) : BaseViewModel(application) {

    private val pollLiveData = MutableLiveData<Poll>()
    private var answersLiveData:MutableLiveData<List<AnswersCombined>> = MutableLiveData()
    private var streamId:Int = -1
    private var pollId:String = ""
    var isAnswered: Boolean = false

    fun getPollLiveData():LiveData<Poll> = pollLiveData
    fun getAnswersLiveData():LiveData<List<AnswersCombined>> = answersLiveData

    fun initPollDetails(streamId:Int,pollId:String){
        this.streamId = streamId
        this.pollId = pollId
        repository.getPollDetails(streamId,pollId).observeForever { data->
            if(data?.data != null){
                pollLiveData.postValue(data.data)
                val poll = data.data
                repository.getAnsweredUsers(streamId, pollId).observeForever{
                    if (it != null){
                        val answeredUsers = it.data
                        val answers = mutableListOf<AnswersCombined>()
                        if (poll.answers != null && answeredUsers != null)
                            for (i in 0 until poll.answers!!.size){
                                var counter = 0
                                for(j in 0 until answeredUsers.size){
                                    if(answeredUsers[j].id == FirebaseAuth.getInstance(FirebaseApp.getInstance(
                                            BuildConfig.FirebaseName)).uid){
                                        isAnswered = true
                                    }
                                    if (answeredUsers[j].choosenAnswer == i)
                                        counter++
                                }
                                val combinedAns = AnswersCombined(poll.answers!![i],counter)
                                answers.add(combinedAns)
                            }
                        answersLiveData.postValue(answers)
                    }
                }
            }
        }
    }
    fun calculateAllAnswers():Int{
        var sum = 0
        if(answersLiveData.value != null)
        for (i in answersLiveData.value!!.indices) {
            sum += answersLiveData.value!![i].numberAnswered
        }
        return sum
    }


    fun onAnswerChosen(pos: Int) {
        FirebaseAuth.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName)).currentUser?.let {
            val userAnswer = AnsweredUser()
            userAnswer.choosenAnswer = pos
            userAnswer.timestamp = Timestamp(Date())
            userAnswer.id = it.uid
            repository.vote(streamId,pollId,userAnswer)
            isAnswered = true
        }
    }
}
