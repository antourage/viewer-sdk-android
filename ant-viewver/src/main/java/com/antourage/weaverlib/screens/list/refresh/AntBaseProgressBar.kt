package com.antourage.weaverlib.screens.list.refresh

import android.content.Context
import android.util.AttributeSet
import android.view.View

abstract class AntBaseProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    lateinit var mParent: AntPullToRefreshView
    var mPercent = 0f

    abstract fun setPercent(percent: Float)
    abstract fun setParent(parent: AntPullToRefreshView)
    abstract fun start()
    abstract fun stop()

    fun dp2px(dp: Int): Int{
        return dp*context.resources.displayMetrics.density.toInt()
    }
}