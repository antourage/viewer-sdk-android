package com.antourage.weaverlib

import android.content.Context
import android.content.res.AssetManager
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.util.*

internal class PropertyManager private constructor(context: Context) {
    private var contextRef: WeakReference<Context>? = null

    init {
        this.contextRef = WeakReference(context)
    }

    companion object {
        private var INSTANCE: PropertyManager? = null

        internal const val CLIENT_ID = "CLIENT_ID"
        internal const val ANONYMOUS_CLIENT_ID = "ANONYMOUS_CLIENT_ID"
        internal const val ANONYMOUS_SECRET = "ANONYMOUS_SECRET"

        internal const val COGNITO_URL_DEV = "COGNITO_URL_DEV"
        internal const val COGNITO_URL_LOAD = "COGNITO_URL_LOAD"
        internal const val COGNITO_URL_STAGING = "COGNITO_URL_STAGING"
        internal const val COGNITO_URL_PROD = "COGNITO_URL_PROD"

        internal const val BASE_URL_DEV = "BASE_URL_DEV"
        internal const val BASE_URL_LOAD = "BASE_URL_LOAD"
        internal const val BASE_URL_STAGING = "BASE_URL_STAGING"
        internal const val BASE_URL_DEMO = "BASE_URL_DEMO"
        internal const val BASE_URL_PROD = "BASE_URL_PROD"

        @Synchronized
        fun getInstance(context: Context): PropertyManager? {
            if (INSTANCE == null) {
                INSTANCE = PropertyManager(context)
            }
            return INSTANCE
        }

        @Synchronized
        fun getInstance(): PropertyManager? {
            return INSTANCE
        }
    }

    @Throws(IOException::class)
    internal fun getProperty(key: String): String? {
        val properties = Properties()
        val assetManager: AssetManager = contextRef?.get()?.assets!!
        val inputStream: InputStream = assetManager.open("config.properties")
        properties.load(inputStream)
        return properties.getProperty(key)
    }


}