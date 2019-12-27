package com.antourage.weaverlib

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.antourage.weaverlib.other.networking.ConnectionStateMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.lang.ref.WeakReference

/**
 * Using this way to get applicationContext to avoid asking parent app to initialize lib
 * Also connecting this way to Firebase does not require json file and is safer as keys are not hardcoded
 */
internal class ModuleResourcesProvider : ContentProvider() {
    companion object {
        var appContext: WeakReference<Context>? = null

        fun getContext() = appContext?.get()
    }

    override fun onCreate(): Boolean {
        appContext = context?.applicationContext?.let { WeakReference(it) }

        appContext?.get()?.let {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FirebaseApiKey)
                .setApplicationId(BuildConfig.ApplicationFirebaseId)
                .setDatabaseUrl(BuildConfig.DatabaseUrl)
                .setProjectId(BuildConfig.FirebaseProjectId)
                .build()
            FirebaseApp.initializeApp(it, options, BuildConfig.FirebaseName)
            ConnectionStateMonitor(it)
            UserCache.initialize(it)
        }
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        return -1
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return -1
    }

    override fun getType(uri: Uri): String? {
        return null
    }
}