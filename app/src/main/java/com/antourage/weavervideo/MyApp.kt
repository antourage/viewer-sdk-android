package com.antourage.weavervideo

import android.app.Application
import com.antourage.weaverlib.screens.base.AntourageActivity
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric



class MyApp:Application(){
    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        AntourageActivity.initAntourage(this.applicationContext)
    }
}