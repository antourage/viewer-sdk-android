package com.antourage.weaverlib.screens.list.rv

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.annotation.NonNull
import android.support.v7.widget.LinearLayoutManager


class VideosLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        return null
    }

    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsAdded(recyclerView, positionStart, itemCount)
        val pos = findFirstCompletelyVisibleItemPosition()
        //        if(findFirstVisibleItemPosition() == 1)
        recyclerView.smoothScrollToPosition(positionStart)
    }
}
