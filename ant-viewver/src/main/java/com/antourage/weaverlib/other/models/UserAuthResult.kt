package com.antourage.weaverlib.other.models

sealed class RegisterPushNotificationsResult {
    class Success(val topicName: String) : RegisterPushNotificationsResult()
    class Failure(val cause: String) : RegisterPushNotificationsResult()
}