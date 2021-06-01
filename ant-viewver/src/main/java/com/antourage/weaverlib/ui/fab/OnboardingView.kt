package com.antourage.weaverlib.ui.fab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.constraintlayout.widget.ConstraintLayout
import com.antourage.weaverlib.R
import com.antourage.weaverlib.other.circularHideOverlay
import com.antourage.weaverlib.other.circularRevealOverlay
import com.antourage.weaverlib.other.slideUpOnboarding
import kotlinx.android.synthetic.main.antourage_onboarding_layout.view.*

class OnboardingView  @JvmOverloads constructor(
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
        onboardingArrow?.viewTreeObserver?.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    onboardingArrow?.viewTreeObserver?.removeOnGlobalLayoutListener(this)

                    fab.bringToFront()
                    fab.invalidate()

                    onboardingArrow?.y = fab.y - onboardingArrow.height + fab.height/2

                    overlayTitle?.slideUpOnboarding(offset = 300)
                    firstItem?.slideUpOnboarding(offset = 300 * 3)
                    secondItem?.slideUpOnboarding(offset = 300 * 5)
                    thirdItem?.slideUpOnboarding(offset = 300 * 7)
                    onboardingArrow?.slideUpOnboarding(offset = 300 * 9)
                }
            })
    }

    private fun init() {
        View.inflate(context, R.layout.antourage_onboarding_layout, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        closeOnboardingBtn.setOnClickListener {
            hideView()
        }
    }

    internal fun hideView(){
        if(this::fab.isInitialized){
            circularHideOverlay(this, fab)
        }else{
            this.visibility = GONE
        }
    }
}