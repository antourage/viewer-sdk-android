package com.antourage.weaverlib.other

import com.antourage.weaverlib.other.models.StreamResponse

object LiveWatchedBeforeSignIn {
    var liveWatchedBeforeSignIn: StreamResponse? = null
    var duration: Long? = null

    fun resetLastWatchedLive(){
        duration = 0L
        liveWatchedBeforeSignIn = null
    }
}