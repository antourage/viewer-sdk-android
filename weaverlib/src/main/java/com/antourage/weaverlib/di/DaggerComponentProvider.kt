package com.antourage.weaverlib.di

import android.app.Activity
import com.antourage.weaverlib.WeaverInitProvider
import com.antourage.weaverlib.other.initDagger

/**
 * This exists so things get less coupled from the application class. Thanks to this interface,
 * our application class doesn't need to be open just so our test application class can extend it.
 */
interface DaggerComponentProvider {

    val component: ApplicationComponent
}

/**
 * And this exists to makes things beautiful in the Activity. Who needs `dagger-android`?
 */
val Activity.injector get() = (application).initDagger()
