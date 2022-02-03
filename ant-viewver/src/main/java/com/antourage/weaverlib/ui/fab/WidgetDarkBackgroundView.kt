package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.circularHideOverlay
import com.antourage.weaverlib.other.circularRevealOverlay
import kotlinx.android.synthetic.main.antourage_onboarding_layout.view.*

class WidgetDarkBackgroundView  @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    lateinit var fab: View

    init {
        init()
    }

    fun startAnimation(fab: View){
        this.fab = fab
        circularRevealOverlay(this, fab)
        fab.bringToFront()
        fab.invalidate()
    }

    private fun init() {
        View.inflate(context, R.layout.antourage_onboarding_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    internal fun hideView(){
        if(this::fab.isInitialized){
            circularHideOverlay(this, fab)
        }else{
            this.visibility = GONE
        }
    }
}