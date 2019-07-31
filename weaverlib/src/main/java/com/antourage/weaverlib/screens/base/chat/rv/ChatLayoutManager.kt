package com.antourage.weaverlib.screens.base.chat.rv

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView

/**
 * added to correctly handle smooth scroll
 */
class ChatLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        return null
    }

    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsAdded(recyclerView, positionStart, itemCount)
        val pos = findLastCompletelyVisibleItemPosition()
        if ((pos+1) == (recyclerView.adapter?.itemCount?.minus(1))) {
                recyclerView.scrollToPosition(positionStart)
        }
    }
}
