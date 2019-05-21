package com.antourage.weaverlib.ui.fab

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent

object AntourageFabLifecycleObserver : LifecycleObserver {
    private var actionHandler: FabActionHandler? = null

    fun registerActionHandler(handler: FabActionHandler) {
        actionHandler = handler
    }

    fun registerLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        this.actionHandler?.onStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        this.actionHandler?.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        this.actionHandler?.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stop() {
        this.actionHandler?.onStop()
    }
}

interface FabActionHandler {
    fun onPause()
    fun onResume()
    fun onStart()
    fun onStop()
}