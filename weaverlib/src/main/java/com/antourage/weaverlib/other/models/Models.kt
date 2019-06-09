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
class Poll : FirestoreModel() {
    var startTimestamp: Timestamp? = null
    var question: String? = null
    @get:PropertyName("isActive")
    var isActive: Boolean = false
    var endTimestamp: Timestamp? = null
    var answers: List<String>? = null
    @get:Exclude var isAnswered:Boolean = false
}

data class AnsweredUser(
    var choosenAnswer:Int? = null,
    var timestamp:Timestamp? = null
):FirestoreModel()

data class AnswersCombined(val answerText:String, var numberAnswered:Int)

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
