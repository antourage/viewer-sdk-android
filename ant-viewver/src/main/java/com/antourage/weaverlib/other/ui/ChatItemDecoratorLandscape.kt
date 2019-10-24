package com.antourage.weaverlib.other.ui

import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.view.View

internal class ChatItemDecoratorLandscape(
    private val marginDivider: Int,
    private val marginLeft: Int,
    private val marginTop: Int,
    private val marginRight: Int,
    private val marginBottom: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View,
        parent: RecyclerView, state: RecyclerView.State
    ) {
        with(outRect) {
            bottom = if (parent.getChildAdapterPosition(view) == ((parent.adapter?.itemCount ?: 0) - 1)) {
                marginBottom
            } else {
                marginDivider
            }
            if (parent.getChildAdapterPosition(view) == 0) {
                top = marginTop
            }
            left = marginLeft
            right = marginRight
        }
    }
}