package com.antourage.weaverlib.other

import android.os.Handler

class Debouncer(private val runnable: Runnable, private val interval: Long) {

    private val handler: Handler = Handler()

    fun run() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, interval)
    }

    fun cancel() {
        handler.removeCallbacks(runnable)
    }
}
