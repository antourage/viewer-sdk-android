package com.antourage.weaverlib.other.networking.base


import android.os.Handler
import android.os.Looper

import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
class AppExecutors private constructor(diskIO: Executor, networkIO: Executor, mainThread: Executor) {

    init {
        Companion.diskIO = diskIO
        Companion.networkIO = networkIO
        Companion.mainThread = mainThread
    }

    constructor() : this(
        Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(3),
        MainThreadExecutor()
    )


    private class MainThreadExecutor : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable) {
            mainThreadHandler.post(command)
        }
    }

    companion object {

        private var diskIO: Executor? = null

        private var networkIO: Executor? = null

        private var mainThread: Executor? = null

        fun diskIO(): Executor? {
            return diskIO
        }

        fun networkIO(): Executor? {
            return networkIO
        }

        fun mainThread(): Executor? {
            return mainThread
        }
    }
}
