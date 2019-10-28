package com.antourage.weaverlib.ui.fab

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.annotation.Keep

/**
 * Elegant way to give view lifecycle.
 * Not used as it is too much to ask for if integrating in Cordova or React Native
 */
@Keep
internal object AntourageFabLifecycleObserver : LifecycleObserver {
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

@Keep
interface FabActionHandler {
    fun onPause() {}
    fun onResume() {}
    fun onStart() {}
    fun onStop() {}
}