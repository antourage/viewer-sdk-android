package com.antourage.weaverlib.other.ui

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet

internal class FadingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    override fun getTopFadingEdgeStrength(): Float {
        return 1.0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return 0.0f
    }
}