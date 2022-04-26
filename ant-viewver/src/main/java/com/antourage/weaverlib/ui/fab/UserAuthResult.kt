package com.antourage.weaverlib.ui.fab

sealed class RegisterPushNotificationsResult {
    class Success(val topicName: String) : RegisterPushNotificationsResult()
    class Failure(val cause: String) : RegisterPushNotificationsResult()
}