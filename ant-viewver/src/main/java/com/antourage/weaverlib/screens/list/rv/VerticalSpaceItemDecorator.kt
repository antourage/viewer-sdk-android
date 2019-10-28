package com.antourage.weaverlib.screens.list.rv

import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

internal class VerticalSpaceItemDecorator(private val verticalSpaceHeight: Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = verticalSpaceHeight
    }
}