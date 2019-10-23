package com.antourage.weaverlib.di

import android.app.Activity
import com.antourage.weaverlib.other.initDagger

/**
 * this exists to makes things beautiful in the Activity. Who needs `dagger-android`?
 */
internal val Activity.injector get() = (application).initDagger()
