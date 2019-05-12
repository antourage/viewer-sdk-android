package com.antourage.weaverlib.other.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.EdgeEffect
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FadingRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
):RecyclerView(context,attrs,defStyleAttr) {
    override fun getTopFadingEdgeStrength(): Float {
        return if((this.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()==0)
            0.0f
        else
            1.0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return 0.0f
    }


}