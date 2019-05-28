package com.antourage.weaverlib.other.models

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName
import com.google.firebase.database.IgnoreExtraProperties



data class Message(
    val messageId:String?=null,
    var avatarUrl: String? = null,
    var email: String? = null,
    var nickname: String? = null,
    var text: String? = null,
    var timestamp: Long? = null
)
@IgnoreExtraProperties
class Poll {
    var userId: Int = 0
    var streamId: Int = 0
    var teamId: Int = 0
    var startTimestamp: Long = 0
    var pollQuestion: String? = null
    @PropertyName("isActive")
    @get:Exclude
    @set:Exclude
    var isActive: Boolean = false
    var endTimestamp: Long = 0
    @get:Exclude
    @set:Exclude
    var pollId: String? = null
    var pollAnswers: List<PollAnswers>? = null
    var isAnswered: Boolean = false

    val totalAnswers: Int
        get() {
            var sum = 0
            for (i in 0 until pollAnswers!!.size) {
                sum += pollAnswers!![i].andweredUsers!!.size
            }
            return sum
        }

    constructor() {}

    constructor(
        userId: Int,
        streamId: Int,
        teamId: Int,
        startTimestamp: Long,
        pollQuestion: String,
        isActive: Boolean,
        endTimestamp: Long,
        pollId: String,
        pollAnswers: List<PollAnswers>
    ) {
        this.userId = userId
        this.streamId = streamId
        this.teamId = teamId
        this.startTimestamp = startTimestamp
        this.pollQuestion = pollQuestion
        this.isActive = isActive
        this.endTimestamp = endTimestamp
        this.pollId = pollId
        this.pollAnswers = pollAnswers
    }
}
class PollAnswers {

    var answerText: String? = null
    var andweredUsers: MutableList<String>? = null

    constructor() {}

    constructor(answerText: String, answeredUsers: MutableList<String>) {
        this.answerText = answerText
        this.andweredUsers = answeredUsers
    }

    constructor(answerText: String) {
        this.answerText = answerText
    }
}
