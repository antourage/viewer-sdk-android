package com.antourage.weaverlib.other.ui

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class FadingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
):RecyclerView(context,attrs,defStyleAttr){
    override fun getTopFadingEdgeStrength(): Float {
        return 0.25f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return 0.0f
    }
}