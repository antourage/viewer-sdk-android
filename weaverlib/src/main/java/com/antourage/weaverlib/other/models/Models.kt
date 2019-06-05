package com.antourage.weaverlib.other.models


import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

class MessageType {
    companion object {
        const val SYSTEM: Int = 0
        const val USER: Int = 1
    }
}
open class FirestoreModel(@get:Exclude var id:String = "")
data class Message(
    var avatarUrl: String? = null,
    var email: String? = null,
    var nickname: String? = null,
    var text: String? = null,
    var type:Int?=null,
    var timestamp: Timestamp? = null
):FirestoreModel()
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
@IgnoreExtraProperties
data class Stream(@get:Exclude val streamId:Int,
                  @get:PropertyName("isChatActive")
                  var isChatActive:Boolean,
                  val teamID: Int,
                  val userID:Int,
                  val viewersCount:Int,
                  @get:Exclude val messages:List<Message>){
    constructor():this(-1,false,-1,-1,-1, mutableListOf())
}
