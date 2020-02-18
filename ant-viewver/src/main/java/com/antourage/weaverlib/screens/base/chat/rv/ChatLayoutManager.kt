package com.antourage.weaverlib.screens.base.chat.rv

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * added to correctly handle smooth scroll
 */
internal class ChatLayoutManager(context: Context?) : LinearLayoutManager(context) {

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams? {
        return null
    }

    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        super.onItemsAdded(recyclerView, positionStart, itemCount)
        val pos = findLastCompletelyVisibleItemPosition()
        if ((pos + 1) == (recyclerView.adapter?.itemCount?.minus(1))) {
//            recyclerView.smoothScrollToPosition(positionStart)
        }
    }
}
