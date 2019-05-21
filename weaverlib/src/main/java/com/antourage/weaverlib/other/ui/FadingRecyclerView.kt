package com.antourage.weaverlib.other.ui

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

class FadingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    override fun getTopFadingEdgeStrength(): Float {
        return if ((this.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition() == 0)
            0.0f
        else
            1.0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return 0.0f
    }


}