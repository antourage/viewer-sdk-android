package com.antourage.weaverlib.ui.fab

import com.antourage.weaverlib.other.models.StreamResponse

internal sealed class WidgetStatus {
    object Inactive : WidgetStatus()
    class ActiveLiveStream(val list: List<StreamResponse>) : WidgetStatus()
    class ActiveUnseenVideos(val numberOfVideos: Int) : WidgetStatus()
}