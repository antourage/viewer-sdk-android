package com.antourage.weaverlib.other.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemDecoration inheritance which makes margins between elements and
 * half margin at top and bottom of list.
 */
class TopBottomItemDecoration(private val dividerMargin: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        with(outRect) {
            top = (dividerMargin / 2)
            bottom = (dividerMargin / 2)
        }
    }
}