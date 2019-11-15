package com.antourage.weaverlib.ui.fab

sealed class UserAuthResult {
    object Success : UserAuthResult()
    class Failure(val cause: String) : UserAuthResult()
}

sealed class RegisterPushNotificationsResult {
    class Success(val topicName: String) : RegisterPushNotificationsResult()
    class Failure(val cause: String) : RegisterPushNotificationsResult()
}