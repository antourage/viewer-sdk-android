package com.antourage.weaverlib.other.models

import android.support.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
class MessageType {
    companion object {
        const val SYSTEM: Int = 0
        const val USER: Int = 1
    }
}

@Keep
open class FirestoreModel(@get:Exclude var id: String = "")

@Keep
data class Message(
    var avatarUrl: String? = null,
    var email: String? = null,
    var nickname: String? = null,
    var text: String? = null,
    var type: Int? = null,
    var timestamp: Timestamp? = null,
    var userID: String? = null
) : FirestoreModel() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (avatarUrl != other.avatarUrl) return false
        if (email != other.email) return false
        if (nickname != other.nickname) return false
        if (text != other.text) return false
        if (type != other.type) return false
        if (timestamp != other.timestamp) return false
        if (userID != other.userID) return false

        return true
    }

    override fun hashCode(): Int {
        var result = avatarUrl?.hashCode() ?: 0
        result = 31 * result + (email?.hashCode() ?: 0)
        result = 31 * result + (nickname?.hashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (type ?: 0)
        result = 31 * result + (timestamp?.hashCode() ?: 0)
        result = 31 * result + (userID?.hashCode() ?: 0)
        return result
    }
}

@Keep
@IgnoreExtraProperties
class Poll : FirestoreModel() {
    var startTimestamp: Timestamp? = null
    var question: String? = null
    @get:PropertyName("isActive")
    var isActive: Boolean = false
    var endTimestamp: Timestamp? = null
    var answers: List<String>? = null
    @get:Exclude
    var isAnswered: Boolean = false
}

@Keep
data class AnsweredUser(
    var choosenAnswer: Int? = null,
    var timestamp: Timestamp? = null
) : FirestoreModel()

@Keep
data class AnswersCombined(val answerText: String, var numberAnswered: Int)

@Keep
@IgnoreExtraProperties
data class Stream(
    @get:Exclude val streamId: Int,
    @get:PropertyName("isChatActive")
    var isChatActive: Boolean,
    val teamID: Int,
    val userID: Int,
    val viewersCount: Int,
    @get:Exclude val messages: List<Message>
) {
    constructor() : this(-1, false, -1, -1, -1, mutableListOf())
}
