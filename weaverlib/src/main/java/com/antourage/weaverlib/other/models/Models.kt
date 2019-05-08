package com.antourage.weaverlib.other.models

data class Message(
    val messageId:String?=null,
    var avatarUrl: String? = null,
    var email: String? = null,
    var nickname: String? = null,
    var text: String? = null,
    var timestamp: Long? = null
)