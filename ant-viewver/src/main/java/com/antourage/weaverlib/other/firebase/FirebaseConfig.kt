package com.antourage.weaverlib.other.firebase

internal class FirebaseConfig {
    internal companion object {
        const val COLLECTION_PATH = "antourage"
        const val COLLECTION_PATH_STREAMS = "streams"
        const val COLLECTION_PATH_MESSAGES = "messages"
        const val COLLECTION_PATH_POLLS = "polls"
        const val COLLECTION_PATH_ANSWERED_USERS = "answeredUsers"

        const val DOCUMENT_PATH_LOCAL = "local"
        const val DOCUMENT_PATH_DEV = "dev"
        const val DOCUMENT_PATH_STAGING = "staging"
        const val DOCUMENT_PATH_PROD = "prod"
        const val DOCUMENT_PATH_LOAD = "load"
        const val DOCUMENT_PATH_DEFAULT = DOCUMENT_PATH_PROD
    }
}