package com.antourage.weaverlib.screens.poll

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.other.models.AnsweredUser
import com.antourage.weaverlib.other.models.AnswersCombined
import com.antourage.weaverlib.other.models.Poll
import com.antourage.weaverlib.other.networking.Resource
import com.antourage.weaverlib.other.networking.Status
import com.antourage.weaverlib.screens.base.BaseViewModel
import com.antourage.weaverlib.screens.base.Repository
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import javax.inject.Inject

internal class PollDetailsViewModel @Inject constructor(
    application: Application
) : BaseViewModel(application) {

    internal var userId: Int? = null
    private val pollLiveData = MutableLiveData<Poll>()
    private var answersLiveData: MutableLiveData<List<AnswersCombined>> = MutableLiveData()
    private var streamId: Int = -1
    private var pollId: String = ""
    var isAnswered: Boolean = false
    private var repository = Repository()

    internal fun getPollLiveData(): LiveData<Poll> = pollLiveData
    internal fun getAnswersLiveData(): LiveData<List<AnswersCombined>> = answersLiveData

    fun initPollDetails(streamId: Int, pollId: String, userId: Int) {
        this.streamId = streamId
        this.pollId = pollId
        this.userId = userId
        Repository.getPollDetails(streamId, pollId).observeForever { resource ->
            resource?.status?.let { status ->
                if (status is Status.Success) {
                    val poll = status.data
                    pollLiveData.postValue(poll)
                    Repository.getAnsweredUsers(streamId, pollId)
                        .observeForever { answeredUsersResource ->
                            answeredUsersResource?.apply { manageAnswers(this, poll) }
                        }
                }
            }
        }
    }

    fun calculateAllAnswers(): Int {
        var sum = 0
        if (answersLiveData.value != null)
            for (answer in answersLiveData.value ?: arrayListOf()) {
                sum += answer.numberAnswered
            }
        return sum
    }

    fun onAnswerChosen(pos: Int, userId: Int) {
        FirebaseAuth.getInstance(FirebaseApp.getInstance(BuildConfig.FirebaseName))
            .currentUser?.let {
            val userAnswer = AnsweredUser()
            userAnswer.chosenAnswer = pos
            userAnswer.timestamp = Timestamp(Date())
            userAnswer.id = userId.toString()
            Repository.vote(streamId, pollId, userAnswer)
            isAnswered = true
        }
    }

    private fun manageAnswers(answeredUsersResource: Resource<List<AnsweredUser>>, poll: Poll?) {
        if (answeredUsersResource.status is Status.Success) {
            val answeredUsers = answeredUsersResource.status.data
            val answers = mutableListOf<AnswersCombined>()
            if (poll?.answers != null && answeredUsers != null)
                for (i in 0 until (poll.answers?.size ?: 0)) {
                    var counter = 0
                    for (j in answeredUsers.indices) {
                        if (answeredUsers[j].id == userId.toString()
                        ) {
                            isAnswered = true
                        }
                        if (answeredUsers[j].chosenAnswer == i)
                            counter++
                    }
                    val combinedAns = AnswersCombined(poll.answers?.get(i) ?: "", counter)
                    answers.add(combinedAns)
                }
            answersLiveData.postValue(answers)
        }
    }
}
