package com.antourage.weaverlib.screens.list

import android.content.Context
import android.graphics.Point
import android.view.WindowManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class PreCachingLayoutManager : LinearLayoutManager {
    private var extraLayoutSpace = -1
    private lateinit var context: Context

    constructor(context: Context?) : super(context) {
        if (context != null) {
            this.context = context
        }
    }

    constructor(context: Context, extraLayoutSpace: Int) : super(context) {
        this.context = context
        this.extraLayoutSpace = extraLayoutSpace
    }

    constructor(context: Context, orientation: Int, reverseLayout: Boolean) : super(
        context,
        orientation,
        reverseLayout
    ) {
        this.context = context
    }

    fun setExtraLayoutSpace() {
        this.extraLayoutSpace = getScreenHeight()
    }

    private fun getScreenHeight(): Int {
        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.y
    }

    override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
        return if (extraLayoutSpace > 0) {
            extraLayoutSpace
        } else DEFAULT_EXTRA_LAYOUT_SPACE
    }

    companion object {
        private const val DEFAULT_EXTRA_LAYOUT_SPACE = 600
    }
}