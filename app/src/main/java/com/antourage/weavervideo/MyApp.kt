package com.antourage.weavervideo

import android.app.Application
import com.antourage.weaverlib.screens.base.AntourageActivity

class MyApp:Application(){
    override fun onCreate() {
        super.onCreate()
        AntourageActivity.initAntourage(this.applicationContext)
    }
}