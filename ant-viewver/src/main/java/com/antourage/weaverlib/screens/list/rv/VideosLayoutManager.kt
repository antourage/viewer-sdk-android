package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class VideosLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        return null
    }

    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsAdded(recyclerView, positionStart, itemCount)
        recyclerView.smoothScrollToPosition(positionStart)
    }

    override fun canScrollVertically(): Boolean {
        return false
    }
}
